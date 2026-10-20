package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    long countByType_Id(UUID professionalTypeId);

    long countBySpecialtyTags_Id(UUID specialtyTagId);

    @Query("select r.professional.id as id, count(r) as total from TB_EVENT_PROFESSIONAL r group by r.professional.id")
    List<EventCountProjection> countEventsGroupedByProfessional();

    @Query("select count(r) from TB_EVENT_PROFESSIONAL r where r.professional.id = :id")
    long countEventsByProfessionalId(@Param("id") UUID id);
}
