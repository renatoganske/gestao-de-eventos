package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hds")
@Validated
@Tag(name = "Hd Controller", description = "CRUD for HD (storage drive) operations.")
public interface IHdController {

    @Operation(summary = "List HDs", description = "Fetches all HDs.")
    @GetMapping
    ResponseEntity<List<HdDto>> findAll();

    @Operation(summary = "Get HD by ID", description = "Fetches an HD by ID.")
    @GetMapping("/{id}")
    ResponseEntity<HdDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create HD", description = "Creates a new HD.")
    @PostMapping
    ResponseEntity<HdDto> create(@RequestBody @Valid CreateHdDto requestDto);

    @Operation(summary = "Update HD", description = "Updates an existing HD.")
    @PutMapping("/{id}")
    ResponseEntity<HdDto> update(@PathVariable UUID id, @RequestBody @Valid CreateHdDto requestDto);

    @Operation(summary = "Delete HD", description = "Deletes an HD by ID.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
