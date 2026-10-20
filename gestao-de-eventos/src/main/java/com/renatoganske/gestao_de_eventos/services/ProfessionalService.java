package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.SpecialtyTagNotFoundException;
import com.renatoganske.gestao_de_eventos.filters.ProfessionalFilter;
import com.renatoganske.gestao_de_eventos.repositories.EventCountProjection;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalTypeRepository;
import com.renatoganske.gestao_de_eventos.repositories.SpecialtyTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalTypeRepository professionalTypeRepository;
    private final SpecialtyTagRepository specialtyTagRepository;

    @Transactional
    public ProfessionalDto createProfessional(CreateProfessionalDto createProfessionalDto) {
        Professional professional = createProfessionalDto.toEntity();
        professional.setType(resolveProfessionalType(createProfessionalDto.typeId()));
        professional.setSpecialtyTags(resolveSpecialtyTags(createProfessionalDto.specialtyTagIds()));

        return professionalRepository.save(professional).toResponseDto(0);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalDto> getAllProfessionals() {
        Map<UUID, Integer> eventCounts = eventCountsByProfessional();
        return professionalRepository.findAll().stream()
                .map(professional -> professional.toResponseDto(EventCountProjection.countOf(eventCounts, professional.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionalDto getProfessionalById(UUID id) {
        Optional<Professional> optionalProfessional = professionalRepository.findById(id);
        return optionalProfessional.map(professional -> professional.toResponseDto(countEvents(professional)))
                .orElseThrow(() -> new ProfessionalNotFoundException(id));
    }

    @Transactional
    public ProfessionalDto updateProfessional(UUID id, CreateProfessionalDto createProfessionalDto) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ProfessionalNotFoundException(id));

        professional.setName(createProfessionalDto.name());
        professional.setType(resolveProfessionalType(createProfessionalDto.typeId()));
        professional.setContact(createProfessionalDto.contact());
        professional.setSpecialtyTags(resolveSpecialtyTags(createProfessionalDto.specialtyTagIds()));
        professional.setOtherInfo(createProfessionalDto.otherInfo());

        Professional updatedProfessional = professionalRepository.save(professional);
        return updatedProfessional.toResponseDto(countEvents(updatedProfessional));
    }

    @Transactional
    public void deleteProfessional(UUID id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ProfessionalNotFoundException(id));
        professionalRepository.delete(professional);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalDto> searchProfessionals(UUID typeId, UUID specialtyTagId) {
        Predicate<Professional> filter = ProfessionalFilter.byType(typeId)
                .and(ProfessionalFilter.bySpecialtyTag(specialtyTagId));

        Map<UUID, Integer> eventCounts = eventCountsByProfessional();
        return professionalRepository.findAll().stream()
                .filter(filter)
                .map(professional -> professional.toResponseDto(EventCountProjection.countOf(eventCounts, professional.getId())))
                .collect(Collectors.toList());
    }

    private Map<UUID, Integer> eventCountsByProfessional() {
        return EventCountProjection.toMap(professionalRepository.countEventsGroupedByProfessional());
    }

    private int countEvents(Professional professional) {
        return Math.toIntExact(professionalRepository.countEventsByProfessionalId(professional.getId()));
    }

    private ProfessionalType resolveProfessionalType(UUID typeId) {
        if (typeId == null) {
            return null;
        }
        return professionalTypeRepository.findById(typeId)
                .orElseThrow(() -> new ProfessionalTypeNotFoundException(typeId));
    }

    private Set<SpecialtyTag> resolveSpecialtyTags(List<UUID> specialtyTagIds) {
        if (specialtyTagIds == null || specialtyTagIds.isEmpty()) {
            return new HashSet<>();
        }
        return specialtyTagIds.stream()
                .map(tagId -> specialtyTagRepository.findById(tagId)
                        .orElseThrow(() -> new SpecialtyTagNotFoundException(tagId)))
                .collect(Collectors.toSet());
    }
}
