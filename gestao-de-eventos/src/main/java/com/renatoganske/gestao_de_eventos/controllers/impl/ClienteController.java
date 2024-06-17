package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IClienteController;
import com.renatoganske.gestao_de_eventos.dtos.ClienteRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;


public class ClienteController implements IClienteController {
    @Override
    public ResponseEntity<List<ClienteResponseDto>> findAll() {
        return null;
    }

    @Override
    public ResponseEntity<ClienteResponseDto> findById(UUID id) {
        return null;
    }

    @Override
    public ResponseEntity<ClienteResponseDto> create(ClienteRequestDto requestDto) {
        return null;
    }

    @Override
    public ResponseEntity<ClienteResponseDto> update(UUID id, ClienteRequestDto requestDto) {
        return null;
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        return null;
    }
}
