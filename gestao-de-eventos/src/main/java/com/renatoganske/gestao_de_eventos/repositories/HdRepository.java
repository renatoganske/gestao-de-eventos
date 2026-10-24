package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.dtos.OptionDto;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HdRepository extends JpaRepository<Hd, UUID> {

    @Query("select r.hd.id as id, count(r) as total from TB_EVENT r where r.hd is not null group by r.hd.id")
    List<EventCountProjection> countEventsGroupedByHd();

    @Query("select count(r) from TB_EVENT r where r.hd.id = :id")
    long countEventsByHdId(@Param("id") UUID id);

    /** Id and name only, for selects and filters (ADR-0029): no entity, so no association is loaded. */
    @Query("select new com.renatoganske.gestao_de_eventos.dtos.OptionDto(r.id, r.name) from TB_HD r order by lower(r.name), r.id")
    List<OptionDto> findOptions();
}
