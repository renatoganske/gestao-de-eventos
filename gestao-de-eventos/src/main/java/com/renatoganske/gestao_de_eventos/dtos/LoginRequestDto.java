package com.renatoganske.gestao_de_eventos.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record LoginRequestDto(
        @NotBlank String username,
        @NotBlank String password
) implements Serializable {
}
