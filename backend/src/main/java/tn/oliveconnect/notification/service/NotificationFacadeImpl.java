package tn.oliveconnect.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.NotificationType;
import tn.oliveconnect.common.facade.EntityRef;
import tn.oliveconnect.common.facade.NotificationFacade;
import tn.oliveconnect.notification.document.NotificationDocument;
import tn.oliveconnect.notification.repository.NotificationRepository;

/**
 * Appelée par A (review, alertes) et par B (tournées). Une notification ratée ne doit pas annuler
 * l'action métier déjà écrite dans MongoDB : on journalise et on continue.
 */
@Service
public class NotificationFacadeImpl implements NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationFacadeImpl.class);
    private final NotificationRepository repo;

    public NotificationFacadeImpl(NotificationRepository repo) { this.repo = repo; }

    @Override
    public void notify(String userId, NotificationType type, String message, EntityRef ref) {
        try {
            NotificationDocument n = new NotificationDocument();
            n.setUserId(userId);
            n.setType(type);
            n.setMessage(message);
            n.setEntityType(ref.entityType());
            n.setEntityId(ref.entityId());
            repo.save(n);
        } catch (RuntimeException e) {
            log.warn("Notification non enregistrée : type={} entityType={} entityId={}",
                    type, ref.entityType(), ref.entityId(), e);
        }
    }
}
