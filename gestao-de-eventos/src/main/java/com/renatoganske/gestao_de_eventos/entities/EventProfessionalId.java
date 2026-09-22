package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record EventProfessionalId(UUID eventId, UUID professionalId) implements Serializable {
}
