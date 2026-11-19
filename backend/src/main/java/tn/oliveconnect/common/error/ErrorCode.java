package tn.oliveconnect.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Requête invalide."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentification requise."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Accès refusé."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Ressource introuvable."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Transition de statut interdite."),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Ressource déjà affectée à une autre tournée ce jour-là."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Ressource déjà existante."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Réessayez plus tard."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne. Réessayez plus tard.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
