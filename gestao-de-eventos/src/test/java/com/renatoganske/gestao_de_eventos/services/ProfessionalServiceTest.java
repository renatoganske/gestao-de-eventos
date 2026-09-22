package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
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
class ProfessionalServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @InjectMocks
    private ProfessionalService professionalService;

    private Professional professional;
    private CreateProfessionalDto createProfessionalDto;

    @BeforeEach
    void setUp() {
        professional = Professional.builder()
                .id(UUID.randomUUID())
                .name("Carlos Souza")
                .type("Segundo fotógrafo")
                .contact("(11) 97777-0000")
                .specialty("Casamentos")
                .otherInfo("Disponível aos finais de semana")
                .build();

        createProfessionalDto = new CreateProfessionalDto(
                professional.getName(),
                professional.getType(),
                professional.getContact(),
                professional.getSpecialty(),
                professional.getOtherInfo());
    }

    @Test
    void createProfessional_savesAndReturnsResponseDto() {
        ArgumentCaptor<Professional> captor = ArgumentCaptor.forClass(Professional.class);
        when(professionalRepository.save(captor.capture())).thenReturn(professional);

        ProfessionalDto result = professionalService.createProfessional(createProfessionalDto);

        assertThat(captor.getValue().getName()).isEqualTo(createProfessionalDto.name());
        assertThat(captor.getValue().getType()).isEqualTo(createProfessionalDto.type());
        assertThat(captor.getValue().getContact()).isEqualTo(createProfessionalDto.contact());
        assertThat(captor.getValue().getSpecialty()).isEqualTo(createProfessionalDto.specialty());
        assertThat(captor.getValue().getOtherInfo()).isEqualTo(createProfessionalDto.otherInfo());

        assertThat(result.id()).isEqualTo(professional.getId());
        assertThat(result.name()).isEqualTo(professional.getName());
        assertThat(result.type()).isEqualTo(professional.getType());
        assertThat(result.contact()).isEqualTo(professional.getContact());
        assertThat(result.specialty()).isEqualTo(professional.getSpecialty());
        assertThat(result.otherInfo()).isEqualTo(professional.getOtherInfo());
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
    void updateProfessional_updatesAndReturnsProfessionalWhenFound() {
        UUID id = professional.getId();
        CreateProfessionalDto updateDto = new CreateProfessionalDto(
                "Carlos Oliveira", "Drone", "(11) 96666-0000", "Eventos corporativos", "Atualizado");
        when(professionalRepository.findById(id)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalDto result = professionalService.updateProfessional(id, updateDto);

        assertThat(result.name()).isEqualTo("Carlos Oliveira");
        assertThat(result.type()).isEqualTo("Drone");
        assertThat(result.contact()).isEqualTo("(11) 96666-0000");
        assertThat(result.specialty()).isEqualTo("Eventos corporativos");
        assertThat(result.otherInfo()).isEqualTo("Atualizado");
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
}
