package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.SpecialtyTagNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalTypeRepository;
import com.renatoganske.gestao_de_eventos.repositories.SpecialtyTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private ProfessionalTypeRepository professionalTypeRepository;

    @Mock
    private SpecialtyTagRepository specialtyTagRepository;

    @InjectMocks
    private ProfessionalService professionalService;

    private ProfessionalType secondPhotographerType;
    private ProfessionalType droneType;
    private SpecialtyTag weddingsTag;
    private SpecialtyTag corporateTag;
    private Professional professional;
    private CreateProfessionalDto createProfessionalDto;

    @BeforeEach
    void setUp() {
        secondPhotographerType = ProfessionalType.builder().id(UUID.randomUUID()).name("Segundo fotógrafo").build();
        droneType = ProfessionalType.builder().id(UUID.randomUUID()).name("Drone").build();
        weddingsTag = SpecialtyTag.builder().id(UUID.randomUUID()).name("Casamentos").build();
        corporateTag = SpecialtyTag.builder().id(UUID.randomUUID()).name("Eventos corporativos").build();
        lenient().when(professionalTypeRepository.findById(secondPhotographerType.getId())).thenReturn(Optional.of(secondPhotographerType));
        lenient().when(professionalTypeRepository.findById(droneType.getId())).thenReturn(Optional.of(droneType));
        lenient().when(specialtyTagRepository.findById(weddingsTag.getId())).thenReturn(Optional.of(weddingsTag));
        lenient().when(specialtyTagRepository.findById(corporateTag.getId())).thenReturn(Optional.of(corporateTag));

        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Carlos Souza")
                .type(secondPhotographerType)
                .contact("(11) 97777-0000")
                .specialtyTags(Set.of(weddingsTag))
                .otherInfo("Disponível aos finais de semana")
                .build();

        createProfessionalDto = new CreateProfessionalDto(
                professional.getName(),
                secondPhotographerType.getId(),
                professional.getContact(),
                List.of(weddingsTag.getId()),
                professional.getOtherInfo());
    }

    @Test
    void createProfessional_resolvesTypeAndTagsSavesAndReturnsResponseDto() {
        ArgumentCaptor<Professional> captor = ArgumentCaptor.forClass(Professional.class);
        when(professionalRepository.save(captor.capture())).thenReturn(professional);

        ProfessionalDto result = professionalService.createProfessional(createProfessionalDto);

        assertThat(captor.getValue().getName()).isEqualTo(createProfessionalDto.name());
        assertThat(captor.getValue().getType()).isEqualTo(secondPhotographerType);
        assertThat(captor.getValue().getContact()).isEqualTo(createProfessionalDto.contact());
        assertThat(captor.getValue().getSpecialtyTags()).containsExactly(weddingsTag);
        assertThat(captor.getValue().getOtherInfo()).isEqualTo(createProfessionalDto.otherInfo());

        assertThat(result.id()).isEqualTo(professional.getId());
        assertThat(result.name()).isEqualTo(professional.getName());
        assertThat(result.type().id()).isEqualTo(secondPhotographerType.getId());
        assertThat(result.contact()).isEqualTo(professional.getContact());
        assertThat(result.specialtyTags()).extracting(tag -> tag.id()).containsExactly(weddingsTag.getId());
        assertThat(result.otherInfo()).isEqualTo(professional.getOtherInfo());
    }

    @Test
    void createProfessional_withoutTypeOrTags_leavesThemEmpty() {
        CreateProfessionalDto dto = new CreateProfessionalDto("Ana Lima", null, null, null, null);
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalDto result = professionalService.createProfessional(dto);

        assertThat(result.type()).isNull();
        assertThat(result.specialtyTags()).isEmpty();
    }

    @Test
    void createProfessional_withUnknownTypeId_throwsProfessionalTypeNotFoundException() {
        UUID missingTypeId = UUID.randomUUID();
        CreateProfessionalDto dto = new CreateProfessionalDto("Ana Lima", missingTypeId, null, null, null);
        when(professionalTypeRepository.findById(missingTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.createProfessional(dto))
                .isInstanceOf(ProfessionalTypeNotFoundException.class)
                .hasMessageContaining(missingTypeId.toString());

        verify(professionalRepository, never()).save(any());
    }

    @Test
    void createProfessional_withUnknownSpecialtyTagId_throwsSpecialtyTagNotFoundException() {
        UUID missingTagId = UUID.randomUUID();
        CreateProfessionalDto dto = new CreateProfessionalDto("Ana Lima", null, null, List.of(missingTagId), null);
        when(specialtyTagRepository.findById(missingTagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.createProfessional(dto))
                .isInstanceOf(SpecialtyTagNotFoundException.class)
                .hasMessageContaining(missingTagId.toString());

        verify(professionalRepository, never()).save(any());
    }

    @Test
    void getAllProfessionals_returnsAllMappedProfessionals() {
        Professional other = Professional.builder()
                .id(UUID.randomUUID())
                .name("Ana Lima")
                .build();
        when(professionalRepository.findAll()).thenReturn(List.of(professional, other));

        List<ProfessionalDto> result = professionalService.getAllProfessionals();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProfessionalDto::name)
                .containsExactly(professional.getName(), other.getName());
    }

    @Test
    void getAllProfessionals_returnsEmptyListWhenNoProfessionals() {
        when(professionalRepository.findAll()).thenReturn(List.of());

        List<ProfessionalDto> result = professionalService.getAllProfessionals();

        assertThat(result).isEmpty();
    }

    @Test
    void getProfessionalById_returnsProfessionalWhenFound() {
        when(professionalRepository.findById(professional.getId())).thenReturn(Optional.of(professional));

        ProfessionalDto result = professionalService.getProfessionalById(professional.getId());

        assertThat(result.id()).isEqualTo(professional.getId());
        assertThat(result.name()).isEqualTo(professional.getName());
    }

    @Test
    void getProfessionalById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.getProfessionalById(id))
                .isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateProfessional_updatesTypeAndTagsAndReturnsProfessionalWhenFound() {
        UUID id = professional.getId();
        CreateProfessionalDto updateDto = new CreateProfessionalDto(
                "Carlos Oliveira", droneType.getId(), "(11) 96666-0000", List.of(corporateTag.getId()), "Atualizado");
        when(professionalRepository.findById(id)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalDto result = professionalService.updateProfessional(id, updateDto);

        assertThat(result.name()).isEqualTo("Carlos Oliveira");
        assertThat(result.type().id()).isEqualTo(droneType.getId());
        assertThat(result.contact()).isEqualTo("(11) 96666-0000");
        assertThat(result.specialtyTags()).extracting(tag -> tag.id()).containsExactly(corporateTag.getId());
        assertThat(result.otherInfo()).isEqualTo("Atualizado");
    }

    @Test
    void updateProfessional_withEmptyTagsList_clearsSpecialtyTags() {
        UUID id = professional.getId();
        CreateProfessionalDto updateDto = new CreateProfessionalDto(
                professional.getName(), null, professional.getContact(), List.of(), professional.getOtherInfo());
        when(professionalRepository.findById(id)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalDto result = professionalService.updateProfessional(id, updateDto);

        assertThat(result.specialtyTags()).isEmpty();
    }

    @Test
    void updateProfessional_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.updateProfessional(id, createProfessionalDto))
                .isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(professionalRepository, never()).save(any());
    }

    @Test
    void deleteProfessional_deletesWhenFound() {
        UUID id = professional.getId();
        when(professionalRepository.findById(id)).thenReturn(Optional.of(professional));

        professionalService.deleteProfessional(id);

        verify(professionalRepository, times(1)).delete(professional);
    }

    @Test
    void deleteProfessional_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.deleteProfessional(id))
                .isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(professionalRepository, never()).delete(any());
    }

    @Test
    void searchProfessionals_appliesCombinedFiltersOverAllProfessionals() {
        Professional other = Professional.builder()
                .id(UUID.randomUUID())
                .name("Ana Lima")
                .type(droneType)
                .specialtyTags(Set.of(corporateTag))
                .build();
        when(professionalRepository.findAll()).thenReturn(List.of(professional, other));

        List<ProfessionalDto> result = professionalService.searchProfessionals(secondPhotographerType.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(professional.getId());
    }

    @Test
    void searchProfessionals_noFiltersReturnsAllProfessionals() {
        Professional other = Professional.builder().id(UUID.randomUUID()).name("Ana Lima").build();
        when(professionalRepository.findAll()).thenReturn(List.of(professional, other));

        List<ProfessionalDto> result = professionalService.searchProfessionals(null, null);

        assertThat(result).hasSize(2);
    }
}
