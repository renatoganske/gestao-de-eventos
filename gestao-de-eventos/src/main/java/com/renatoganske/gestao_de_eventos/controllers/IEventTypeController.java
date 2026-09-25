package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
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
@RequestMapping("/api/event-types")
@Validated
@Tag(name = "Event Type Controller", description = "CRUD for event type operations.")
public interface IEventTypeController {

    @Operation(summary = "List event types", description = "Fetches all event types.")
    @GetMapping
    ResponseEntity<List<EventTypeDto>> findAll();

    @Operation(summary = "Get event type by ID", description = "Fetches an event type by ID.")
    @GetMapping("/{id}")
    ResponseEntity<EventTypeDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create event type", description = "Creates a new event type.")
    @PostMapping
    ResponseEntity<EventTypeDto> create(@RequestBody @Valid CreateEventTypeDto requestDto);

    @Operation(summary = "Update event type", description = "Updates an existing event type.")
    @PutMapping("/{id}")
    ResponseEntity<EventTypeDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventTypeDto requestDto);

    @Operation(summary = "Delete event type", description = "Deletes an event type by ID.",
            responses = @ApiResponse(responseCode = "204", description = "Event type deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
