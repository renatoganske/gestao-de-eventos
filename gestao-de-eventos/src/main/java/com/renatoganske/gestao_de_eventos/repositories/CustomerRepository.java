package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.dtos.OptionDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("select r.customer.id as id, count(r) as total from TB_EVENT_CUSTOMER r group by r.customer.id")
    List<EventCountProjection> countEventsGroupedByCustomer();

    @Query("select count(r) from TB_EVENT_CUSTOMER r where r.customer.id = :id")
    long countEventsByCustomerId(@Param("id") UUID id);

    /** Id and name only, for selects and filters (ADR-0029): no entity, so no association is loaded. */
    @Query("select new com.renatoganske.gestao_de_eventos.dtos.OptionDto(r.id, r.name) from TB_CUSTOMER r order by lower(r.name), r.id")
    List<OptionDto> findOptions();
}
