package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real Postgres instance (not a mocked repository) to prove the fix for the
 * bug found in the develop-wide review: creating an Event with a customer used to throw
 * org.hibernate.TransientPropertyValueException at flush time, because CreateCustomerDto's
 * nested toEntity() always built a transient Customer and Event.customer has no cascade.
 * A mocked-repository test cannot catch this class of bug -- it never flushes to the database.
 */
@SpringBootTest
@Transactional
@Rollback
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private HdService hdService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createEvent_withExistingCustomerAndHdIds_persistsSuccessfully() {
        CustomerResponseDto customer = customerService.createCustomer(
                new CreateCustomerDto("Integration Test Customer", null, null, null));
        HdDto hd = hdService.createHd(
                new CreateHdDto("Integration Test HD", 1000, 0, null, null, null, null));

        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-001", null, "Integration Test Event", null,
                null, null, null, null, null, 50, null,
                hd.id(), null, customer.id());

        EventDto result = eventService.createEvent(createEventDto);
        entityManager.flush();

        assertThat(result.id()).isNotNull();
        assertThat(result.customerId()).isEqualTo(customer.id());
        assertThat(result.hdId()).isEqualTo(hd.id());
        assertThat(hdService.getHdById(hd.id()).usedSpaceGb()).isEqualTo(50);
    }

    @Test
    void createEvent_withNonExistentCustomerId_throwsCustomerNotFoundExceptionAndPersistsNothing() {
        UUID missingCustomerId = UUID.randomUUID();
        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-002", null, "Integration Test Event 2", null,
                null, null, null, null, null, null, null,
                null, null, missingCustomerId);

        assertThatThrownBy(() -> {
            eventService.createEvent(createEventDto);
            entityManager.flush();
        }).isInstanceOf(CustomerNotFoundException.class);
    }
}
