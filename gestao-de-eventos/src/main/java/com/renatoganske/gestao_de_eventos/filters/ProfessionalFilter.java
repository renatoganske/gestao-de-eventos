package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Professional;

import java.util.UUID;
import java.util.function.Predicate;

public interface ProfessionalFilter extends Predicate<Professional> {

    static ProfessionalFilter byType(UUID typeId) {
        return p -> typeId == null
                || (p.getType() != null && typeId.equals(p.getType().getId()));
    }

    static ProfessionalFilter bySpecialtyTag(UUID specialtyTagId) {
        return p -> specialtyTagId == null
                || (p.getSpecialtyTags() != null && p.getSpecialtyTags().stream()
                        .anyMatch(tag -> specialtyTagId.equals(tag.getId())));
    }
}
