package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
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
@RequestMapping("/api/professionals")
@Validated
@Tag(name = "Professional Controller", description = "CRUD for professional operations.")
public interface IProfessionalController {

    @Operation(summary = "List professionals", description = "Fetches all professionals.")
    @GetMapping
    ResponseEntity<List<ProfessionalDto>> findAll();

    @Operation(summary = "Get professional by ID", description = "Fetches a professional by ID.")
    @GetMapping("/{id}")
    ResponseEntity<ProfessionalDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create professional", description = "Creates a new professional.")
    @PostMapping
    ResponseEntity<ProfessionalDto> create(@RequestBody @Valid CreateProfessionalDto requestDto);

    @Operation(summary = "Update professional", description = "Updates an existing professional.")
    @PutMapping("/{id}")
    ResponseEntity<ProfessionalDto> update(@PathVariable UUID id, @RequestBody @Valid CreateProfessionalDto requestDto);

    @Operation(summary = "Delete professional", description = "Deletes a professional by ID.",
            responses = @ApiResponse(responseCode = "204", description = "Professional deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
