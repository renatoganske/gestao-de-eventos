package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventProfessionalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventProfessionalRepository extends JpaRepository<EventProfessional, EventProfessionalId> {

    List<EventProfessional> findByEvent_Id(UUID eventId);

    void deleteByEvent_Id(UUID eventId);
}
