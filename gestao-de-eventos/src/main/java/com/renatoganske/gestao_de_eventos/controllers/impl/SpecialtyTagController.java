package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.ISpecialtyTagController;
import com.renatoganske.gestao_de_eventos.dtos.CreateSpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.services.SpecialtyTagService;
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
public class SpecialtyTagController implements ISpecialtyTagController {

    private final SpecialtyTagService specialtyTagService;

    @Override
    public ResponseEntity<List<SpecialtyTagDto>> findAll() {
        return ResponseEntity.ok(specialtyTagService.getAllSpecialtyTags());
    }

    @Override
    public ResponseEntity<SpecialtyTagDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(specialtyTagService.getSpecialtyTagById(id));
    }

    @Override
    public ResponseEntity<SpecialtyTagDto> create(@RequestBody @Valid CreateSpecialtyTagDto requestDto) {
        SpecialtyTagDto responseDto = specialtyTagService.createSpecialtyTag(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<SpecialtyTagDto> update(@PathVariable UUID id, @RequestBody @Valid CreateSpecialtyTagDto requestDto) {
        return ResponseEntity.ok(specialtyTagService.updateSpecialtyTag(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        specialtyTagService.deleteSpecialtyTag(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
