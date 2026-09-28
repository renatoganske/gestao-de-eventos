package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IProfessionalTypeController;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.services.ProfessionalTypeService;
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
public class ProfessionalTypeController implements IProfessionalTypeController {

    private final ProfessionalTypeService professionalTypeService;

    @Override
    public ResponseEntity<List<ProfessionalTypeDto>> findAll() {
        return ResponseEntity.ok(professionalTypeService.getAllProfessionalTypes());
    }

    @Override
    public ResponseEntity<ProfessionalTypeDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(professionalTypeService.getProfessionalTypeById(id));
    }

    @Override
    public ResponseEntity<ProfessionalTypeDto> create(@RequestBody @Valid CreateProfessionalTypeDto requestDto) {
        ProfessionalTypeDto responseDto = professionalTypeService.createProfessionalType(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<ProfessionalTypeDto> update(@PathVariable UUID id, @RequestBody @Valid CreateProfessionalTypeDto requestDto) {
        return ResponseEntity.ok(professionalTypeService.updateProfessionalType(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        professionalTypeService.deleteProfessionalType(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
