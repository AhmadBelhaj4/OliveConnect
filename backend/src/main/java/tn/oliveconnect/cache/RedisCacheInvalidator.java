package tn.oliveconnect.cache;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.facade.CacheInvalidator;

/** Clés figées §3.3 : oliveconnect:map:*, oliveconnect:orchard:summary:<id>, oliveconnect:dashboard:* */
@Component
public class RedisCacheInvalidator implements CacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidator.class);
    private final StringRedisTemplate redis;

    public RedisCacheInvalidator(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public void orchardChanged(String orchardId) {
        safeDelete("oliveconnect:orchard:summary:" + orchardId);
    }

    @Override
    public void mapChanged() {
        safeDeletePattern("oliveconnect:map:*");
    }

    @Override
    public void dashboardChanged() {
        safeDeletePattern("oliveconnect:dashboard:*");
    }

    private void safeDelete(String key) {
        try { redis.delete(key); } catch (RuntimeException e) { log.warn("Invalidation Redis ignorée ({})", key); }
    }

    private void safeDeletePattern(String pattern) {
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (RuntimeException e) {
            log.warn("Invalidation Redis ignorée ({})", pattern);
        }
    }
}
