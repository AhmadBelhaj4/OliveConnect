package tn.oliveconnect.notification.service;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.notification.document.NotificationDocument;
import tn.oliveconnect.notification.dto.NotificationResponse;
import tn.oliveconnect.notification.repository.NotificationRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public PageResponse<NotificationResponse> list(AuthUser me, boolean unreadOnly, Pageable pageable) {
        Page<NotificationDocument> page = unreadOnly
                ? repo.findByUserIdAndReadAtIsNull(me.id(), pageable)
                : repo.findByUserId(me.id(), pageable);
        return PageResponse.from(page, NotificationResponse::from);
    }

    public NotificationResponse markRead(AuthUser me, String id) {
        NotificationDocument n = repo.findByIdAndUserId(id, me.id())
                .orElseThrow(() -> ApiException.notFound("Notification introuvable."));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            n = repo.save(n);
        }
        return NotificationResponse.from(n);
    }
}
