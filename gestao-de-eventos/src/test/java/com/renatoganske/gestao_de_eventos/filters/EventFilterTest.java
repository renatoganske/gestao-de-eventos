package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class EventFilterTest {

    private final UUID venueId = UUID.randomUUID();
    private final UUID hdId = UUID.randomUUID();
    private final UUID professionalId = UUID.randomUUID();
    private final EventType weddingType = EventType.builder().id(UUID.randomUUID()).name("WEDDING").build();
    private final EventType birthdayType = EventType.builder().id(UUID.randomUUID()).name("BIRTHDAY").build();

    private Event weddingEvent() {
        Professional professional = Professional.builder().id(professionalId).name("Joao Fotografo").build();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-001")
                .type(weddingType)
                .eventDate(LocalDate.of(2026, 6, 15))
                .eventVenue(EventVenue.builder().id(venueId).build())
                .hd(Hd.builder().id(hdId).build())
                .deliveryStatus(DeliveryStatus.PENDING)
                .customer(Customer.builder().name("Maria Silva").build())
                .build();
        EventProfessional eventProfessional = EventProfessional.builder()
                .event(event)
                .professional(professional)
                .build();
        event.setEventProfessionals(List.of(eventProfessional));
        return event;
    }

    private Event otherEvent() {
        return Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-002")
                .type(birthdayType)
                .eventDate(LocalDate.of(2026, 1, 1))
                .eventVenue(EventVenue.builder().id(UUID.randomUUID()).build())
                .hd(Hd.builder().id(UUID.randomUUID()).build())
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .customer(Customer.builder().name("Ana Souza").build())
                .eventProfessionals(List.of())
                .build();
    }

    @Test
    void byType_matchesOnlyGivenType() {
        assertThat(EventFilter.byType(weddingType.getId()).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byType(weddingType.getId()).test(otherEvent())).isFalse();
    }

    @Test
    void byType_nullMatchesEverything() {
        assertThat(EventFilter.byType(null).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byType(null).test(otherEvent())).isTrue();
    }

    @Test
    void byVenue_matchesOnlyGivenVenue() {
        assertThat(EventFilter.byVenue(venueId).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byVenue(venueId).test(otherEvent())).isFalse();
    }

    @Test
    void byVenue_nullMatchesEverything() {
        assertThat(EventFilter.byVenue(null).test(weddingEvent())).isTrue();
    }

    @Test
    void byProfessional_matchesOnlyEventsWithGivenProfessional() {
        assertThat(EventFilter.byProfessional(professionalId).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byProfessional(professionalId).test(otherEvent())).isFalse();
    }

    @Test
    void byProfessional_nullMatchesEverything() {
        assertThat(EventFilter.byProfessional(null).test(otherEvent())).isTrue();
    }

    @Test
    void byPeriod_matchesWithinRange() {
        EventFilter filter = EventFilter.byPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(filter.test(weddingEvent())).isTrue();
        assertThat(filter.test(otherEvent())).isFalse();
    }

    @Test
    void byPeriod_bothNullMatchesEverything() {
        assertThat(EventFilter.byPeriod(null, null).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byPeriod(null, null).test(otherEvent())).isTrue();
    }

    @Test
    void byPeriod_onlyFromBoundsLowerLimit() {
        EventFilter filter = EventFilter.byPeriod(LocalDate.of(2026, 3, 1), null);

        assertThat(filter.test(weddingEvent())).isTrue();
        assertThat(filter.test(otherEvent())).isFalse();
    }

    @Test
    void byPeriod_onlyToBoundsUpperLimit() {
        EventFilter filter = EventFilter.byPeriod(null, LocalDate.of(2026, 3, 1));

        assertThat(filter.test(weddingEvent())).isFalse();
        assertThat(filter.test(otherEvent())).isTrue();
    }

    @Test
    void byHd_matchesOnlyGivenHd() {
        assertThat(EventFilter.byHd(hdId).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byHd(hdId).test(otherEvent())).isFalse();
    }

    @Test
    void byHd_nullMatchesEverything() {
        assertThat(EventFilter.byHd(null).test(otherEvent())).isTrue();
    }

    @Test
    void byDeliveryStatus_matchesOnlyGivenStatus() {
        assertThat(EventFilter.byDeliveryStatus(DeliveryStatus.PENDING).test(weddingEvent())).isTrue();
        assertThat(EventFilter.byDeliveryStatus(DeliveryStatus.PENDING).test(otherEvent())).isFalse();
    }

    @Test
    void byDeliveryStatus_nullMatchesEverything() {
        assertThat(EventFilter.byDeliveryStatus(null).test(otherEvent())).isTrue();
    }

    @Test
    void byCustomerName_matchesPartialCaseInsensitive() {
        assertThat(EventFilter.byCustomerName("maria").test(weddingEvent())).isTrue();
        assertThat(EventFilter.byCustomerName("SILVA").test(weddingEvent())).isTrue();
        assertThat(EventFilter.byCustomerName("maria").test(otherEvent())).isFalse();
    }

    @Test
    void byCustomerName_nullOrBlankMatchesEverything() {
        assertThat(EventFilter.byCustomerName(null).test(otherEvent())).isTrue();
        assertThat(EventFilter.byCustomerName("  ").test(otherEvent())).isTrue();
    }

    @Test
    void byCustomerName_eventWithoutCustomerNeverMatchesNonBlankName() {
        Event eventWithoutCustomer = Event.builder().id(UUID.randomUUID()).build();
        assertThat(EventFilter.byCustomerName("maria").test(eventWithoutCustomer)).isFalse();
    }

    @Test
    void byEventCode_matchesPartialCaseInsensitive() {
        assertThat(EventFilter.byEventCode("evt-001").test(weddingEvent())).isTrue();
        assertThat(EventFilter.byEventCode("001").test(weddingEvent())).isTrue();
        assertThat(EventFilter.byEventCode("evt-001").test(otherEvent())).isFalse();
    }

    @Test
    void byEventCode_nullOrBlankMatchesEverything() {
        assertThat(EventFilter.byEventCode(null).test(otherEvent())).isTrue();
        assertThat(EventFilter.byEventCode("  ").test(otherEvent())).isTrue();
    }

    private Event weddingWithFlags(Boolean daytime, Boolean outdoor) {
        return Event.builder()
                .id(UUID.randomUUID())
                .type(weddingType)
                .daytimeWedding(daytime)
                .outdoorWedding(outdoor)
                .build();
    }

    @Test
    void byOutdoorWedding_trueMatchesOnlyExplicitTrue() {
        EventFilter filter = EventFilter.byOutdoorWedding(true);

        assertThat(filter.test(weddingWithFlags(null, true))).isTrue();
        assertThat(filter.test(weddingWithFlags(null, false))).isFalse();
    }

    @Test
    void byOutdoorWedding_falseMatchesOnlyExplicitFalse() {
        EventFilter filter = EventFilter.byOutdoorWedding(false);

        assertThat(filter.test(weddingWithFlags(null, false))).isTrue();
        assertThat(filter.test(weddingWithFlags(null, true))).isFalse();
    }

    @Test
    void byOutdoorWedding_activeFilterExcludesEventsWithNullFlag() {
        assertThat(EventFilter.byOutdoorWedding(true).test(otherEvent())).isFalse();
        assertThat(EventFilter.byOutdoorWedding(false).test(otherEvent())).isFalse();
    }

    @Test
    void byOutdoorWedding_nullMatchesEverything() {
        assertThat(EventFilter.byOutdoorWedding(null).test(weddingWithFlags(null, true))).isTrue();
        assertThat(EventFilter.byOutdoorWedding(null).test(weddingWithFlags(null, false))).isTrue();
        assertThat(EventFilter.byOutdoorWedding(null).test(otherEvent())).isTrue();
    }

    @Test
    void byDaytimeWedding_trueMatchesOnlyExplicitTrue() {
        EventFilter filter = EventFilter.byDaytimeWedding(true);

        assertThat(filter.test(weddingWithFlags(true, null))).isTrue();
        assertThat(filter.test(weddingWithFlags(false, null))).isFalse();
    }

    @Test
    void byDaytimeWedding_falseMatchesOnlyExplicitFalse() {
        EventFilter filter = EventFilter.byDaytimeWedding(false);

        assertThat(filter.test(weddingWithFlags(false, null))).isTrue();
        assertThat(filter.test(weddingWithFlags(true, null))).isFalse();
    }

    @Test
    void byDaytimeWedding_activeFilterExcludesEventsWithNullFlag() {
        assertThat(EventFilter.byDaytimeWedding(true).test(otherEvent())).isFalse();
        assertThat(EventFilter.byDaytimeWedding(false).test(otherEvent())).isFalse();
    }

    @Test
    void byDaytimeWedding_nullMatchesEverything() {
        assertThat(EventFilter.byDaytimeWedding(null).test(weddingWithFlags(true, null))).isTrue();
        assertThat(EventFilter.byDaytimeWedding(null).test(weddingWithFlags(false, null))).isTrue();
        assertThat(EventFilter.byDaytimeWedding(null).test(otherEvent())).isTrue();
    }

    @Test
    void weddingFlagFilters_combineIndependently() {
        Predicate<Event> outdoorAndDaytime = EventFilter.byOutdoorWedding(true)
                .and(EventFilter.byDaytimeWedding(true));

        assertThat(outdoorAndDaytime.test(weddingWithFlags(true, true))).isTrue();
        assertThat(outdoorAndDaytime.test(weddingWithFlags(true, false))).isFalse();
        assertThat(outdoorAndDaytime.test(weddingWithFlags(false, true))).isFalse();
    }

    @Test
    void combinedFilters_matchOnlyWhenAllCriteriaMatch() {
        Predicate<Event> filter = EventFilter.byType(weddingType.getId())
                .and(EventFilter.byVenue(venueId))
                .and(EventFilter.byPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));

        assertThat(filter.test(weddingEvent())).isTrue();
        assertThat(filter.test(otherEvent())).isFalse();
    }

    @Test
    void combinedFilters_allNullCriteriaMatchEverything() {
        Predicate<Event> filter = EventFilter.byType(null)
                .and(EventFilter.byVenue(null))
                .and(EventFilter.byProfessional(null))
                .and(EventFilter.byPeriod(null, null))
                .and(EventFilter.byHd(null))
                .and(EventFilter.byDeliveryStatus(null))
                .and(EventFilter.byCustomerName(null))
                .and(EventFilter.byEventCode(null))
                .and(EventFilter.byDaytimeWedding(null))
                .and(EventFilter.byOutdoorWedding(null));

        assertThat(filter.test(weddingEvent())).isTrue();
        assertThat(filter.test(otherEvent())).isTrue();
    }
}
