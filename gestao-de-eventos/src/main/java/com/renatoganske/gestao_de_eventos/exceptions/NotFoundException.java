package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String resource, UUID id) {
        super("%s not found with id: %s".formatted(resource, id));
    }
}
