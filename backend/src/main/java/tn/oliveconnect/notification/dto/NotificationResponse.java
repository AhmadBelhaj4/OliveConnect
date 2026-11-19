package tn.oliveconnect.notification.dto;

import java.time.Instant;
import tn.oliveconnect.common.enums.EntityType;
import tn.oliveconnect.common.enums.NotificationType;
import tn.oliveconnect.notification.document.NotificationDocument;

public record NotificationResponse(
        String id, NotificationType type, String message,
        EntityType entityType, String entityId, Instant readAt, Instant createdAt) {

    public static NotificationResponse from(NotificationDocument n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(),
                n.getEntityType(), n.getEntityId(), n.getReadAt(), n.getCreatedAt());
    }
}
