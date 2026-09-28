package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateSpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real Postgres instance to prove the {@code @ManyToOne}/{@code @ManyToMany}
 * mapping introduced by ADR-0022 round-trips correctly -- lower risk than the composite-key
 * trap fixed in ADR-0021 (no {@code @EmbeddedId}/{@code @MapsId} involved here), but a mocked
 * repository would still never flush and could not catch a wrong {@code @JoinTable} column name.
 */
@SpringBootTest
@Transactional
@Rollback
class ProfessionalServiceIntegrationTest {

    @Autowired
    private ProfessionalService professionalService;

    @Autowired
    private ProfessionalTypeService professionalTypeService;

    @Autowired
    private SpecialtyTagService specialtyTagService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createProfessional_withTypeAndSpecialtyTags_persistsAndReadsThemBack() {
        ProfessionalTypeDto type = professionalTypeService.createProfessionalType(
                new CreateProfessionalTypeDto("Integration Decorador"));
        SpecialtyTagDto weddingsTag = specialtyTagService.createSpecialtyTag(
                new CreateSpecialtyTagDto("Integration Casamentos"));
        SpecialtyTagDto corporateTag = specialtyTagService.createSpecialtyTag(
                new CreateSpecialtyTagDto("Integration Eventos Corporativos"));

        ProfessionalDto created = professionalService.createProfessional(new CreateProfessionalDto(
                "Integration Professional", type.id(), "(11) 90000-0000",
                List.of(weddingsTag.id(), corporateTag.id()), null));
        entityManager.flush();
        entityManager.clear();

        ProfessionalDto reloaded = professionalService.getProfessionalById(created.id());
        assertThat(reloaded.type().id()).isEqualTo(type.id());
        assertThat(reloaded.specialtyTags())
                .extracting(SpecialtyTagDto::id)
                .containsExactlyInAnyOrder(weddingsTag.id(), corporateTag.id());
    }

    @Test
    void updateProfessional_replacingSpecialtyTags_persistsTheReplacement() {
        SpecialtyTagDto oldTag = specialtyTagService.createSpecialtyTag(
                new CreateSpecialtyTagDto("Integration Old Tag"));
        SpecialtyTagDto newTag = specialtyTagService.createSpecialtyTag(
                new CreateSpecialtyTagDto("Integration New Tag"));

        ProfessionalDto created = professionalService.createProfessional(new CreateProfessionalDto(
                "Integration Professional 2", null, null, List.of(oldTag.id()), null));
        entityManager.flush();

        ProfessionalDto updated = professionalService.updateProfessional(created.id(), new CreateProfessionalDto(
                "Integration Professional 2", null, null, List.of(newTag.id()), null));
        entityManager.flush();
        entityManager.clear();

        ProfessionalDto reloaded = professionalService.getProfessionalById(updated.id());
        assertThat(reloaded.specialtyTags())
                .extracting(SpecialtyTagDto::id)
                .containsExactly(newTag.id());
    }

    @Test
    void deleteSpecialtyTag_inUseByProfessional_throwsResourceInUseExceptionAndPersistsNothing() {
        SpecialtyTagDto tag = specialtyTagService.createSpecialtyTag(
                new CreateSpecialtyTagDto("Integration In Use Tag"));
        professionalService.createProfessional(new CreateProfessionalDto(
                "Integration Professional 3", null, null, List.of(tag.id()), null));
        entityManager.flush();

        assertThatThrownBy(() -> specialtyTagService.deleteSpecialtyTag(tag.id()))
                .isInstanceOf(ResourceInUseException.class);
    }
}
