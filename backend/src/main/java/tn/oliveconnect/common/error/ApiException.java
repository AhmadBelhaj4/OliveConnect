package tn.oliveconnect.common.error;

import java.util.Map;

/** Lancée par les services ; traduite en ApiError par le GlobalExceptionHandler. */
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final Map<String, String> fieldErrors;

    public ApiException(ErrorCode code, String message, Map<String, String> fieldErrors) {
        super(message != null ? message : code.defaultMessage());
        this.code = code;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message, null);
    }

    public static ApiException accessDenied(String message) {
        return new ApiException(ErrorCode.ACCESS_DENIED, message, null);
    }

    public static ApiException unauthenticated(String message) {
        return new ApiException(ErrorCode.UNAUTHENTICATED, message, null);
    }

    public static ApiException duplicate(String field, String message) {
        return new ApiException(ErrorCode.DUPLICATE_RESOURCE, message, field == null ? null : Map.of(field, message));
    }

    public static ApiException validation(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), Map.of(field, message));
    }

    public static ApiException invalidTransition(String message) {
        return new ApiException(ErrorCode.INVALID_STATE_TRANSITION, message, null);
    }
}
