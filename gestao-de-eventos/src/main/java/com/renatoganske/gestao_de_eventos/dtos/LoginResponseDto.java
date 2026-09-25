package com.renatoganske.gestao_de_eventos.dtos;

import java.io.Serializable;

public record LoginResponseDto(
        String token
) implements Serializable {
}
