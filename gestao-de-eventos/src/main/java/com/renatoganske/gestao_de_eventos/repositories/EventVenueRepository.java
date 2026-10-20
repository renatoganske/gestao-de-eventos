package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventVenueRepository extends JpaRepository<EventVenue, UUID> {

    @Query("select r.eventVenue.id as id, count(r) as total from TB_EVENT_EVENT_VENUE r group by r.eventVenue.id")
    List<EventCountProjection> countEventsGroupedByEventVenue();

    @Query("select count(r) from TB_EVENT_EVENT_VENUE r where r.eventVenue.id = :id")
    long countEventsByEventVenueId(@Param("id") UUID id);
}
