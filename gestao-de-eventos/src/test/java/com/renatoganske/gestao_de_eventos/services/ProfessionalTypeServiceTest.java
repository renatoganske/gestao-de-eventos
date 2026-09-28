package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalTypeServiceTest {

    @Mock
    private ProfessionalTypeRepository professionalTypeRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @InjectMocks
    private ProfessionalTypeService professionalTypeService;

    private ProfessionalType professionalType;
    private CreateProfessionalTypeDto createProfessionalTypeDto;

    @BeforeEach
    void setUp() {
        professionalType = ProfessionalType.builder()
                .id(UUID.randomUUID())
                .name("Decorador")
                .build();

        createProfessionalTypeDto = new CreateProfessionalTypeDto(professionalType.getName());
    }

    @Test
    void createProfessionalType_savesAndReturnsResponseDto() {
        ArgumentCaptor<ProfessionalType> captor = ArgumentCaptor.forClass(ProfessionalType.class);
        when(professionalTypeRepository.save(captor.capture())).thenReturn(professionalType);

        ProfessionalTypeDto result = professionalTypeService.createProfessionalType(createProfessionalTypeDto);

        assertThat(captor.getValue().getName()).isEqualTo(createProfessionalTypeDto.name());

        assertThat(result.id()).isEqualTo(professionalType.getId());
        assertThat(result.name()).isEqualTo(professionalType.getName());
    }

    @Test
    void getAllProfessionalTypes_returnsAllMappedProfessionalTypes() {
        ProfessionalType other = ProfessionalType.builder().id(UUID.randomUUID()).name("Cerimonialista").build();
        when(professionalTypeRepository.findAll()).thenReturn(List.of(professionalType, other));

        List<ProfessionalTypeDto> result = professionalTypeService.getAllProfessionalTypes();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProfessionalTypeDto::name)
                .containsExactly(professionalType.getName(), other.getName());
    }

    @Test
    void getAllProfessionalTypes_returnsEmptyListWhenNoProfessionalTypes() {
        when(professionalTypeRepository.findAll()).thenReturn(List.of());

        List<ProfessionalTypeDto> result = professionalTypeService.getAllProfessionalTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getProfessionalTypeById_returnsProfessionalTypeWhenFound() {
        when(professionalTypeRepository.findById(professionalType.getId())).thenReturn(Optional.of(professionalType));

        ProfessionalTypeDto result = professionalTypeService.getProfessionalTypeById(professionalType.getId());

        assertThat(result.id()).isEqualTo(professionalType.getId());
        assertThat(result.name()).isEqualTo(professionalType.getName());
    }

    @Test
    void getProfessionalTypeById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalTypeService.getProfessionalTypeById(id))
                .isInstanceOf(ProfessionalTypeNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateProfessionalType_updatesAndReturnsProfessionalTypeWhenFound() {
        UUID id = professionalType.getId();
        CreateProfessionalTypeDto updateDto = new CreateProfessionalTypeDto("Cerimonialista");
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.of(professionalType));
        when(professionalTypeRepository.save(any(ProfessionalType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalTypeDto result = professionalTypeService.updateProfessionalType(id, updateDto);

        assertThat(result.name()).isEqualTo("Cerimonialista");
    }

    @Test
    void updateProfessionalType_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalTypeService.updateProfessionalType(id, createProfessionalTypeDto))
                .isInstanceOf(ProfessionalTypeNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(professionalTypeRepository, never()).save(any());
    }

    @Test
    void deleteProfessionalType_deletesWhenFoundAndUnused() {
        UUID id = professionalType.getId();
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.of(professionalType));
        when(professionalRepository.countByType_Id(id)).thenReturn(0L);

        professionalTypeService.deleteProfessionalType(id);

        verify(professionalTypeRepository, times(1)).delete(professionalType);
    }

    @Test
    void deleteProfessionalType_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalTypeService.deleteProfessionalType(id))
                .isInstanceOf(ProfessionalTypeNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(professionalTypeRepository, never()).delete(any());
    }

    @Test
    void deleteProfessionalType_throwsResourceInUseExceptionWhenUsedByProfessionals() {
        UUID id = professionalType.getId();
        when(professionalTypeRepository.findById(id)).thenReturn(Optional.of(professionalType));
        when(professionalRepository.countByType_Id(id)).thenReturn(2L);

        assertThatThrownBy(() -> professionalTypeService.deleteProfessionalType(id))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("2");

        verify(professionalTypeRepository, never()).delete(any());
    }
}
