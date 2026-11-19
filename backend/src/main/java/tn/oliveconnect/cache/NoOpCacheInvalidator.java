package tn.oliveconnect.cache;

import org.springframework.stereotype.Component;
import tn.oliveconnect.common.facade.CacheInvalidator;

/**
 * Implémentation sans effet livrée en S1 (tâche B4).
 * Remplacée en S3 (tâche B12) par l'invalidation Redis réelle. Ne lève jamais d'exception.
 */
@Component
public class NoOpCacheInvalidator implements CacheInvalidator {

    @Override
    public void orchardChanged(String orchardId) {
        // S1 : no-op (Redis arrive en S3)
    }

    @Override
    public void mapChanged() {
        // S1 : no-op
    }

    @Override
    public void dashboardChanged() {
        // S1 : no-op
    }
}
