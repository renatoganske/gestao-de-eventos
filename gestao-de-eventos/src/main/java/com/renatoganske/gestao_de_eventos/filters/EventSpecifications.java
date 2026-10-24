package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventCustomer;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Composable criteria for the event search, run by the database (ADR-0027, revising ADR-0005).
 * Each factory returns {@code null} when its criterion is empty, which Spring Data treats as
 * "no restriction" when composing, so an absent search parameter still means "do not filter by it".
 */
public final class EventSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private EventSpecifications() {
    }

    public static Specification<Event> byType(UUID eventTypeId) {
        if (eventTypeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type").get("id"), eventTypeId);
    }

    /** Matches when any of the event's venues is the given one, not only the first. */
    public static Specification<Event> byVenue(UUID venueId) {
        if (venueId == null) {
            return null;
        }
        return hasLink(EventEventVenue.class,
                (link, cb) -> cb.equal(link.get("eventVenue").get("id"), venueId));
    }

    public static Specification<Event> byProfessional(UUID professionalId) {
        if (professionalId == null) {
            return null;
        }
        return hasLink(EventProfessional.class,
                (link, cb) -> cb.equal(link.get("professional").get("id"), professionalId));
    }

    /** With any bound set, events without a date never match: there is nothing to compare. */
    public static Specification<Event> byPeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<LocalDate> eventDate = root.get("eventDate");
            Predicate afterFrom = from == null ? null : cb.greaterThanOrEqualTo(eventDate, from);
            Predicate beforeTo = to == null ? null : cb.lessThanOrEqualTo(eventDate, to);
            return cb.and(Stream.of(afterFrom, beforeTo).filter(Objects::nonNull).toArray(Predicate[]::new));
        };
    }

    public static Specification<Event> byHd(UUID hdId) {
        if (hdId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("hd").get("id"), hdId);
    }

    public static Specification<Event> byDeliveryStatus(DeliveryStatus deliveryStatus) {
        if (deliveryStatus == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("deliveryStatus"), deliveryStatus);
    }

    /** Matches when any of the event's customers is the given one, contracting or not. */
    public static Specification<Event> byCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return hasLink(EventCustomer.class,
                (link, cb) -> cb.equal(link.get("customer").get("id"), customerId));
    }

    /** Partial, case-insensitive match on the name of any of the event's customers. */
    public static Specification<Event> byCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return null;
        }
        return hasLink(EventCustomer.class,
                (link, cb) -> containsIgnoringCase(cb, link.join("customer").get("name"), customerName));
    }

    public static Specification<Event> byEventCode(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> containsIgnoringCase(cb, root.get("eventCode"), eventCode);
    }

    /**
     * The list's search box (ADR-0029): matches the event code OR the name of any of the event's
     * customers, with the same partial, case-insensitive, literal-wildcard rules as each of them.
     * One query instead of the two the page used to send and merge.
     */
    public static Specification<Event> byText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Specification.anyOf(byEventCode(text), byCustomerName(text));
    }

    // The wedding flags are null for events that are not weddings ("not applicable"), so an active
    // filter only matches an explicit true/false -- which SQL equality already does for a null column.
    public static Specification<Event> byDaytimeWedding(Boolean daytimeWedding) {
        if (daytimeWedding == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("daytimeWedding"), daytimeWedding);
    }

    public static Specification<Event> byOutdoorWedding(Boolean outdoorWedding) {
        if (outdoorWedding == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("outdoorWedding"), outdoorWedding);
    }

    /**
     * Orders the search by a list column (ADR-0029), then by code and id so that pages are stable when
     * the column ties. Empty values (no date, no type, no customer...) always go last, whatever the
     * direction. It restricts nothing: it only sets the ORDER BY of the select of events, and leaves the
     * page's count query alone (Spring Data runs the same specification for both).
     */
    public static Specification<Event> orderedBy(EventSort sort) {
        return (root, query, cb) -> {
            if (Event.class.equals(query.getResultType())) {
                Expression<?> column = switch (sort.key()) {
                    case CODE -> cb.lower(root.get("eventCode"));
                    case TYPE -> cb.lower(root.join("type", JoinType.LEFT).get("name"));
                    case DATE -> root.get("eventDate");
                    case HD -> cb.lower(root.join("hd", JoinType.LEFT).get("name"));
                    // The enum is stored by name, and ARCHIVED < DELIVERED < PENDING is also the order of
                    // the labels the list shows (Arquivado < Entregue < Pendente).
                    case STATUS -> root.get("deliveryStatus");
                    case CUSTOMER -> cb.coalesce(
                            firstCustomerName(root, query, cb, true), firstCustomerName(root, query, cb, false));
                    case VENUES -> firstVenueName(root, query, cb);
                };
                query.orderBy(List.of(
                        cb.asc(cb.selectCase().when(cb.isNull(column), 1).otherwise(0)),
                        sort.ascending() ? cb.asc(column) : cb.desc(column),
                        cb.asc(root.get("eventCode")),
                        cb.asc(root.get("id"))));
            }
            return null;
        };
    }

    /**
     * Name the "Clientes" column starts with: the contracting customer's when {@code onlyContracting},
     * otherwise the first by name. A scalar subquery, so sorting never repeats an event per customer.
     */
    private static Expression<String> firstCustomerName(
            Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            boolean onlyContracting) {
        Subquery<String> subquery = query.subquery(String.class);
        Root<EventCustomer> link = subquery.from(EventCustomer.class);
        Expression<String> name = cb.lower(link.get("customer").get("name"));
        Predicate ofEvent = cb.equal(link.get("event"), root);
        subquery.select(cb.least(name))
                .where(onlyContracting ? cb.and(ofEvent, cb.isTrue(link.get("contracting"))) : ofEvent);
        return subquery;
    }

    /** The "Locais" column lists the venues by name, so it sorts by the first of them. */
    private static Expression<String> firstVenueName(
            Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<String> subquery = query.subquery(String.class);
        Root<EventEventVenue> link = subquery.from(EventEventVenue.class);
        subquery.select(cb.least(cb.lower(link.get("eventVenue").get("name"))))
                .where(cb.equal(link.get("event"), root));
        return subquery;
    }

    /**
     * {@code EXISTS (select 1 from <link> l where l.event = e and <matches>)}. A join would return
     * the event once per matching row (two customers named "Silva" = the event twice) and need a
     * DISTINCT; EXISTS keeps one row per event and composes with the other collection filters.
     */
    private static <L> Specification<Event> hasLink(
            Class<L> linkType, BiFunction<Root<L>, CriteriaBuilder, Predicate> matches) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<L> link = subquery.from(linkType);
            subquery.select(cb.literal(1))
                    .where(cb.equal(link.get("event"), root), matches.apply(link, cb));
            return cb.exists(subquery);
        };
    }

    /** {@code %} and {@code _} typed by the user are matched literally, as the in-memory filter did. */
    private static Predicate containsIgnoringCase(CriteriaBuilder cb, Expression<String> value, String fragment) {
        String escaped = fragment.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(cb.lower(value), "%" + escaped + "%", LIKE_ESCAPE);
    }
}
