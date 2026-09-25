package com.renatoganske.gestao_de_eventos.exceptions;

import java.util.UUID;

public class EventVenueNotFoundException extends NotFoundException {

    public EventVenueNotFoundException(UUID id) {
        super("EventVenue", id);
    }
}
