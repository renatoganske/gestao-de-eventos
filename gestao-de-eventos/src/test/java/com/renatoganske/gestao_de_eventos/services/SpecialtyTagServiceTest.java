package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateSpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import com.renatoganske.gestao_de_eventos.exceptions.SpecialtyTagNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialtyTagServiceTest {

    @Mock
    private SpecialtyTagRepository specialtyTagRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @InjectMocks
    private SpecialtyTagService specialtyTagService;

    private SpecialtyTag specialtyTag;
    private CreateSpecialtyTagDto createSpecialtyTagDto;

    @BeforeEach
    void setUp() {
        specialtyTag = SpecialtyTag.builder()
                .id(UUID.randomUUID())
                .name("Casamentos")
                .build();

        createSpecialtyTagDto = new CreateSpecialtyTagDto(specialtyTag.getName());
    }

    @Test
    void createSpecialtyTag_savesAndReturnsResponseDto() {
        ArgumentCaptor<SpecialtyTag> captor = ArgumentCaptor.forClass(SpecialtyTag.class);
        when(specialtyTagRepository.save(captor.capture())).thenReturn(specialtyTag);

        SpecialtyTagDto result = specialtyTagService.createSpecialtyTag(createSpecialtyTagDto);

        assertThat(captor.getValue().getName()).isEqualTo(createSpecialtyTagDto.name());

        assertThat(result.id()).isEqualTo(specialtyTag.getId());
        assertThat(result.name()).isEqualTo(specialtyTag.getName());
    }

    @Test
    void getAllSpecialtyTags_returnsAllMappedSpecialtyTags() {
        SpecialtyTag other = SpecialtyTag.builder().id(UUID.randomUUID()).name("Eventos corporativos").build();
        when(specialtyTagRepository.findAll()).thenReturn(List.of(specialtyTag, other));

        List<SpecialtyTagDto> result = specialtyTagService.getAllSpecialtyTags();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SpecialtyTagDto::name)
                .containsExactly(specialtyTag.getName(), other.getName());
    }

    @Test
    void getAllSpecialtyTags_returnsEmptyListWhenNoSpecialtyTags() {
        when(specialtyTagRepository.findAll()).thenReturn(List.of());

        List<SpecialtyTagDto> result = specialtyTagService.getAllSpecialtyTags();

        assertThat(result).isEmpty();
    }

    @Test
    void getSpecialtyTagById_returnsSpecialtyTagWhenFound() {
        when(specialtyTagRepository.findById(specialtyTag.getId())).thenReturn(Optional.of(specialtyTag));

        SpecialtyTagDto result = specialtyTagService.getSpecialtyTagById(specialtyTag.getId());

        assertThat(result.id()).isEqualTo(specialtyTag.getId());
        assertThat(result.name()).isEqualTo(specialtyTag.getName());
    }

    @Test
    void getSpecialtyTagById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyTagService.getSpecialtyTagById(id))
                .isInstanceOf(SpecialtyTagNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateSpecialtyTag_updatesAndReturnsSpecialtyTagWhenFound() {
        UUID id = specialtyTag.getId();
        CreateSpecialtyTagDto updateDto = new CreateSpecialtyTagDto("Eventos corporativos");
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.of(specialtyTag));
        when(specialtyTagRepository.save(any(SpecialtyTag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyTagDto result = specialtyTagService.updateSpecialtyTag(id, updateDto);

        assertThat(result.name()).isEqualTo("Eventos corporativos");
    }

    @Test
    void updateSpecialtyTag_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyTagService.updateSpecialtyTag(id, createSpecialtyTagDto))
                .isInstanceOf(SpecialtyTagNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(specialtyTagRepository, never()).save(any());
    }

    @Test
    void deleteSpecialtyTag_deletesWhenFoundAndUnused() {
        UUID id = specialtyTag.getId();
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.of(specialtyTag));
        when(professionalRepository.countBySpecialtyTags_Id(id)).thenReturn(0L);

        specialtyTagService.deleteSpecialtyTag(id);

        verify(specialtyTagRepository, times(1)).delete(specialtyTag);
    }

    @Test
    void deleteSpecialtyTag_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyTagService.deleteSpecialtyTag(id))
                .isInstanceOf(SpecialtyTagNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(specialtyTagRepository, never()).delete(any());
    }

    @Test
    void deleteSpecialtyTag_throwsResourceInUseExceptionWhenUsedByProfessionals() {
        UUID id = specialtyTag.getId();
        when(specialtyTagRepository.findById(id)).thenReturn(Optional.of(specialtyTag));
        when(professionalRepository.countBySpecialtyTags_Id(id)).thenReturn(4L);

        assertThatThrownBy(() -> specialtyTagService.deleteSpecialtyTag(id))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("4");

        verify(specialtyTagRepository, never()).delete(any());
    }
}
