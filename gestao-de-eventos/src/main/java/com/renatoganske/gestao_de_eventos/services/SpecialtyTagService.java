package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateSpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import com.renatoganske.gestao_de_eventos.exceptions.SpecialtyTagNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.SpecialtyTagRepository;
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
public class SpecialtyTagService {

    private final SpecialtyTagRepository specialtyTagRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public SpecialtyTagDto createSpecialtyTag(CreateSpecialtyTagDto createSpecialtyTagDto) {
        return specialtyTagRepository.save(createSpecialtyTagDto.toEntity()).toResponseDto();
    }

    public List<SpecialtyTagDto> getAllSpecialtyTags() {
        return specialtyTagRepository.findAll().stream()
                .map(SpecialtyTag::toResponseDto)
                .collect(Collectors.toList());
    }

    public SpecialtyTagDto getSpecialtyTagById(UUID id) {
        return specialtyTagRepository.findById(id)
                .map(SpecialtyTag::toResponseDto)
                .orElseThrow(() -> new SpecialtyTagNotFoundException(id));
    }

    @Transactional
    public SpecialtyTagDto updateSpecialtyTag(UUID id, CreateSpecialtyTagDto createSpecialtyTagDto) {
        SpecialtyTag specialtyTag = specialtyTagRepository.findById(id)
                .orElseThrow(() -> new SpecialtyTagNotFoundException(id));

        specialtyTag.setName(createSpecialtyTagDto.name());

        SpecialtyTag updatedSpecialtyTag = specialtyTagRepository.save(specialtyTag);
        return updatedSpecialtyTag.toResponseDto();
    }

    @Transactional
    public void deleteSpecialtyTag(UUID id) {
        SpecialtyTag specialtyTag = specialtyTagRepository.findById(id)
                .orElseThrow(() -> new SpecialtyTagNotFoundException(id));

        long usageCount = professionalRepository.countBySpecialtyTags_Id(id);
        if (usageCount > 0) {
            throw new ResourceInUseException(
                    "Specialty tag is in use by %d professional(s) and cannot be deleted.".formatted(usageCount));
        }

        specialtyTagRepository.delete(specialtyTag);
    }
}
