package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IEventTypeController;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
import com.renatoganske.gestao_de_eventos.services.EventTypeService;
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
public class EventTypeController implements IEventTypeController {

    private final EventTypeService eventTypeService;

    @Override
    public ResponseEntity<List<EventTypeDto>> findAll() {
        return ResponseEntity.ok(eventTypeService.getAllEventTypes());
    }

    @Override
    public ResponseEntity<EventTypeDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventTypeService.getEventTypeById(id));
    }

    @Override
    public ResponseEntity<EventTypeDto> create(@RequestBody @Valid CreateEventTypeDto requestDto) {
        EventTypeDto responseDto = eventTypeService.createEventType(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<EventTypeDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventTypeDto requestDto) {
        return ResponseEntity.ok(eventTypeService.updateEventType(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventTypeService.deleteEventType(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
