package tn.oliveconnect.notification.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.EntityType;
import tn.oliveconnect.common.enums.NotificationType;

@Document("notifications")
@Getter @Setter @NoArgsConstructor
public class NotificationDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String userId;     // destinataire
    private NotificationType type;
    private String message;                                             // en français
    private EntityType entityType;                                      // HARVEST_REQUEST, TOUR, ALERT
    @Field(targetType = FieldType.OBJECT_ID) private String entityId;
    private Instant readAt;
    @CreatedDate private Instant createdAt;
}
