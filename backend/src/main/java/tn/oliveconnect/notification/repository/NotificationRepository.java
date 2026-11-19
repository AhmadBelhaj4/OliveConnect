package tn.oliveconnect.notification.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.notification.document.NotificationDocument;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    Page<NotificationDocument> findByUserId(String userId, Pageable pageable);
    Page<NotificationDocument> findByUserIdAndReadAtIsNull(String userId, Pageable pageable);
    Optional<NotificationDocument> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
