package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Search events", description = "Searches events combining optional filters (type, venue, professional, period, HD, delivery status, customer name, event code, daytime wedding, outdoor wedding). "
            + "'daytimeWedding' and 'outdoorWedding' only match events whose flag is explicitly true or false; "
            + "events where the flag is not set (for example, non-wedding events) are left out whenever one of them is provided.")
    @GetMapping("/search")
    ResponseEntity<List<EventDto>> search(
            @RequestParam(required = false) UUID eventTypeId,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hdId,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) Boolean daytimeWedding,
            @RequestParam(required = false) Boolean outdoorWedding);

    @Operation(summary = "Create event", description = "Creates a new event, optionally with the professionals who worked on it. "
            + "The 'professionals' list is saved in the same transaction as the event; omitting it (null) creates the event with no team. "
            + "A professional repeated in the list keeps only its last role, since one professional holds at most one role per event.")
    @PostMapping
    ResponseEntity<EventDto> create(@RequestBody @Valid CreateEventDto requestDto);

    @Operation(summary = "Update event", description = "Updates an existing event. "
            + "The 'professionals' list replaces the event's whole team: omitting it (null) leaves the current team untouched, "
            + "while an empty list clears it.")
    @PutMapping("/{id}")
    ResponseEntity<EventDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventDto requestDto);

    @Operation(summary = "Delete event", description = "Deletes an event by ID.",
            responses = @ApiResponse(responseCode = "204", description = "Event deleted."))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
