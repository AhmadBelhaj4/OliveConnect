package tn.oliveconnect.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final tn.oliveconnect.audit.service.AuditService audit;

    public GlobalExceptionHandler(tn.oliveconnect.audit.service.AuditService audit) {
        this.audit = audit;
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(code.status()).body(ApiError.of(code, message, fieldErrors));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex) {
        return build(ex.getCode(), ex.getMessage(), ex.getFieldErrors());
    }

    /** @Valid sur un @RequestBody : on remplit fieldErrors (premier message par champ). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
          .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return build(ErrorCode.VALIDATION_ERROR, null, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "JSON mal formé ou valeur invalide.", null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleParams(Exception ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Paramètre invalide ou manquant.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Méthode HTTP non supportée.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, null, null);
    }

    /** Index unique violé (ex. email) : filet de sécurité si deux inscriptions arrivent en même temps. */
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiError> handleDuplicate(DuplicateKeyException ex) {
        return build(ErrorCode.DUPLICATE_RESOURCE, null, null);
    }

    /** @PreAuthorize refusé -> 403 (l'utilisateur EST connecté, sinon le filtre aurait renvoyé 401). */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ApiError> handleDenied(Exception ex, jakarta.servlet.http.HttpServletRequest req) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        tn.oliveconnect.security.AuthUser actor = (auth != null && auth.getPrincipal() instanceof tn.oliveconnect.security.AuthUser u) ? u : null;
        audit.record(actor, "ACCESS_DENIED", "HTTP_ENDPOINT", req.getMethod() + " " + req.getRequestURI(), null);
        return build(ErrorCode.ACCESS_DENIED, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuth(AuthenticationException ex) {
        return build(ErrorCode.UNAUTHENTICATED, null, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Erreur inattendue", ex);
        return build(ErrorCode.INTERNAL_ERROR, null, null);
    }
}
