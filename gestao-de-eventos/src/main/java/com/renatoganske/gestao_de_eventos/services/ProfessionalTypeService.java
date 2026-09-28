package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfessionalTypeService {

    private final ProfessionalTypeRepository professionalTypeRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public ProfessionalTypeDto createProfessionalType(CreateProfessionalTypeDto createProfessionalTypeDto) {
        return professionalTypeRepository.save(createProfessionalTypeDto.toEntity()).toResponseDto();
    }

    public List<ProfessionalTypeDto> getAllProfessionalTypes() {
        return professionalTypeRepository.findAll().stream()
                .map(ProfessionalType::toResponseDto)
                .collect(Collectors.toList());
    }

    public ProfessionalTypeDto getProfessionalTypeById(UUID id) {
        return professionalTypeRepository.findById(id)
                .map(ProfessionalType::toResponseDto)
                .orElseThrow(() -> new ProfessionalTypeNotFoundException(id));
    }

    @Transactional
    public ProfessionalTypeDto updateProfessionalType(UUID id, CreateProfessionalTypeDto createProfessionalTypeDto) {
        ProfessionalType professionalType = professionalTypeRepository.findById(id)
                .orElseThrow(() -> new ProfessionalTypeNotFoundException(id));

        professionalType.setName(createProfessionalTypeDto.name());

        ProfessionalType updatedProfessionalType = professionalTypeRepository.save(professionalType);
        return updatedProfessionalType.toResponseDto();
    }

    @Transactional
    public void deleteProfessionalType(UUID id) {
        ProfessionalType professionalType = professionalTypeRepository.findById(id)
                .orElseThrow(() -> new ProfessionalTypeNotFoundException(id));

        long usageCount = professionalRepository.countByType_Id(id);
        if (usageCount > 0) {
            throw new ResourceInUseException(
                    "Professional type is in use by %d professional(s) and cannot be deleted.".formatted(usageCount));
        }

        professionalTypeRepository.delete(professionalType);
    }
}
