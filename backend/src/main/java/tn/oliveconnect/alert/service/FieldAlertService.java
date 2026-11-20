package tn.oliveconnect.alert.service;

import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.alert.document.FieldAlertDocument;
import tn.oliveconnect.alert.dto.AlertDtos.*;
import tn.oliveconnect.alert.repository.FieldAlertRepository;
import tn.oliveconnect.alert.repository.FieldAlertSearchRepository;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.EntityRef;
import tn.oliveconnect.common.facade.NotificationFacade;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class FieldAlertService {

    private static final String NOT_FOUND = "Alerte introuvable.";
    private final FieldAlertRepository alerts;
    private final FieldAlertSearchRepository search;
    private final OrchardRepository orchards;
    private final NotificationFacade notifications;

    public FieldAlertService(FieldAlertRepository alerts, FieldAlertSearchRepository search,
                             OrchardRepository orchards, NotificationFacade notifications) {
        this.alerts = alerts; this.search = search; this.orchards = orchards; this.notifications = notifications;
    }

    public FieldAlertResponse create(AuthUser me, FieldAlertCreateRequest req) {
        // Le verger doit appartenir à l'agriculteur (sinon 404)
        var orchard = orchards.findById(req.orchardId())
                .filter(o -> me.id().equals(o.getFarmerId()))
                .orElseThrow(() -> ApiException.notFound("Verger introuvable."));

        FieldAlertDocument a = new FieldAlertDocument();
        a.setFarmerId(me.id());
        a.setOrchardId(orchard.getId());
        a.setType(req.type());
        a.setDescription(req.description().trim());
        a.setLocation(req.location().toGeoJson("location"));
        a.setPriority(req.priority());
        a.setStatus(AlertStatus.OPEN);
        return FieldAlertResponse.from(alerts.save(a));
    }

    public PageResponse<FieldAlertResponse> list(AuthUser me, AlertStatus status, AlertPriority priority, Pageable p) {
        String scopedFarmer = me.hasRole(Role.FARMER) ? me.id() : null;
        return PageResponse.from(search.search(scopedFarmer, status, priority, p), FieldAlertResponse::from);
    }

    public FieldAlertResponse get(AuthUser me, String id) {
        FieldAlertDocument a = alerts.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        boolean owner = me.hasRole(Role.FARMER) && me.id().equals(a.getFarmerId());
        if (!owner && !me.isStaff()) throw ApiException.notFound(NOT_FOUND);
        return FieldAlertResponse.from(a);
    }

    public FieldAlertResponse acknowledge(String id) {
        FieldAlertDocument a = alerts.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (a.getStatus() != AlertStatus.OPEN) {
            throw ApiException.invalidTransition("Seule une alerte OPEN peut être prise en charge.");
        }
        a.setStatus(AlertStatus.ACKNOWLEDGED);
        a.setAcknowledgedAt(Instant.now());
        a = alerts.save(a);
        notifications.notify(a.getFarmerId(), NotificationType.ALERT_ACKNOWLEDGED,
                "Votre alerte terrain a été prise en charge.", new EntityRef(EntityType.ALERT, a.getId()));
        return FieldAlertResponse.from(a);
    }

    public FieldAlertResponse resolve(String id, ResolveAlertRequest req) {
        FieldAlertDocument a = alerts.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (a.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw ApiException.invalidTransition("Seule une alerte ACKNOWLEDGED peut être résolue.");
        }
        a.setStatus(AlertStatus.RESOLVED);
        a.setResolvedAt(Instant.now());
        a.setResolutionNote(req.note().trim());
        a = alerts.save(a);
        notifications.notify(a.getFarmerId(), NotificationType.ALERT_RESOLVED,
                "Votre alerte terrain est résolue : " + a.getResolutionNote(), new EntityRef(EntityType.ALERT, a.getId()));
        return FieldAlertResponse.from(a);
    }

    public FieldAlertResponse dismiss(String id, DismissAlertRequest req) {
        FieldAlertDocument a = alerts.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (a.getStatus() != AlertStatus.OPEN && a.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw ApiException.invalidTransition("Seule une alerte OPEN ou ACKNOWLEDGED peut être écartée.");
        }
        a.setStatus(AlertStatus.DISMISSED);
        a.setResolvedAt(Instant.now());
        a.setResolutionNote(req.reason().trim());
        a = alerts.save(a);
        notifications.notify(a.getFarmerId(), NotificationType.ALERT_DISMISSED,
                "Votre alerte terrain a été écartée : " + a.getResolutionNote(), new EntityRef(EntityType.ALERT, a.getId()));
        return FieldAlertResponse.from(a);
    }
}
