package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(UUID id) {
        super("Customer", id);
    }
}
