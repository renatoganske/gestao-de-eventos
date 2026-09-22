package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class HdNotFoundException extends NotFoundException {

    public HdNotFoundException(UUID id) {
        super("Hd", id);
    }
}
