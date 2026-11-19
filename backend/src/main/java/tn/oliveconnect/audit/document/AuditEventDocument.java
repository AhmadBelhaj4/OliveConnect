package tn.oliveconnect.audit.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_events")
@Getter @Setter @NoArgsConstructor
public class AuditEventDocument {
    @Id private String id;
    private String actorId;
    private String actorRole;
    private String action;       // HARVEST_REQUEST_APPROVED, HARVEST_REQUEST_CANCELLED, ACCESS_DENIED, ROLE_CHANGED
    private String entityType;
    private String entityId;
    private String details;      // jamais de mot de passe ni de token
    private Instant at;
}
