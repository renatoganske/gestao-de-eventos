package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IProfessionalController;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.services.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProfessionalController implements IProfessionalController {

    private final ProfessionalService professionalService;

    @Override
    public ResponseEntity<List<ProfessionalDto>> findAll() {
        return ResponseEntity.ok(professionalService.getAllProfessionals());
    }

    @Override
    public ResponseEntity<ProfessionalDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(professionalService.getProfessionalById(id));
    }

    @Override
    public ResponseEntity<List<ProfessionalDto>> search(UUID typeId, UUID specialtyTagId) {
        return ResponseEntity.ok(professionalService.searchProfessionals(typeId, specialtyTagId));
    }

    @Override
    public ResponseEntity<ProfessionalDto> create(@RequestBody @Valid CreateProfessionalDto requestDto) {
        ProfessionalDto responseDto = professionalService.createProfessional(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<ProfessionalDto> update(@PathVariable UUID id, @RequestBody @Valid CreateProfessionalDto requestDto) {
        return ResponseEntity.ok(professionalService.updateProfessional(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        professionalService.deleteProfessional(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
