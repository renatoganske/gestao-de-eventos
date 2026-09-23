package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;

    @Transactional
    public ProfessionalDto createProfessional(CreateProfessionalDto createProfessionalDto) {
        Professional professional = createProfessionalDto.toEntity();

        return professionalRepository.save(professional).toResponseDto();
    }

    public List<ProfessionalDto> getAllProfessionals() {
        return professionalRepository.findAll().stream()
                .map(Professional::toResponseDto)
                .collect(Collectors.toList());
    }

    public ProfessionalDto getProfessionalById(UUID id) {
        Optional<Professional> optionalProfessional = professionalRepository.findById(id);
        return optionalProfessional.map(Professional::toResponseDto)
                .orElseThrow(() -> new ProfessionalNotFoundException(id));
    }

    @Transactional
    public ProfessionalDto updateProfessional(UUID id, CreateProfessionalDto createProfessionalDto) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ProfessionalNotFoundException(id));

        professional.setName(createProfessionalDto.name());
        professional.setType(createProfessionalDto.type());
        professional.setContact(createProfessionalDto.contact());
        professional.setSpecialty(createProfessionalDto.specialty());
        professional.setOtherInfo(createProfessionalDto.otherInfo());

        Professional updatedProfessional = professionalRepository.save(professional);
        return updatedProfessional.toResponseDto();
    }

    @Transactional
    public void deleteProfessional(UUID id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ProfessionalNotFoundException(id));
        professionalRepository.delete(professional);
    }

}
