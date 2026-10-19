package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record EventCustomerId(UUID eventId, UUID customerId) implements Serializable {
}
