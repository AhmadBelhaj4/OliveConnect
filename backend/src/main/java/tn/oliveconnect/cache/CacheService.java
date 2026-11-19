package tn.oliveconnect.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Cache-aside jetable (§3.3) : si Redis tombe, l'application lit MongoDB et renvoie 200 sans jamais planter.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public CacheService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return mapper.readValue(cached, type);
        } catch (Exception e) {
            log.warn("Lecture cache Redis ignorée pour {} : {}", key, e.getMessage());
        }

        T fresh = loader.get();   // MongoDB = source de vérité

        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(fresh), ttl);
        } catch (Exception e) {
            log.warn("Écriture cache Redis ignorée pour {} : {}", key, e.getMessage());
        }
        return fresh;
    }

    public static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
