package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalFilterTest {

    private final UUID specialtyTagId = UUID.randomUUID();
    private final ProfessionalType photographerType = ProfessionalType.builder().id(UUID.randomUUID()).name("Fotografo").build();
    private final ProfessionalType droneType = ProfessionalType.builder().id(UUID.randomUUID()).name("Drone").build();

    private Professional photographer() {
        SpecialtyTag weddingsTag = SpecialtyTag.builder().id(specialtyTagId).name("Casamentos").build();
        return Professional.builder()
                .id(UUID.randomUUID())
                .name("Joao Fotografo")
                .type(photographerType)
                .specialtyTags(Set.of(weddingsTag))
                .build();
    }

    private Professional other() {
        return Professional.builder()
                .id(UUID.randomUUID())
                .name("Pedro Drone")
                .type(droneType)
                .specialtyTags(Set.of())
                .build();
    }

    @Test
    void byType_matchesOnlyGivenType() {
        assertThat(ProfessionalFilter.byType(photographerType.getId()).test(photographer())).isTrue();
        assertThat(ProfessionalFilter.byType(photographerType.getId()).test(other())).isFalse();
    }

    @Test
    void byType_nullMatchesEverything() {
        assertThat(ProfessionalFilter.byType(null).test(photographer())).isTrue();
        assertThat(ProfessionalFilter.byType(null).test(other())).isTrue();
    }

    @Test
    void bySpecialtyTag_matchesOnlyProfessionalsWithGivenTag() {
        assertThat(ProfessionalFilter.bySpecialtyTag(specialtyTagId).test(photographer())).isTrue();
        assertThat(ProfessionalFilter.bySpecialtyTag(specialtyTagId).test(other())).isFalse();
    }

    @Test
    void bySpecialtyTag_nullMatchesEverything() {
        assertThat(ProfessionalFilter.bySpecialtyTag(null).test(other())).isTrue();
    }

    @Test
    void combinedFilters_matchOnlyWhenAllCriteriaMatch() {
        var filter = ProfessionalFilter.byType(photographerType.getId())
                .and(ProfessionalFilter.bySpecialtyTag(specialtyTagId));

        assertThat(filter.test(photographer())).isTrue();
        assertThat(filter.test(other())).isFalse();
    }
}
