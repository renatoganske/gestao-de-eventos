package com.renatoganske.gestao_de_eventos.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Test-only controller used to exercise {@link DomainExceptionHandler} in isolation,
 * since no production controller wires these exceptions in yet.
 */
@RestController
@RequestMapping("/test-exceptions")
class ThrowingTestController {

    @GetMapping("/not-found")
    public void notFound() {
        throw new CustomerNotFoundException(UUID.randomUUID());
    }

    @PostMapping("/validate")
    public void validate(@RequestBody @Valid SampleValidatedRequest request) {
    }

    record SampleValidatedRequest(@NotBlank String name) {
    }
}
