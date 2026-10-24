package com.renatoganske.gestao_de_eventos.dtos;

import java.io.Serializable;
import java.util.UUID;

/** Id and name of a record, for selects and filters that need nothing else (ADR-0029). */
public record OptionDto(
        UUID id,
        String name
) implements Serializable {
}
