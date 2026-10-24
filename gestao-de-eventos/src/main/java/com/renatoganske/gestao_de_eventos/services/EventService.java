package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventListItemDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.PageDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventCustomer;
import com.renatoganske.gestao_de_eventos.entities.EventCustomerId;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenueId;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventProfessionalId;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.InvalidRequestParameterException;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.filters.EventSpecifications;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.filters.EventSort;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventCustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventEventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    /** Large enough for a whole HD's events in one call (the import scripts), small enough to stay cheap. */
    public static final int MAX_PAGE_SIZE = 500;

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final HdRepository hdRepository;
    private final EventVenueRepository eventVenueRepository;
    private final EventTypeRepository eventTypeRepository;
    private final ProfessionalRepository professionalRepository;
    private final EventProfessionalRepository eventProfessionalRepository;
    private final EventCustomerRepository eventCustomerRepository;
    private final EventEventVenueRepository eventEventVenueRepository;

    @Transactional
    public EventDto createEvent(CreateEventDto createEventDto) {
        Event event = createEventDto.toEntity();
        event.setType(resolveEventType(createEventDto.eventTypeId()));
        event.setHd(resolveHd(createEventDto.hdId()));

        Event savedEvent = eventRepository.save(event);
        syncEventVenues(savedEvent, createEventDto.venues());
        syncEventCustomers(savedEvent, createEventDto.customers());
        syncEventProfessionals(savedEvent, createEventDto.professionals());
        adjustHdUsedSpace(savedEvent.getHd(), savedEvent.getSizeGb());

        return savedEvent.toDTO();
    }

    // Read-only transactions keep the session open while toDTO() walks the lazy associations,
    // instead of relying on the implicit open-in-view session (ADR-0027).
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(Event::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(Event::toDTO)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public EventDto updateEvent(UUID id, CreateEventDto createEventDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        Hd previousHd = event.getHd();
        Integer previousSizeGb = event.getSizeGb();

        event.setEventCode(createEventDto.eventCode());
        event.setType(resolveEventType(createEventDto.eventTypeId()));
        event.setName(createEventDto.name());
        event.setEventDate(createEventDto.eventDate());
        event.setDaytimeWedding(createEventDto.daytimeWedding());
        event.setOutdoorWedding(createEventDto.outdoorWedding());
        event.setGuestCount(createEventDto.guestCount());
        event.setDescription(createEventDto.description());
        event.setAmount(createEventDto.amount());
        event.setSizeGb(createEventDto.sizeGb());
        event.setDeliveryStatus(createEventDto.deliveryStatus());
        event.setHd(resolveHd(createEventDto.hdId()));

        Event updatedEvent = eventRepository.save(event);
        syncEventVenues(updatedEvent, createEventDto.venues());
        syncEventCustomers(updatedEvent, createEventDto.customers());
        syncEventProfessionals(updatedEvent, createEventDto.professionals());

        adjustHdUsedSpace(previousHd, previousSizeGb == null ? null : -previousSizeGb);
        adjustHdUsedSpace(updatedEvent.getHd(), updatedEvent.getSizeGb());

        return updatedEvent.toDTO();
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        Hd hd = event.getHd();
        Integer sizeGb = event.getSizeGb();

        // TB_EVENT_PROFESSIONAL references TB_EVENT without ON DELETE CASCADE, and the
        // @OneToMany side is read-only (no cascade/orphanRemoval), so the association rows
        // have to go first or the event delete fails on the foreign key.
        eventProfessionalRepository.deleteByEvent_Id(id);
        eventProfessionalRepository.flush();
        eventCustomerRepository.deleteByEvent_Id(id);
        eventCustomerRepository.flush();
        eventEventVenueRepository.deleteByEvent_Id(id);
        eventEventVenueRepository.flush();

        eventRepository.delete(event);

        adjustHdUsedSpace(hd, sizeGb == null ? null : -sizeGb);
    }

    /**
     * One page of the event search (ADR-0029). Filtered, ordered and paged by the database (ADR-0027),
     * so only the events of the requested page are read; their associations come in batches, so the
     * number of queries does not grow with the page size.
     */
    @Transactional(readOnly = true)
    public PageDto<EventListItemDto> searchEvents(EventSearchCriteria criteria, int page, int size, EventSort sort) {
        if (page < 0) {
            throw new InvalidRequestParameterException("Invalid page %d: pages start at 0.".formatted(page));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestParameterException(
                    "Invalid size %d: use 1 to %d events per page.".formatted(size, MAX_PAGE_SIZE));
        }
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.byType(criteria.eventTypeId()),
                EventSpecifications.byVenue(criteria.venueId()),
                EventSpecifications.byProfessional(criteria.professionalId()),
                EventSpecifications.byPeriod(criteria.from(), criteria.to()),
                EventSpecifications.byHd(criteria.hdId()),
                EventSpecifications.byDeliveryStatus(criteria.deliveryStatus()),
                EventSpecifications.byCustomer(criteria.customerId()),
                EventSpecifications.byCustomerName(criteria.customerName()),
                EventSpecifications.byEventCode(criteria.eventCode()),
                EventSpecifications.byDaytimeWedding(criteria.daytimeWedding()),
                EventSpecifications.byOutdoorWedding(criteria.outdoorWedding()),
                EventSpecifications.byText(criteria.text()),
                // The order lives in the specification, not in the Pageable: some columns are subqueries.
                EventSpecifications.orderedBy(sort));

        return PageDto.of(eventRepository.findAll(spec, PageRequest.of(page, size)), Event::toListItemDto);
    }

    private EventType resolveEventType(UUID eventTypeId) {
        if (eventTypeId == null) {
            return null;
        }
        return eventTypeRepository.findById(eventTypeId)
                .orElseThrow(() -> new EventTypeNotFoundException(eventTypeId));
    }

    private Hd resolveHd(UUID hdId) {
        if (hdId == null) {
            return null;
        }
        return hdRepository.findById(hdId).orElseThrow(() -> new HdNotFoundException(hdId));
    }

    private EventVenue resolveEventVenue(UUID eventVenueId) {
        return eventVenueRepository.findById(eventVenueId)
                .orElseThrow(() -> new EventVenueNotFoundException(eventVenueId));
    }

    /**
     * Replaces the event's whole venue list with the submitted one (ADR-0026), with the same
     * semantics as customers (ADR-0025) and the team (ADR-0021): {@code null} leaves the current
     * venues untouched, an empty list removes them all.
     */
    private void syncEventVenues(Event event, List<EventVenueAssignmentDto> assignments) {
        if (assignments == null) {
            return;
        }

        eventEventVenueRepository.deleteByEvent_Id(event.getId());
        // Same reason as in syncEventProfessionals: the inserts below can reuse the deleted keys.
        eventEventVenueRepository.flush();

        List<EventEventVenue> persisted = eventEventVenueRepository.saveAll(
                dedupeByVenue(assignments).stream()
                        .map(assignment -> toEventEventVenue(event, assignment))
                        .toList());

        event.setEventVenues(persisted);
    }

    private EventEventVenue toEventEventVenue(Event event, EventVenueAssignmentDto assignment) {
        EventVenue venue = resolveEventVenue(assignment.venueId());

        return EventEventVenue.builder()
                // Built explicitly for the same reason as EventProfessionalId (record, no @MapsId fill).
                .id(new EventEventVenueId(event.getId(), venue.getId()))
                .event(event)
                .eventVenue(venue)
                .role(assignment.role())
                .build();
    }

    /**
     * The primary key is (event_id, event_venue_id), so a venue cannot appear twice in one event.
     * Keeping the last entry turns a duplicated payload into defined behaviour instead of a
     * constraint violation surfacing as a 500.
     */
    private Collection<EventVenueAssignmentDto> dedupeByVenue(List<EventVenueAssignmentDto> assignments) {
        Map<UUID, EventVenueAssignmentDto> byVenue = new LinkedHashMap<>();
        assignments.forEach(assignment -> byVenue.put(assignment.venueId(), assignment));

        return byVenue.values();
    }

    private Customer resolveCustomer(UUID customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    /**
     * Replaces the event's whole customer list with the submitted one (ADR-0025), with the same
     * semantics as the team (ADR-0021): {@code null} leaves the current customers untouched, an
     * empty list removes them all.
     */
    private void syncEventCustomers(Event event, List<EventCustomerAssignmentDto> assignments) {
        if (assignments == null) {
            return;
        }

        eventCustomerRepository.deleteByEvent_Id(event.getId());
        // Same reason as in syncEventProfessionals: the inserts below can reuse the deleted keys.
        eventCustomerRepository.flush();

        List<EventCustomer> persisted = eventCustomerRepository.saveAll(
                dedupeByCustomer(assignments).stream()
                        .map(assignment -> toEventCustomer(event, assignment))
                        .toList());

        event.setEventCustomers(persisted);
    }

    private EventCustomer toEventCustomer(Event event, EventCustomerAssignmentDto assignment) {
        Customer customer = resolveCustomer(assignment.customerId());

        return EventCustomer.builder()
                // Built explicitly for the same reason as EventProfessionalId (record, no @MapsId fill).
                .id(new EventCustomerId(event.getId(), customer.getId()))
                .event(event)
                .customer(customer)
                .contracting(assignment.isContracting())
                .build();
    }

    /**
     * The primary key is (event_id, customer_id), so a customer listed twice keeps a single row;
     * if any of the duplicates marks them as contracting, the kept row does too.
     */
    private Collection<EventCustomerAssignmentDto> dedupeByCustomer(List<EventCustomerAssignmentDto> assignments) {
        Map<UUID, EventCustomerAssignmentDto> byCustomer = new LinkedHashMap<>();
        assignments.forEach(assignment -> byCustomer.merge(assignment.customerId(), assignment,
                (kept, duplicate) -> duplicate.isContracting() ? duplicate : kept));

        return byCustomer.values();
    }

    /**
     * Replaces the event's whole team with the submitted one (ADR-0021). A {@code null} list means
     * "leave the current team untouched", so a client that does not know about this field -- the
     * Swagger UI, for instance -- cannot wipe the team by omission; an empty list clears it.
     */
    private void syncEventProfessionals(Event event, List<EventProfessionalAssignmentDto> assignments) {
        if (assignments == null) {
            return;
        }

        eventProfessionalRepository.deleteByEvent_Id(event.getId());
        // The rows being inserted below can reuse the primary keys just deleted, so the delete has
        // to reach the database before the insert instead of being reordered after it at flush time.
        eventProfessionalRepository.flush();

        List<EventProfessional> persisted = eventProfessionalRepository.saveAll(
                dedupeByProfessional(assignments).stream()
                        .map(assignment -> toEventProfessional(event, assignment))
                        .toList());

        event.setEventProfessionals(persisted);
    }

    private EventProfessional toEventProfessional(Event event, EventProfessionalAssignmentDto assignment) {
        Professional professional = professionalRepository.findById(assignment.professionalId())
                .orElseThrow(() -> new ProfessionalNotFoundException(assignment.professionalId()));

        return EventProfessional.builder()
                // EventProfessionalId is a record, so @MapsId cannot populate it -- Hibernate would
                // have to write into immutable fields. The key is built explicitly instead, which is
                // why this runs only after the event has been saved and already has an id.
                .id(new EventProfessionalId(event.getId(), professional.getId()))
                .event(event)
                .professional(professional)
                .roleInEvent(assignment.roleInEvent())
                .build();
    }

    /**
     * The primary key is (event_id, professional_id), so the same professional cannot hold two roles
     * in one event. Keeping the last entry turns a duplicated payload into defined behaviour instead
     * of a constraint violation surfacing as a 500.
     */
    private Collection<EventProfessionalAssignmentDto> dedupeByProfessional(
            List<EventProfessionalAssignmentDto> assignments) {
        Map<UUID, EventProfessionalAssignmentDto> byProfessional = new LinkedHashMap<>();
        assignments.forEach(assignment -> byProfessional.put(assignment.professionalId(), assignment));

        return byProfessional.values();
    }

    private void adjustHdUsedSpace(Hd hd, Integer deltaGb) {
        if (hd == null || deltaGb == null || deltaGb == 0) {
            return;
        }
        int currentUsedSpaceGb = hd.getUsedSpaceGb() == null ? 0 : hd.getUsedSpaceGb();
        hd.setUsedSpaceGb(currentUsedSpaceGb + deltaGb);
        hdRepository.save(hd);
    }
}
