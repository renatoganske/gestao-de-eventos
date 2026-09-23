package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Validated
@Tag(name = "Event Controller", description = "CRUD for event operations.")
public interface IEventController {

    @Operation(summary = "List events", description = "Fetches all events.")
    @GetMapping
    ResponseEntity<List<EventDto>> findAll();

    @Operation(summary = "Get event by ID", description = "Fetches an event by ID.")
    @GetMapping("/{id}")
    ResponseEntity<EventDto> findById(@PathVariable UUID id);

    @Operation(summary = "Search events", description = "Searches events combining optional filters (type, venue, professional, period, HD, delivery status).")
    @GetMapping("/search")
    ResponseEntity<List<EventDto>> search(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hdId,
            @RequestParam(required = false) DeliveryStatus deliveryStatus);

    @Operation(summary = "Create event", description = "Creates a new event.")
    @PostMapping
    ResponseEntity<EventDto> create(@RequestBody @Valid CreateEventDto requestDto);

    @Operation(summary = "Update event", description = "Updates an existing event.")
    @PutMapping("/{id}")
    ResponseEntity<EventDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventDto requestDto);

    @Operation(summary = "Delete event", description = "Deletes an event by ID.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
