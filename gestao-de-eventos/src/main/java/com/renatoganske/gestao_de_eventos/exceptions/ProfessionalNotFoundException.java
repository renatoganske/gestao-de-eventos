package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class ProfessionalNotFoundException extends NotFoundException {

    public ProfessionalNotFoundException(UUID id) {
        super("Professional", id);
    }
}
