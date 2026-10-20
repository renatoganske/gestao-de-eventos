package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventEventVenueRepository extends JpaRepository<EventEventVenue, EventEventVenueId> {

    List<EventEventVenue> findByEvent_Id(UUID eventId);

    void deleteByEvent_Id(UUID eventId);
}
