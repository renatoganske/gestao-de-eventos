package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.ClienteRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import com.renatoganske.gestao_de_eventos.entities.Cliente;
import com.renatoganske.gestao_de_eventos.repositories.ClienteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
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
    public ClienteResponseDto createCliente(ClienteRequestDto clienteRequestDto) {
        return clienteRepository.save(clienteRequestDto.toEntity()).toResponseDto();
    }

    public List<ClienteResponseDto> getAllClientes() {
        return clienteRepository.findAll().stream()
                .map(Cliente::toResponseDto)
                .collect(Collectors.toList());
    }

    public ClienteResponseDto getClienteById(UUID id) {
        Optional<Cliente> optionalCliente = clienteRepository.findById(id);
        return optionalCliente.map(Cliente::toResponseDto)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));
    }

    @Transactional
    public ClienteResponseDto updateCliente(UUID id, ClienteRequestDto clienteRequestDto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));

        cliente.setNome(clienteRequestDto.nome());
        cliente.setContato(clienteRequestDto.contato());
        cliente.setEndereco(clienteRequestDto.endereco());
        cliente.setObservacoes(clienteRequestDto.observacoes());

        Cliente updatedCliente = clienteRepository.save(cliente);
        return updatedCliente.toResponseDto();
    }

    @Transactional
    public void deleteCliente(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente not found with id: " + id));
        clienteRepository.delete(cliente);
    }

}
