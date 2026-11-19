package tn.oliveconnect.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidator {

    private final StringRedisTemplate redisTemplate;

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache évincé pour la clé : {}", key);
        } catch (Exception e) {
            log.warn("Erreur lors de l'éviction de cache (mode dégradé sans Redis) : {}", e.getMessage());
        }
    }

    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cache évincé pour le pattern {} ({} clés)", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("Erreur lors de l'éviction par pattern : {}", e.getMessage());
        }
    }
}
