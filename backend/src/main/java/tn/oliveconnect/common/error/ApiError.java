package tn.oliveconnect.common.error;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Enveloppe de TOUTES les réponses 4xx/5xx. Jamais de stack trace ni de chaîne de connexion. */
public record ApiError(Instant timestamp, int status, String code, String message,
                       Map<String, String> fieldErrors) {

    public static ApiError of(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().truncatedTo(ChronoUnit.SECONDS),
                code.status().value(), code.name(),
                message != null ? message : code.defaultMessage(),
                fieldErrors != null ? fieldErrors : Map.of());   // {} et jamais null
    }
}
