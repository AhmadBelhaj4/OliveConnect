package tn.oliveconnect.security;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;

@Component
public class LoginRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;
    public LoginRateLimiter(StringRedisTemplate redis) { this.redis = redis; }

    private String key(String ip, String email) {
        return "oliveconnect:rl:login:" + ip + "+" + email;
    }

    public void checkAllowed(String ip, String email) {
        try {
            String val = redis.opsForValue().get(key(ip, email));
            if (val != null && Long.parseLong(val) >= MAX_FAILURES) {
                throw new ApiException(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.defaultMessage(), null);
            }
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Redis indisponible pour le rate-limit login — poursuite sans blocage");
        }
    }

    public void recordFailure(String ip, String email) {
        try {
            String k = key(ip, email);
            Long count = redis.opsForValue().increment(k);
            if (count != null && count == 1L) redis.expire(k, WINDOW);
        } catch (RuntimeException e) {
            log.warn("Redis indisponible (recordFailure ignoré)");
        }
    }

    public void reset(String ip, String email) {
        try { redis.delete(key(ip, email)); } catch (RuntimeException ignored) {}
    }
}
