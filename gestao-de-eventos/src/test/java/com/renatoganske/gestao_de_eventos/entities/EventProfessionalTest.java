package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventProfessionalTest {

    @Test
    void shouldAssociateProfessionalWithEventThroughRoleInEvent() {
        UUID eventId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        Event event = Event.builder()
                .id(eventId)
                .name("Casamento Ana e Bruno")
                .build();

        Professional professional = Professional.builder()
                .id(professionalId)
                .name("Fernanda Souza")
                .build();

        EventProfessional eventProfessional = EventProfessional.builder()
                .id(new EventProfessionalId(eventId, professionalId))
                .event(event)
                .professional(professional)
                .roleInEvent("Segundo fotógrafo")
                .build();

        event.setEventProfessionals(List.of(eventProfessional));
        professional.setEventProfessionals(List.of(eventProfessional));

        EventDto eventDto = event.toDTO();
        ProfessionalDto professionalDto = professional.toResponseDto();

        assertThat(eventDto.eventProfessionals()).hasSize(1);
        assertThat(eventDto.eventProfessionals().get(0).getRoleInEvent()).isEqualTo("Segundo fotógrafo");
        assertThat(eventDto.eventProfessionals().get(0).getProfessional()).isEqualTo(professional);

        assertThat(professionalDto.eventProfessionals()).hasSize(1);
        assertThat(professionalDto.eventProfessionals().get(0).getRoleInEvent()).isEqualTo("Segundo fotógrafo");
        assertThat(professionalDto.eventProfessionals().get(0).getEvent()).isEqualTo(event);

        assertThat(eventProfessional.getId().eventId()).isEqualTo(eventId);
        assertThat(eventProfessional.getId().professionalId()).isEqualTo(professionalId);
    }
}
