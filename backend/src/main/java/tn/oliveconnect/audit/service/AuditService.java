package tn.oliveconnect.audit.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.oliveconnect.audit.document.AuditEventDocument;
import tn.oliveconnect.audit.repository.AuditEventRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    public void record(AuthUser actor, String action, String entityType, String entityId, String details) {
        try {
            AuditEventDocument e = new AuditEventDocument();
            e.setActorId(actor == null ? "ANONYMOUS" : actor.id());
            e.setActorRole(actor == null ? "VISITOR" : actor.role().name());
            e.setAction(action);
            e.setEntityType(entityType);
            e.setEntityId(entityId);
            e.setDetails(details);
            e.setAt(Instant.now());
            repo.save(e);
        } catch (RuntimeException ex) {
            log.warn("Échec enregistrement audit action={}", action, ex);
        }
    }
}
