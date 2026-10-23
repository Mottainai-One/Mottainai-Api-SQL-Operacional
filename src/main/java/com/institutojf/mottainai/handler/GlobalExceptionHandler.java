package com.institutojf.mottainai.handler;

import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.CepNotFoundException;
import com.institutojf.mottainai.exception.CepLookupUnavailableException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.InvitationDeliveryException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Organiza os erros da API retornando respostas fáceis de entender
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvitationDeliveryException.class)
    public ResponseEntity<ApiError> handleInvitationDeliveryException(InvitationDeliveryException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 503, "SERVICE_UNAVAILABLE", exception.getMessage(),
                null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // Erro de conexão com o banco
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiError> handleSqlException(SQLException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 503, "SERVICE_UNAVAILABLE",
                "Database service is temporarily unavailable", null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // Dados duplicados ou relacionados de forma inválida
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 409, "CONFLICT",
                "The operation violates an existing data constraint", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockingFailureException(OptimisticLockingFailureException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 409, "CONFLICT",
                "The resource was updated by another request; reload it and retry", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Outro erro ao acessar o banco
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccessException(DataAccessException exception) {
        logger.error("Database access error while processing request", exception);
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 500, "INTERNAL_SERVER_ERROR",
                "Unable to process the request due to a database error", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Item não encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 404, "NOT_FOUND", exception.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // CEP não existe na base dos Correios
    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ApiError> handleCepNotFoundException(CepNotFoundException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 404, "NOT_FOUND", exception.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CepLookupUnavailableException.class)
    public ResponseEntity<ApiError> handleCepLookupUnavailableException(CepLookupUnavailableException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 503, "SERVICE_UNAVAILABLE",
                exception.getMessage(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // Dados que entram em conflito com outro cadastro
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 409, "CONFLICT", exception.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Ação não permitida pelas regras do sistema
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST", exception.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Lista os campos preenchidos de forma inválida
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST",
                "One or more fields are invalid",
                exception.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                    .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Parâmetro enviado no formato errado
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST",
                "Invalid value for parameter: " + exception.getName(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Parâmetro obrigatório não informado
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST",
                "Missing required parameter: " + exception.getParameterName(), null);
        return ResponseEntity.badRequest().body(error);
    }

    // Dados enviados no formato errado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST", "Malformed request body", null);
        return ResponseEntity.badRequest().body(error);
    }

    // Endereço da API não encontrado
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 404, "NOT_FOUND", "Resource not found", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Permissão insuficiente para a operação solicitada
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 403, "FORBIDDEN", "Access denied", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Login inválido sem informar se o CPF existe
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 401, "UNAUTHORIZED", "Invalid CPF or password",
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Argumento inválido enviado para uma regra de negócio
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 400, "BAD_REQUEST", exception.getMessage(), null);
        return ResponseEntity.badRequest().body(error);
    }

    // Erro inesperado durante a execução
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 500, "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Captura qualquer exceção restante
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception exception) {
        var error = new ApiError(LocalDateTime.now(ZoneOffset.UTC), 500, "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
