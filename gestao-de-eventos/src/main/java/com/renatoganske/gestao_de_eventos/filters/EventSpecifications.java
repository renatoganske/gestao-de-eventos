package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventCustomer;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
