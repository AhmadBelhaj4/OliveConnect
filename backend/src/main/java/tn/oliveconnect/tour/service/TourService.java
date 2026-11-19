package tn.oliveconnect.tour.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;
import tn.oliveconnect.common.facade.*;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.resource.document.*;
import tn.oliveconnect.resource.repository.*;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.tour.document.TourDocument;
import tn.oliveconnect.tour.dto.TourDtos.*;
import tn.oliveconnect.tour.repository.TourRepository;

@Service
public class TourService {

    private final TourRepository tours;
    private final WorkerRepository workers;
    private final VehicleRepository vehicles;
    private final BinRepository bins;
    private final MongoTemplate mongo;
    private final HarvestRequestFacade harvestRequests;   // façade de A (A7)
    private final NotificationFacade notifications;       // façade de A (A7)
    private final CacheInvalidator cache;

    public TourService(TourRepository tours, WorkerRepository workers, VehicleRepository vehicles,
                       BinRepository bins, MongoTemplate mongo, HarvestRequestFacade harvestRequests,
                       NotificationFacade notifications, CacheInvalidator cache) {
        this.tours = tours; this.workers = workers; this.vehicles = vehicles; this.bins = bins;
        this.mongo = mongo; this.harvestRequests = harvestRequests; this.notifications = notifications;
        this.cache = cache;
    }

    // --- B8 : créer DRAFT -------------------------------------------------------
    public TourResponse create(TourCreateRequest req) {
        TourDocument t = new TourDocument();
        t.setTourDate(req.tourDate());
        t.setStatus(TourStatus.DRAFT);
        return TourResponse.from(tours.save(t));
    }

    // --- B8 : planifier DRAFT -> PLANNED (conflit vérifié dans MongoDB !) -------
    public TourResponse plan(String tourId, TourPlanRequest req) {
        TourDocument tour = tours.findById(tourId).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        if (tour.getStatus() != TourStatus.DRAFT) {
            throw ApiException.invalidTransition("Seule une tournée DRAFT peut être planifiée.");
        }

        // 1. Vérifier que le chef d'équipe existe et a le rôle TEAM_LEADER
        verifyTeamLeader(req.teamLeaderId());

        // 2. Vérifier que toutes les demandes existent et sont APPROVED (via façade de A)
        List<String> reqIds = req.stops().stream().map(PlanStopRequest::harvestRequestId).toList();
        if (new HashSet<>(reqIds).size() != reqIds.size()) {
            throw ApiException.validation("stops", "Demande en double dans les arrêts");
        }
        List<HarvestRequestView> approved = harvestRequests.findApproved(reqIds);
        if (approved.size() != reqIds.size()) {
            throw ApiException.invalidTransition("Toutes les demandes doivent exister et être au statut APPROVED.");
        }
        Map<String, HarvestRequestView> byId = approved.stream()
                .collect(Collectors.toMap(HarvestRequestView::id, v -> v));

        // 3. Vérifier l'existence et le statut AVAILABLE des ressources dans MongoDB
        checkResourcesAvailable(req);

        // 4. Vérifier les CONFLITS le même jour dans MongoDB (jamais Redis !) -> 409 RESOURCE_CONFLICT
        checkSameDayConflicts(tour, req);

        // 5. Construire les arrêts imbriqués ordonnés par sequence
        List<TourDocument.TourStop> stops = req.stops().stream()
                .sorted(Comparator.comparingInt(PlanStopRequest::sequence))
                .map(s -> {
                    HarvestRequestView v = byId.get(s.harvestRequestId());
                    TourDocument.TourStop stop = new TourDocument.TourStop();
                    stop.setStopId(new ObjectId().toHexString());
                    stop.setSequence(s.sequence());
                    stop.setHarvestRequestId(v.id());
                    stop.setFarmerId(v.farmerId());
                    stop.setOrchardId(v.orchardId());
                    stop.setOrchardName(v.orchardName());
                    stop.setPlannedQuantityKg(v.estimatedQuantityKg());
                    stop.setStatus(StopStatus.PLANNED);
                    return stop;
                }).toList();

        tour.setTeamLeaderId(req.teamLeaderId());
        tour.setVehicleIds(new ArrayList<>(req.vehicleIds()));
        tour.setWorkerIds(new ArrayList<>(req.workerIds()));
        tour.setBinIds(new ArrayList<>(req.binIds()));
        tour.setStops(new ArrayList<>(stops));
        tour.setStatus(TourStatus.PLANNED);
        tour = tours.save(tour);

        // 6. Faire passer les demandes en SCHEDULED (façade A) + notifier les agriculteurs
        harvestRequests.markScheduled(reqIds, tour.getId());
        for (HarvestRequestView v : approved) {
            notifications.notify(v.farmerId(), NotificationType.TOUR_SCHEDULED,
                    "Votre récolte (%s) est planifiée sur la tournée du %s.".formatted(v.orchardName(), tour.getTourDate()),
                    new EntityRef(EntityType.TOUR, tour.getId()));
        }
        return TourResponse.from(tour);
    }

    // --- B9 : lectures ----------------------------------------------------------
    public PageResponse<TourResponse> list(AuthUser me, Pageable pageable) {
        Page<TourDocument> page = me.hasRole(Role.TEAM_LEADER)
                ? tours.findByTeamLeaderId(me.id(), pageable)
                : tours.findAll(pageable);
        return PageResponse.from(page, TourResponse::from);
    }

    public TourResponse get(AuthUser me, String id) {
        TourDocument t = tours.findById(id).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        if (me.hasRole(Role.TEAM_LEADER) && !me.id().equals(t.getTeamLeaderId())) {
            throw ApiException.notFound("Tournée introuvable.");   // §3.4 propriété -> 404
        }
        return TourResponse.from(t);
    }

    // --- B9 : démarrer PLANNED -> IN_PROGRESS -----------------------------------
    public TourResponse start(AuthUser me, String id) {
        TourDocument t = tours.findById(id).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        assertAssignedLeaderOrManager(me, t);   // 403 si autre chef d'équipe (§4)
        if (t.getStatus() != TourStatus.PLANNED) {
            throw ApiException.invalidTransition("Seule une tournée PLANNED peut démarrer.");
        }
        t.setStatus(TourStatus.IN_PROGRESS);
        t.setStartedAt(Instant.now());
        t = tours.save(t);

        List<String> reqIds = t.getStops().stream().map(TourDocument.TourStop::getHarvestRequestId).toList();
        if (!reqIds.isEmpty()) harvestRequests.markInProgress(reqIds);
        for (var s : t.getStops()) {
            notifications.notify(s.getFarmerId(), NotificationType.COLLECTION_STARTED,
                    "La collecte de votre verger %s a démarré.".formatted(s.getOrchardName()),
                    new EntityRef(EntityType.TOUR, t.getId()));
        }
        return TourResponse.from(t);
    }

    // --- B9 : terminer un arrêt -------------------------------------------------
    public TourResponse completeStop(AuthUser me, String tourId, String stopId, StopCompleteRequest req) {
        TourDocument t = tours.findById(tourId).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        if (!me.id().equals(t.getTeamLeaderId())) {
            throw ApiException.accessDenied("Seul le chef d'équipe assigné peut saisir les quantités.");
        }
        if (t.getStatus() != TourStatus.IN_PROGRESS) {
            throw ApiException.invalidTransition("La tournée doit être IN_PROGRESS pour terminer un arrêt.");
        }
        TourDocument.TourStop stop = t.getStops().stream().filter(s -> stopId.equals(s.getStopId())).findFirst()
                .orElseThrow(() -> ApiException.notFound("Arrêt introuvable."));
        if (stop.getStatus() == StopStatus.COMPLETED) {
            throw ApiException.invalidTransition("Cet arrêt est déjà terminé.");
        }
        stop.setActualQuantityKg(req.actualQuantityKg());
        stop.setNote(req.note());
        stop.setStatus(StopStatus.COMPLETED);
        t = tours.save(t);

        harvestRequests.markCompleted(stop.getHarvestRequestId(), req.actualQuantityKg());
        notifications.notify(stop.getFarmerId(), NotificationType.COLLECTION_COMPLETED,
                "Collecte terminée sur %s : %s kg récoltés.".formatted(stop.getOrchardName(), req.actualQuantityKg()),
                new EntityRef(EntityType.HARVEST_REQUEST, stop.getHarvestRequestId()));
        return TourResponse.from(t);
    }

    // --- B9 : terminer la tournée IN_PROGRESS -> COMPLETED ----------------------
    public TourResponse complete(AuthUser me, String id) {
        TourDocument t = tours.findById(id).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        assertAssignedLeaderOrManager(me, t);
        if (t.getStatus() != TourStatus.IN_PROGRESS) {
            throw ApiException.invalidTransition("Seule une tournée IN_PROGRESS peut être terminée.");
        }
        boolean anyUnfinished = t.getStops().stream().anyMatch(s -> s.getStatus() != StopStatus.COMPLETED);
        if (anyUnfinished) {
            throw ApiException.invalidTransition("Tous les arrêts doivent être terminés avant de clôturer la tournée.");
        }
        t.setStatus(TourStatus.COMPLETED);
        t.setCompletedAt(Instant.now());
        t = tours.save(t);
        cache.dashboardChanged();
        return TourResponse.from(t);
    }

    // --- B9 : annuler DRAFT|PLANNED -> CANCELLED --------------------------------
    public TourResponse cancel(String id) {
        TourDocument t = tours.findById(id).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        if (t.getStatus() != TourStatus.DRAFT && t.getStatus() != TourStatus.PLANNED) {
            throw ApiException.invalidTransition("Seule une tournée DRAFT ou PLANNED peut être annulée.");
        }
        boolean wasPlanned = t.getStatus() == TourStatus.PLANNED;
        t.setStatus(TourStatus.CANCELLED);
        t = tours.save(t);

        if (wasPlanned && !t.getStops().isEmpty()) {
            List<String> reqIds = t.getStops().stream().map(TourDocument.TourStop::getHarvestRequestId).toList();
            harvestRequests.revertToApproved(reqIds);
            for (var s : t.getStops()) {
                notifications.notify(s.getFarmerId(), NotificationType.TOUR_CHANGED,
                        "La tournée du %s a été annulée ; votre demande revient en APPROVED.".formatted(t.getTourDate()),
                        new EntityRef(EntityType.TOUR, t.getId()));
            }
        }
        return TourResponse.from(t);
    }

    // --- helpers privés ---------------------------------------------------------
    private void assertAssignedLeaderOrManager(AuthUser me, TourDocument t) {
        if (me.hasRole(Role.COLLECTION_MANAGER)) return;
        if (me.hasRole(Role.TEAM_LEADER) && me.id().equals(t.getTeamLeaderId())) return;
        throw ApiException.accessDenied("Vous n'êtes pas le chef d'équipe assigné à cette tournée.");
    }

    private void verifyTeamLeader(String leaderId) {
        if (!ObjectId.isValid(leaderId)) throw ApiException.validation("teamLeaderId", "Identifiant invalide");
        Document u = mongo.findOne(new Query(Criteria.where("_id").is(new ObjectId(leaderId))
                .and("role").is("TEAM_LEADER").and("active").is(true)), Document.class, "users");
        if (u == null) throw ApiException.validation("teamLeaderId", "Chef d'équipe introuvable ou inactif");
    }

    private void checkResourcesAvailable(TourPlanRequest req) {
        List<WorkerDocument> ws = workers.findAllById(req.workerIds());
        if (ws.size() != new HashSet<>(req.workerIds()).size()
                || ws.stream().anyMatch(w -> w.getStatus() != ResourceStatus.AVAILABLE)) {
            throw ApiException.validation("workerIds", "Un ou plusieurs ouvriers sont indisponibles");
        }
        List<VehicleDocument> vs = vehicles.findAllById(req.vehicleIds());
        if (vs.size() != new HashSet<>(req.vehicleIds()).size()
                || vs.stream().anyMatch(v -> v.getStatus() != ResourceStatus.AVAILABLE)) {
            throw ApiException.validation("vehicleIds", "Un ou plusieurs véhicules sont indisponibles");
        }
        List<BinDocument> bs = bins.findAllById(req.binIds());
        if (bs.size() != new HashSet<>(req.binIds()).size()
                || bs.stream().anyMatch(b -> b.getStatus() != ResourceStatus.AVAILABLE)) {
            throw ApiException.validation("binIds", "Une ou plusieurs bennes sont indisponibles");
        }
    }

    private void checkSameDayConflicts(TourDocument current, TourPlanRequest req) {
        List<TourDocument> sameDay = tours.findByTourDateAndStatusNot(current.getTourDate(), TourStatus.CANCELLED)
                .stream().filter(t -> !t.getId().equals(current.getId())).toList();
        Map<String, String> conflicts = new LinkedHashMap<>();
        for (TourDocument other : sameDay) {
            if (req.teamLeaderId().equals(other.getTeamLeaderId())) {
                conflicts.put("teamLeaderId", "Chef d'équipe déjà affecté à la tournée " + other.getId());
            }
            req.vehicleIds().stream().filter(other.getVehicleIds()::contains).findFirst()
                    .ifPresent(v -> conflicts.put("vehicleIds", "Véhicule " + v + " déjà affecté ce jour-là"));
            req.workerIds().stream().filter(other.getWorkerIds()::contains).findFirst()
                    .ifPresent(w -> conflicts.put("workerIds", "Ouvrier " + w + " déjà affecté ce jour-là"));
            req.binIds().stream().filter(other.getBinIds()::contains).findFirst()
                    .ifPresent(b -> conflicts.put("binIds", "Benne " + b + " déjà affectée ce jour-là"));
        }
        if (!conflicts.isEmpty()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    ErrorCode.RESOURCE_CONFLICT.defaultMessage(), conflicts);
        }
    }
}
