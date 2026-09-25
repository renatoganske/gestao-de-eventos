package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
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
@RequestMapping("/api/event-venues")
@Validated
@Tag(name = "Event Venue Controller", description = "CRUD for event venue operations.")
public interface IEventVenueController {

    @Operation(summary = "List event venues", description = "Fetches all event venues.")
    @GetMapping
    ResponseEntity<List<EventVenueDto>> findAll();

    @Operation(summary = "Get event venue by ID", description = "Fetches an event venue by ID.")
    @GetMapping("/{id}")
    ResponseEntity<EventVenueDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create event venue", description = "Creates a new event venue.")
    @PostMapping
    ResponseEntity<EventVenueDto> create(@RequestBody @Valid CreateEventVenueDto requestDto);

    @Operation(summary = "Update event venue", description = "Updates an existing event venue.")
    @PutMapping("/{id}")
    ResponseEntity<EventVenueDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventVenueDto requestDto);

    @Operation(summary = "Delete event venue", description = "Deletes an event venue by ID.",
            responses = @ApiResponse(responseCode = "204", description = "Event venue deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
