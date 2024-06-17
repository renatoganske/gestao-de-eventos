package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IClienteController;
import com.renatoganske.gestao_de_eventos.dtos.ClienteRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import com.renatoganske.gestao_de_eventos.services.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClienteController implements IClienteController {

    private final ClienteService clienteService;

    @Override
    public ResponseEntity<List<ClienteResponseDto>> findAll() {
        return ResponseEntity.ok(clienteService.getAllClientes());
    }

    @Override
    public ResponseEntity<ClienteResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.getClienteById(id));
    }

    @Override
    public ResponseEntity<ClienteResponseDto> create(@RequestBody @Valid ClienteRequestDto requestDto) {
        ClienteResponseDto responseDto = clienteService.createCliente(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<ClienteResponseDto> update(@PathVariable UUID id, @RequestBody @Valid ClienteRequestDto requestDto) {
        return ResponseEntity.ok(clienteService.updateCliente(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clienteService.deleteCliente(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
