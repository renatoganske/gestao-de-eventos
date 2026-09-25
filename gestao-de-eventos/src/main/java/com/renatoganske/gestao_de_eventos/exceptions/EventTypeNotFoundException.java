package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class EventTypeNotFoundException extends NotFoundException {

    public EventTypeNotFoundException(UUID id) {
        super("EventType", id);
    }
}
