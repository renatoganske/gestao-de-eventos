package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * GDE-48 / ADR-0024. Runs against the real schema (Flyway migrations + {@code ddl-auto=validate}), which a
 * mocked repository cannot check: it never proves the {@code has_wedding_fields} column exists, is mapped,
 * and that the V6 migration kept the seeded {@code WEDDING} type working.
 */
@SpringBootTest
@Transactional
@Rollback
class EventTypeServiceIntegrationTest {

    @Autowired
    private EventTypeService eventTypeService;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createEventType_persistsHasWeddingFieldsAndReadsItBack() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        EventTypeDto marked = eventTypeService.createEventType(new CreateEventTypeDto("Mini Wedding " + suffix, true));
        EventTypeDto plain = eventTypeService.createEventType(new CreateEventTypeDto("Ensaio " + suffix, null));
        entityManager.flush();
        entityManager.clear();

        assertThat(eventTypeRepository.findById(marked.id())).get().extracting(EventType::isHasWeddingFields).isEqualTo(true);
        assertThat(eventTypeRepository.findById(plain.id())).get().extracting(EventType::isHasWeddingFields).isEqualTo(false);
    }

    @Test
    void updateEventType_canMarkAnExistingTypeAndRenamingAloneKeepsTheMark() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        EventTypeDto created = eventTypeService.createEventType(new CreateEventTypeDto("Casamento " + suffix, false));

        eventTypeService.updateEventType(created.id(), new CreateEventTypeDto("Casamento " + suffix, true));
        eventTypeService.updateEventType(created.id(), new CreateEventTypeDto("Casamento renomeado " + suffix, null));
        entityManager.flush();
        entityManager.clear();

        EventType reloaded = eventTypeRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Casamento renomeado " + suffix);
        assertThat(reloaded.isHasWeddingFields()).isTrue();
    }

    @Test
    void v6Migration_marksTheSeededWeddingTypeAndLeavesTheOthersUnmarked() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery("select name, has_wedding_fields from tb_event_type where name in ('WEDDING', 'BIRTHDAY', 'PHOTO_SHOOT', 'OTHER')")
                .getResultList();
        // The V2 seed types only exist on a database created from the migrations (CI); a long-lived local
        // database may have had them renamed, so there is nothing to assert about V6 there.
        assumeTrue(!rows.isEmpty(), "seeded event types are not present in this database");

        for (Object[] row : rows) {
            boolean expected = "WEDDING".equals(row[0]);
            assertThat((Boolean) row[1]).as("has_wedding_fields of " + row[0]).isEqualTo(expected);
        }
    }
}
