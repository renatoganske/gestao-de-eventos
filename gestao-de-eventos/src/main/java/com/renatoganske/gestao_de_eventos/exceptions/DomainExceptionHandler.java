package com.renatoganske.gestao_de_eventos.exceptions;

import com.renatoganske.gestao_de_eventos.dtos.ApiErrorDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class DomainExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(NotFoundException ex) {
        ApiErrorDto body = new ApiErrorDto(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDto> handleAuthenticationException(AuthenticationException ex) {
        ApiErrorDto body = new ApiErrorDto(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiErrorDto> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex) {
        ApiErrorDto body = new ApiErrorDto(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ApiErrorDto> handleResourceInUse(ResourceInUseException ex) {
        ApiErrorDto body = new ApiErrorDto(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiErrorDto body = new ApiErrorDto(HttpStatus.CONFLICT.value(),
                "Operation conflicts with existing data (duplicate value or record referenced by other data).");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(fieldError -> "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage())),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(ObjectError::getDefaultMessage))
                .collect(Collectors.joining("; "));
        ApiErrorDto body = new ApiErrorDto(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
