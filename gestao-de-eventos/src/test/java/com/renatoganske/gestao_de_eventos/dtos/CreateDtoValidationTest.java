package com.renatoganske.gestao_de_eventos.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the bean validation gap found in the develop-wide review is closed: every
 * Create*Dto now rejects a missing/blank "name", the field that is "not null" in the
 * database for all five resources (V1__baseline.sql). Before this fix, @Valid on the
 * controllers produced zero violations for these payloads and the request only failed
 * downstream with a raw DataIntegrityViolationException (500), not the 400 ApiErrorDto
 * DomainExceptionHandler already builds for MethodArgumentNotValidException.
 */
class CreateDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createCustomerDto_withNullOrBlankName_producesViolation() {
        assertThat(validator.validate(new CreateCustomerDto(null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new CreateCustomerDto("  ", null, null, null))).isNotEmpty();
        assertThat(validator.validate(new CreateCustomerDto("Maria Silva", null, null, null))).isEmpty();
    }

    @Test
    void createHdDto_withNullOrBlankName_producesViolation() {
        Set<ConstraintViolation<CreateHdDto>> violations =
                validator.validate(new CreateHdDto(null, null, null, null, null, null, null));

        assertThat(violations).isNotEmpty();
        assertThat(validator.validate(new CreateHdDto("HD Externo 1", null, null, null, null, null, null)))
                .isEmpty();
    }

    @Test
    void createEventVenueDto_withNullOrBlankName_producesViolation() {
        assertThat(validator.validate(new CreateEventVenueDto(null, null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new CreateEventVenueDto("Buffet Jardim das Rosas", null, null, null, null)))
                .isEmpty();
    }

    @Test
    void createProfessionalDto_withNullOrBlankName_producesViolation() {
        assertThat(validator.validate(new CreateProfessionalDto(null, null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new CreateProfessionalDto("Joao Fotografo", null, null, null, null)))
                .isEmpty();
    }

    @Test
    void createEventDto_withNullOrBlankName_producesViolation() {
        CreateEventDto withoutName = new CreateEventDto(
                "EVT-001", null, null, null, null, null, null, null, null, null, null, null, null, null);
        CreateEventDto withName = new CreateEventDto(
                "EVT-001", null, "Casamento Maria e Joao", null, null, null, null, null, null, null, null,
                null, null, null);

        assertThat(validator.validate(withoutName)).isNotEmpty();
        assertThat(validator.validate(withName)).isEmpty();
    }
}
