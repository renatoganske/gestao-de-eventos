package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.EventCustomer;
import com.renatoganske.gestao_de_eventos.entities.EventCustomerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventCustomerRepository extends JpaRepository<EventCustomer, EventCustomerId> {

    List<EventCustomer> findByEvent_Id(UUID eventId);

    void deleteByEvent_Id(UUID eventId);
}
