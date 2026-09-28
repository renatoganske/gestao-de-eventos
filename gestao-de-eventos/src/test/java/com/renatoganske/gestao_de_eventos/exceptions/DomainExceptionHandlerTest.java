package com.renatoganske.gestao_de_eventos.exceptions;

import com.renatoganske.gestao_de_eventos.dtos.ApiErrorDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test calling {@link DomainExceptionHandler}'s handler methods directly, instead of going
 * through a real/test @RestController — avoids adding a throwaway controller that Spring Boot's
 * default component scan would also pick up in full-context (@SpringBootTest) tests.
 */
class DomainExceptionHandlerTest {

    private final DomainExceptionHandler handler = new DomainExceptionHandler();

    @Test
    void mapsNotFoundExceptionTo404WithMessage() {
        ResponseEntity<ApiErrorDto> response = handler.handleNotFound(new CustomerNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("Customer not found");
    }

    @Test
    void mapsTooManyLoginAttemptsExceptionTo429WithMessage() {
        ResponseEntity<ApiErrorDto> response = handler.handleTooManyLoginAttempts(
                new TooManyLoginAttemptsException("Too many failed login attempts. Try again in a few minutes."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getBody().message()).contains("Too many failed login attempts");
    }

    @Test
    void mapsResourceInUseExceptionTo409WithMessage() {
        ResponseEntity<ApiErrorDto> response = handler.handleResourceInUse(
                new ResourceInUseException("Specialty tag is in use by 4 professional(s) and cannot be deleted."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("in use by 4 professional(s)");
    }

    @Test
    void mapsFieldValidationErrorsTo400WithMessage() throws NoSuchMethodException {
        MethodArgumentNotValidException ex = notValidException(
                List.of(new FieldError("target", "name", "must not be blank")),
                List.of());

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, null, HttpStatus.BAD_REQUEST, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorDto body = (ApiErrorDto) response.getBody();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.message()).contains("name: must not be blank");
    }

    @Test
    void mapsGlobalValidationErrorsTo400WithMessage() throws NoSuchMethodException {
        MethodArgumentNotValidException ex = notValidException(
                List.of(),
                List.of(new ObjectError("target", "start date must be before end date")));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, null, HttpStatus.BAD_REQUEST, null);

        ApiErrorDto body = (ApiErrorDto) response.getBody();
        assertThat(body.message()).contains("start date must be before end date");
    }

    private MethodArgumentNotValidException notValidException(List<FieldError> fieldErrors, List<ObjectError> globalErrors)
            throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        fieldErrors.forEach(bindingResult::addError);
        globalErrors.forEach(bindingResult::addError);

        Method method = DummyTarget.class.getDeclaredMethod("dummy", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        return new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private static final class DummyTarget {
        void dummy(String name) {
        }
    }
}
