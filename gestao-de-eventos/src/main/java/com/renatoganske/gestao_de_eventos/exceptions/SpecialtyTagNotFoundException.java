package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class SpecialtyTagNotFoundException extends NotFoundException {

    public SpecialtyTagNotFoundException(UUID id) {
        super("SpecialtyTag", id);
    }
}
