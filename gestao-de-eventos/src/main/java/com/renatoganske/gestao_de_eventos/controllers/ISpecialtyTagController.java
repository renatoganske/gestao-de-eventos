package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateSpecialtyTagDto;
import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
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
@RequestMapping("/api/specialty-tags")
@Validated
@Tag(name = "Specialty Tag Controller", description = "CRUD for professional specialty tag operations.")
public interface ISpecialtyTagController {

    @Operation(summary = "List specialty tags", description = "Fetches all specialty tags.")
    @GetMapping
    ResponseEntity<List<SpecialtyTagDto>> findAll();

    @Operation(summary = "Get specialty tag by ID", description = "Fetches a specialty tag by ID.")
    @GetMapping("/{id}")
    ResponseEntity<SpecialtyTagDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create specialty tag", description = "Creates a new specialty tag.")
    @PostMapping
    ResponseEntity<SpecialtyTagDto> create(@RequestBody @Valid CreateSpecialtyTagDto requestDto);

    @Operation(summary = "Update specialty tag", description = "Updates an existing specialty tag.")
    @PutMapping("/{id}")
    ResponseEntity<SpecialtyTagDto> update(@PathVariable UUID id, @RequestBody @Valid CreateSpecialtyTagDto requestDto);

    @Operation(summary = "Delete specialty tag", description = "Deletes a specialty tag by ID. Fails if any professional still uses it.",
            responses = @ApiResponse(responseCode = "204", description = "Specialty tag deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
