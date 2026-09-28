package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/professional-types")
@Validated
@Tag(name = "Professional Type Controller", description = "CRUD for professional type operations.")
public interface IProfessionalTypeController {

    @Operation(summary = "List professional types", description = "Fetches all professional types.")
    @GetMapping
    ResponseEntity<List<ProfessionalTypeDto>> findAll();

    @Operation(summary = "Get professional type by ID", description = "Fetches a professional type by ID.")
    @GetMapping("/{id}")
    ResponseEntity<ProfessionalTypeDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create professional type", description = "Creates a new professional type.")
    @PostMapping
    ResponseEntity<ProfessionalTypeDto> create(@RequestBody @Valid CreateProfessionalTypeDto requestDto);

    @Operation(summary = "Update professional type", description = "Updates an existing professional type.")
    @PutMapping("/{id}")
    ResponseEntity<ProfessionalTypeDto> update(@PathVariable UUID id, @RequestBody @Valid CreateProfessionalTypeDto requestDto);

    @Operation(summary = "Delete professional type", description = "Deletes a professional type by ID. Fails if any professional still uses it.",
            responses = @ApiResponse(responseCode = "204", description = "Professional type deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
