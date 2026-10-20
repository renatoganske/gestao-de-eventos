package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GDE-58: the lists of customers, professionals, venues and HDs carry {@code eventCount}. Runs on
 * the real Postgres because the count is a GROUP BY over the association tables -- a mock cannot
 * prove the JPQL, and the dev database also holds imported data, so the assertions look only at the
 * fixtures created here (tagged with a random suffix), never at the whole list.
 *
 * Three of each kind: {@code busy} is in two events, {@code single} in one, {@code idle} in none --
 * the 0 matters because the GROUP BY returns no row for it.
 */
@SpringBootTest
@Transactional
@Rollback
class EventCountIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private CustomerService customerService;
    @Autowired private ProfessionalService professionalService;
    @Autowired private EventVenueService eventVenueService;
    @Autowired private HdService hdService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private EventVenueRepository eventVenueRepository;
    @Autowired private HdRepository hdRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private final String tag = "cnt" + UUID.randomUUID().toString().substring(0, 8);

    private UUID busyCustomer, singleCustomer, idleCustomer;
    private UUID busyProfessional, singleProfessional, idleProfessional;
    private UUID busyVenue, singleVenue, idleVenue;
    private UUID busyHd, singleHd, idleHd;

    @BeforeEach
    void setUp() {
        busyCustomer = customer("busy");
        singleCustomer = customer("single");
        idleCustomer = customer("idle");
        busyProfessional = professional("busy");
        singleProfessional = professional("single");
        idleProfessional = professional("idle");
        busyVenue = venue("busy");
        singleVenue = venue("single");
        idleVenue = venue("idle");
        busyHd = hd("busy");
        singleHd = hd("single");
        idleHd = hd("idle");

        // two events for every "busy" and "single" fixture; the second one only has "busy"
        event("1", busyHd, busyVenue, List.of(busyCustomer, singleCustomer), busyProfessional);
        event("2", busyHd, busyVenue, List.of(busyCustomer), busyProfessional);
        // the "single" ones appear in just one event; an event with two venues/customers counts once for each
        event("3", singleHd, singleVenue, List.of(singleCustomer), singleProfessional);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void customers_listCarriesTheEventCountIncludingZero() {
        List<CustomerResponseDto> all = customerService.getAllCustomers();

        assertThat(countOf(all, CustomerResponseDto::id, CustomerResponseDto::eventCount, busyCustomer)).isEqualTo(2);
        assertThat(countOf(all, CustomerResponseDto::id, CustomerResponseDto::eventCount, singleCustomer)).isEqualTo(2);
        assertThat(countOf(all, CustomerResponseDto::id, CustomerResponseDto::eventCount, idleCustomer)).isZero();
    }

    @Test
    void customers_getByIdAgreesWithTheList() {
        assertThat(customerService.getCustomerById(busyCustomer).eventCount()).isEqualTo(2);
        assertThat(customerService.getCustomerById(idleCustomer).eventCount()).isZero();
    }

    @Test
    void professionals_listCarriesTheEventCountIncludingZero() {
        List<ProfessionalDto> all = professionalService.getAllProfessionals();

        assertThat(countOf(all, ProfessionalDto::id, ProfessionalDto::eventCount, busyProfessional)).isEqualTo(2);
        assertThat(countOf(all, ProfessionalDto::id, ProfessionalDto::eventCount, singleProfessional)).isEqualTo(1);
        assertThat(countOf(all, ProfessionalDto::id, ProfessionalDto::eventCount, idleProfessional)).isZero();
    }

    @Test
    void professionals_searchAndGetByIdCarryTheCountToo() {
        assertThat(professionalService.getProfessionalById(busyProfessional).eventCount()).isEqualTo(2);
        assertThat(professionalService.searchProfessionals(null, null))
                .filteredOn(p -> p.id().equals(singleProfessional))
                .singleElement().extracting(ProfessionalDto::eventCount).isEqualTo(1);
    }

    @Test
    void venues_listCarriesTheEventCountIncludingZero() {
        List<EventVenueDto> all = eventVenueService.getAllEventVenues();

        assertThat(countOf(all, EventVenueDto::id, EventVenueDto::eventCount, busyVenue)).isEqualTo(2);
        assertThat(countOf(all, EventVenueDto::id, EventVenueDto::eventCount, singleVenue)).isEqualTo(1);
        assertThat(countOf(all, EventVenueDto::id, EventVenueDto::eventCount, idleVenue)).isZero();
        assertThat(eventVenueService.getEventVenueById(busyVenue).eventCount()).isEqualTo(2);
    }

    @Test
    void hds_listCarriesTheEventCountIncludingZero() {
        List<HdDto> all = hdService.getAllHds();

        assertThat(countOf(all, HdDto::id, HdDto::eventCount, busyHd)).isEqualTo(2);
        assertThat(countOf(all, HdDto::id, HdDto::eventCount, singleHd)).isEqualTo(1);
        assertThat(countOf(all, HdDto::id, HdDto::eventCount, idleHd)).isZero();
        assertThat(hdService.getHdById(busyHd).eventCount()).isEqualTo(2);
    }

    @Test
    void createdAndUpdatedResourcesReportTheirCount() {
        assertThat(customerService.updateCustomer(busyCustomer, new CreateCustomerDto(tag + " busy", null, null, null))
                .eventCount()).isEqualTo(2);
        assertThat(hdService.createHd(new CreateHdDto(tag + " new", 1000, 931, 0, null, null, null, HdStatus.ACTIVE))
                .eventCount()).isZero();
    }

    @Test
    void aggregateQueriesRunOneStatementEachRegardlessOfHowManyRowsExist() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        long before = statementsOf(() -> {
            customerRepository.countEventsGroupedByCustomer();
            professionalRepository.countEventsGroupedByProfessional();
            eventVenueRepository.countEventsGroupedByEventVenue();
            hdRepository.countEventsGroupedByHd();
        });

        // more rows on every side: new fixtures, each already attached to events
        for (int i = 0; i < 5; i++) {
            event("x" + i, hd("extra" + i), venue("extra" + i), List.of(customer("extra" + i)), professional("extra" + i));
        }
        entityManager.flush();
        entityManager.clear();

        long after = statementsOf(() -> {
            customerRepository.countEventsGroupedByCustomer();
            professionalRepository.countEventsGroupedByProfessional();
            eventVenueRepository.countEventsGroupedByEventVenue();
            hdRepository.countEventsGroupedByHd();
        });

        assertThat(before).isEqualTo(4);
        assertThat(after).isEqualTo(before);
    }

    private long statementsOf(Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    private <T> int countOf(List<T> all, java.util.function.Function<T, UUID> id,
                            java.util.function.ToIntFunction<T> count, UUID wanted) {
        return all.stream().filter(item -> id.apply(item).equals(wanted)).mapToInt(count).findFirst().orElseThrow();
    }

    private UUID customer(String label) {
        return customerService.createCustomer(new CreateCustomerDto(tag + " " + label, null, null, null)).id();
    }

    private UUID professional(String label) {
        return professionalService.createProfessional(
                new CreateProfessionalDto(tag + " " + label, null, null, null, null)).id();
    }

    private UUID venue(String label) {
        return eventVenueService.createEventVenue(new CreateEventVenueDto(tag + " " + label, null, null, null, null)).id();
    }

    private UUID hd(String label) {
        return hdService.createHd(new CreateHdDto(tag + " " + label, 1000, 931, 0, null, null, null, HdStatus.ACTIVE)).id();
    }

    private void event(String code, UUID hdId, UUID venueId, List<UUID> customerIds, UUID professionalId) {
        eventService.createEvent(new CreateEventDto(
                tag + "-" + code, null, "Evento " + tag + code, null, null, null, null, null, null, null, null, hdId,
                List.of(new EventVenueAssignmentDto(venueId, null)),
                customerIds.stream().map(id -> new EventCustomerAssignmentDto(id, false)).toList(),
                List.of(new EventProfessionalAssignmentDto(professionalId, "Fotógrafo"))));
    }
}
