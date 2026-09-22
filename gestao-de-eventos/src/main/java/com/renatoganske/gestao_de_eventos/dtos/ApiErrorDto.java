package com.renatoganske.gestao_de_eventos.dtos;

import java.io.Serializable;

public record ApiErrorDto(
        int status,
        String message
) implements Serializable {
}
