package com.renatoganske.gestao_de_eventos.repositories;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Row of an aggregate "events per X" query (GDE-58): the id of the customer / professional / venue /
 * HD and how many events it has. Lets the lists get every count in one GROUP BY query instead of
 * loading each entity's event collection.
 */
public interface EventCountProjection {

    UUID getId();

    long getTotal();

    /** Indexes the rows by id; ids absent from the map have no events, so callers default to 0. */
    static Map<UUID, Integer> toMap(List<EventCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                EventCountProjection::getId,
                row -> Math.toIntExact(row.getTotal())));
    }

    static int countOf(Map<UUID, Integer> counts, UUID id) {
        return counts.getOrDefault(id, 0);
    }
}
