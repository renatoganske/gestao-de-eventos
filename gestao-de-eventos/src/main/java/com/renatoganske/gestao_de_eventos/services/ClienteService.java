package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.repositories.ClienteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponseDto createCliente(CreateCustomerDto createCustomerDto) {
        return clienteRepository.save(createCustomerDto.toEntity()).toResponseDto();
    }

    public List<ClienteResponseDto> getAllClientes() {
        return clienteRepository.findAll().stream()
                .map(Customer::toResponseDto)
                .collect(Collectors.toList());
    }

    public ClienteResponseDto getClienteById(UUID id) {
        Optional<Customer> optionalCliente = clienteRepository.findById(id);
        return optionalCliente.map(Customer::toResponseDto)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));
    }

    @Transactional
    public ClienteResponseDto updateCliente(UUID id, CreateCustomerDto createCustomerDto) {
        Customer customer = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));

        customer.setNome(createCustomerDto.nome());
        customer.setContato(createCustomerDto.contato());
        customer.setEndereco(createCustomerDto.endereco());
        customer.setObservacoes(createCustomerDto.observacoes());

        Customer updatedCustomer = clienteRepository.save(customer);
        return updatedCustomer.toResponseDto();
    }

    @Transactional
    public void deleteCliente(UUID id) {
        Customer customer = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));
        clienteRepository.delete(customer);
    }

}
