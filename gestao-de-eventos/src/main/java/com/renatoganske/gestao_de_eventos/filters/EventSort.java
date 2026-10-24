package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.exceptions.InvalidRequestParameterException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Order of the paged event search (ADR-0029). The client names a column of the list, never an entity
 * property path: the key is translated to a JPA expression by {@link EventSpecifications#orderedBy}, so
 * columns derived from collections (customers, venues) can be sorted and nothing else is exposed.
 */
public record EventSort(Key key, boolean ascending) {

    /** Newest events first (GDE-62). */
    public static final EventSort DEFAULT = new EventSort(Key.DATE, false);

    public enum Key {
        CODE, CUSTOMER, VENUES, TYPE, DATE, HD, STATUS;

        String paramValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Parses {@code "<key>,<asc|desc>"}; the direction is optional (ascending). Blank means the default. */
    public static EventSort parse(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String[] parts = value.split(",", -1);
        if (parts.length > 2) {
            throw invalid(value);
        }
        Key key = Arrays.stream(Key.values())
                .filter(candidate -> candidate.paramValue().equals(parts[0].trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> invalid(value));
        String direction = parts.length == 2 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        return switch (direction) {
            case "asc" -> new EventSort(key, true);
            case "desc" -> new EventSort(key, false);
            default -> throw invalid(value);
        };
    }

    private static InvalidRequestParameterException invalid(String value) {
        String keys = Arrays.stream(Key.values()).map(Key::paramValue).collect(Collectors.joining(", "));
        return new InvalidRequestParameterException(
                "Invalid sort '%s': use <key>,<asc|desc> with key one of %s.".formatted(value, keys));
    }
}
