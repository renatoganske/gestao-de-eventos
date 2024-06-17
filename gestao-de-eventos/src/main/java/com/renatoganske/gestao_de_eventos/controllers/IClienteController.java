package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.ClienteRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@Validated
@Tag(name = "Controller de Clientes", description = "Crud para realizar operações com clientes.")
public interface IClienteController {

    @Operation(summary = "Listar clientes", description = "Busca todos os clientes.")
    @GetMapping
    ResponseEntity<List<ClienteResponseDto>> findAll();

    @Operation(summary = "Listar cliente por ID", description = "Busca um cliente pelo ID.")
    @GetMapping("/{id}")
    ResponseEntity<ClienteResponseDto> findById(@PathVariable UUID id);

    @Operation(summary = "Cadastrar cliente", description = "Cadastra um novo cliente.")
    @PostMapping
    ResponseEntity<ClienteResponseDto> create(@RequestBody @Valid ClienteRequestDto requestDto);

    @Operation(summary = "Atualizar cliente", description = "Atualiza um cliente existente.")
    @PutMapping("/{id}")
    ResponseEntity<ClienteResponseDto> update(@PathVariable UUID id, @RequestBody @Valid ClienteRequestDto requestDto);

    @Operation(summary = "Excluir cliente", description = "Exclui um cliente pelo ID.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
