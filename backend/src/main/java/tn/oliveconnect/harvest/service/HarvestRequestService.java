package tn.oliveconnect.harvest.service;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.audit.service.AuditService;
import tn.oliveconnect.common.enums.EntityType;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;
import tn.oliveconnect.common.enums.NotificationType;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.facade.EntityRef;
import tn.oliveconnect.common.facade.NotificationFacade;
import tn.oliveconnect.common.facade.TourFacade;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.dto.*;
import tn.oliveconnect.harvest.repository.*;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class HarvestRequestService {

    private static final String NOT_FOUND = "Demande introuvable.";
    private final HarvestRequestRepository requests;
    private final HarvestRequestSearchRepository search;
    private final OrchardRepository orchards;
    private final CacheInvalidator cache;
    private final HarvestStatusPolicy policy;
    private final NotificationFacade notifications;
    private final TourFacade tours;
    private final AuditService audit;

    public HarvestRequestService(HarvestRequestRepository requests, HarvestRequestSearchRepository search,
                                 OrchardRepository orchards, CacheInvalidator cache, HarvestStatusPolicy policy,
                                 NotificationFacade notifications, TourFacade tours, AuditService audit) {
        this.requests = requests;
        this.search = search;
        this.orchards = orchards;
        this.cache = cache;
        this.policy = policy;
        this.notifications = notifications;
        this.tours = tours;
        this.audit = audit;
    }

    public HarvestRequestResponse create(AuthUser me, HarvestRequestCreateRequest req) {
        if (req.type() == HarvestType.URGENT && (req.urgencyReason() == null || req.urgencyReason().isBlank())) {
            throw ApiException.validation("urgencyReason", "Obligatoire pour une demande urgente");
        }
        OrchardDocument orchard = orchards.findById(req.orchardId())
                .filter(o -> me.id().equals(o.getFarmerId()))
                .orElseThrow(() -> ApiException.notFound("Verger introuvable."));

        HarvestRequestDocument d = new HarvestRequestDocument();
        d.setFarmerId(me.id());
        d.setOrchardId(orchard.getId());
        d.setOrchardName(orchard.getName());
        d.setType(req.type());
        d.setUrgencyReason(req.type() == HarvestType.URGENT ? req.urgencyReason().trim() : null);
        d.setStatus(HarvestStatus.SUBMITTED);
        d.setRequestedAt(Instant.now());
        d.setPreferredDate(req.preferredDate());
        d.setEstimatedQuantityKg(req.estimatedQuantityKg());
        d = requests.save(d);
        cache.orchardChanged(orchard.getId());
        cache.mapChanged();
        return HarvestRequestResponse.from(d);
    }

    public PageResponse<HarvestRequestResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.from(requests.findByFarmerId(me.id(), pageable), HarvestRequestResponse::from);
    }

    public PageResponse<HarvestRequestResponse> queue(HarvestStatus status, LocalDate from, LocalDate to, Pageable p) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.validation("from", "from doit être ≤ to");
        }
        return PageResponse.from(search.search(status, from, to, p), HarvestRequestResponse::from);
    }

    public HarvestRequestResponse get(AuthUser me, String id) {
        HarvestRequestDocument d = requests.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        boolean owner = me.hasRole(Role.FARMER) && me.id().equals(d.getFarmerId());
        if (!owner && !me.isStaff()) throw ApiException.notFound(NOT_FOUND);
        return HarvestRequestResponse.from(d);
    }

    // --- A8 : review ---------------------------------------------------------------
    public HarvestRequestResponse review(AuthUser me, String id, ReviewRequest req) {
        if (req.decision() == ReviewDecision.REJECT && isBlank(req.reason())) {
            throw ApiException.validation("reason", "Motif obligatoire pour un rejet");
        }
        HarvestRequestDocument d = requests.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        // §4 : « 409 si ≠ SUBMITTED ». On le vérifie explicitement car la policy autorise aussi
        // SCHEDULED -> APPROVED (transition système, pas une revue).
        if (d.getStatus() != HarvestStatus.SUBMITTED) {
            throw ApiException.invalidTransition("Seule une demande SUBMITTED peut être revue (statut actuel : "
                    + d.getStatus() + ").");
        }
        HarvestStatus target = req.decision() == ReviewDecision.APPROVE ? HarvestStatus.APPROVED : HarvestStatus.REJECTED;
        policy.assertTransition(d.getStatus(), target);

        d.setStatus(target);
        d.setReviewedBy(me.id());
        d.setReviewNote(trimOrNull(req.reason()));
        d = requests.save(d);

        boolean approved = target == HarvestStatus.APPROVED;
        notifications.notify(d.getFarmerId(),
                approved ? NotificationType.REQUEST_APPROVED : NotificationType.REQUEST_REJECTED,
                approved ? "Votre demande du %s est approuvée.".formatted(d.getPreferredDate())
                         : "Votre demande du %s est rejetée : %s".formatted(d.getPreferredDate(), d.getReviewNote()),
                new EntityRef(EntityType.HARVEST_REQUEST, d.getId()));
        audit.record(me, approved ? "HARVEST_REQUEST_APPROVED" : "HARVEST_REQUEST_REJECTED",
                "HARVEST_REQUEST", d.getId(), d.getReviewNote());
        cache.orchardChanged(d.getOrchardId());
        cache.mapChanged();
        return HarvestRequestResponse.from(d);
    }

    // --- A8 : cancel ---------------------------------------------------------------
    public HarvestRequestResponse cancel(AuthUser me, String id, CancelRequest req) {
        HarvestRequestDocument d = loadForOwnerOrManager(me, id);
        policy.assertTransition(d.getStatus(), HarvestStatus.CANCELLED); // 409 si IN_PROGRESS ou état final

        if (d.getStatus() == HarvestStatus.SCHEDULED) {
            tours.removeStop(d.getId());   // §4 : l'arrêt est retiré de la tournée (B, via façade)
        }
        d.setStatus(HarvestStatus.CANCELLED);
        d.setCancelReason(req == null ? null : trimOrNull(req.reason()));
        d.setScheduledTourId(null);
        d.setScheduledAt(null);
        d = requests.save(d);

        audit.record(me, "HARVEST_REQUEST_CANCELLED", "HARVEST_REQUEST", d.getId(), d.getCancelReason());
        cache.orchardChanged(d.getOrchardId());
        cache.mapChanged();
        return HarvestRequestResponse.from(d);
    }

    // --- A8 : PATCH ----------------------------------------------------------------
    public HarvestRequestResponse patch(AuthUser me, String id, HarvestRequestPatchRequest req) {
        HarvestRequestDocument d = requests.findById(id)
                .filter(r -> me.id().equals(r.getFarmerId()))          // propriétaire sinon 404
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (!policy.isEditable(d.getStatus())) {
            throw ApiException.invalidTransition("Modification impossible au statut " + d.getStatus() + ".");
        }
        HarvestType newType = req.type() != null ? req.type() : d.getType();
        String newReason = req.urgencyReason() != null ? trimOrNull(req.urgencyReason()) : d.getUrgencyReason();
        if (newType == HarvestType.URGENT && isBlank(newReason)) {
            throw ApiException.validation("urgencyReason", "Obligatoire pour une demande urgente");
        }
        d.setType(newType);
        d.setUrgencyReason(newType == HarvestType.URGENT ? newReason : null);
        if (req.preferredDate() != null) d.setPreferredDate(req.preferredDate());
        if (req.estimatedQuantityKg() != null) d.setEstimatedQuantityKg(req.estimatedQuantityKg());
        d = requests.save(d);   // le statut ne change pas (aucune transition APPROVED -> SUBMITTED au §4)
        cache.orchardChanged(d.getOrchardId());
        return HarvestRequestResponse.from(d);
    }

    // --- helpers -------------------------------------------------------------------
    /** FARMER : seulement les siennes (404 sinon) ; COLLECTION_MANAGER : toutes. */
    private HarvestRequestDocument loadForOwnerOrManager(AuthUser me, String id) {
        HarvestRequestDocument d = requests.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        boolean owner = me.hasRole(Role.FARMER) && me.id().equals(d.getFarmerId());
        if (!owner && !me.hasRole(Role.COLLECTION_MANAGER)) throw ApiException.notFound(NOT_FOUND);
        return d;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trimOrNull(String s) { return isBlank(s) ? null : s.trim(); }
}
