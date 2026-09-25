package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class EventNotFoundException extends NotFoundException {

    public EventNotFoundException(UUID id) {
        super("Event", id);
    }
}
