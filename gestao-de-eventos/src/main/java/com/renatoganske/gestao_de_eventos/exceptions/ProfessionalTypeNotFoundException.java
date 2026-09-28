package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class ProfessionalTypeNotFoundException extends NotFoundException {

    public ProfessionalTypeNotFoundException(UUID id) {
        super("ProfessionalType", id);
    }
}
