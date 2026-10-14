package com.minimarket.infrastructure.exception;

import com.minimarket.domain.exception.BusinessException;
import com.minimarket.domain.exception.AuthenticationFailedException;
import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.exception.ResourceNotFoundException;
import com.minimarket.domain.exception.ForbiddenOperationException;
import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiError;
import com.minimarket.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_KEY = "correlationId";

    private final ApiResponseFactory responseFactory;
    private final Clock clock;

    public GlobalExceptionHandler(ApiResponseFactory responseFactory, Clock clock) {
        this.responseFactory = responseFactory;
        this.clock = clock;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAuthenticationFailed(
            AuthenticationFailedException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception.getCode(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        log.debug("Resource not found. code={}", exception.getCode());
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), exception.getCode(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConflict(
            ConflictException exception, HttpServletRequest request) {
        log.warn("Business conflict. code={}", exception.getCode());
        return error(HttpStatus.CONFLICT, exception.getMessage(), exception.getCode(), request, null);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleForbiddenOperation(
            ForbiddenOperationException exception, HttpServletRequest request) {
        log.warn("Forbidden business operation. code={}", exception.getCode());
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), exception.getCode(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBusiness(
            BusinessException exception, HttpServletRequest request) {
        log.debug("Business rule rejected request. code={}", exception.getCode());
        return error(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), exception.getCode(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Error de validación", "VALIDATION_ERROR", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        lastPathSegment(violation.getPropertyPath().toString()), violation.getMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Error de validación", "VALIDATION_ERROR", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "JSON inválido", "INVALID_JSON", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", "METHOD_NOT_ALLOWED", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleStaticResourceNotFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Recurso no encontrado", "RESOURCE_NOT_FOUND", request, null);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<ApiError>> handleInvalidParameter(
            Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Parámetro inválido", "INVALID_PARAMETER", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_REQUEST", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database integrity conflict");
        return error(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes",
                "DATA_CONFLICT", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("Unexpected error processing request", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado",
                "INTERNAL_ERROR", request, null);
    }

    private ResponseEntity<ApiResponse<ApiError>> error(
            HttpStatus status,
            String message,
            String code,
            HttpServletRequest request,
            List<ValidationError> errors) {
        ApiError detail = new ApiError(
                code,
                Instant.now(clock),
                request.getRequestURI(),
                MDC.get(CORRELATION_ID_KEY),
                errors);
        return responseFactory.error(status, message, detail);
    }

    private static ValidationError toValidationError(FieldError error) {
        String field = error.getField().startsWith("data.")
                ? error.getField().substring("data.".length())
                : error.getField();
        return new ValidationError(field, error.getDefaultMessage());
    }

    private static String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }
}
