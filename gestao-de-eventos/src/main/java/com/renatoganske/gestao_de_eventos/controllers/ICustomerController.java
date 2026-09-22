package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@Validated
@Tag(name = "Customer Controller", description = "CRUD for customer operations.")
public interface ICustomerController {

    @Operation(summary = "List customers", description = "Fetches all customers.")
    @GetMapping
    ResponseEntity<List<CustomerResponseDto>> findAll();

    @Operation(summary = "Get customer by ID", description = "Fetches a customer by ID.")
    @GetMapping("/{id}")
    ResponseEntity<CustomerResponseDto> findById(@PathVariable UUID id);

    @Operation(summary = "Create customer", description = "Creates a new customer.")
    @PostMapping
    ResponseEntity<CustomerResponseDto> create(@RequestBody @Valid CreateCustomerDto requestDto);

    @Operation(summary = "Update customer", description = "Updates an existing customer.")
    @PutMapping("/{id}")
    ResponseEntity<CustomerResponseDto> update(@PathVariable UUID id, @RequestBody @Valid CreateCustomerDto requestDto);

    @Operation(summary = "Delete customer", description = "Deletes a customer by ID.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
