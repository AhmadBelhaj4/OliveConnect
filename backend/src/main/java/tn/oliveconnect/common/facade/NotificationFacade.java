package tn.oliveconnect.common.facade;

import tn.oliveconnect.common.enums.NotificationType;

public interface NotificationFacade {
    void notify(String userId, NotificationType type, String message, EntityRef ref);
}
