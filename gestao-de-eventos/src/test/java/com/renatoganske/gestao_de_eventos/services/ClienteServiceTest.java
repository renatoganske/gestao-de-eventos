package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.repositories.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Customer customer;
    private CreateCustomerDto createCustomerDto;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .nome("Maria Silva")
                .contato("(11) 99999-0000")
                .endereco("Rua das Flores, 123")
                .observacoes("Cliente preferencial")
                .build();

        createCustomerDto = new CreateCustomerDto(
                customer.getNome(),
                customer.getContato(),
                customer.getEndereco(),
                customer.getObservacoes());
    }

    @Test
    void createCliente_deveSalvarEDevolverDtoDeResposta() {
        when(clienteRepository.save(any(Customer.class))).thenReturn(customer);

        ClienteResponseDto result = clienteService.createCliente(createCustomerDto);

        assertThat(result.id()).isEqualTo(customer.getId());
        assertThat(result.nome()).isEqualTo(customer.getNome());
        assertThat(result.contato()).isEqualTo(customer.getContato());
        assertThat(result.endereco()).isEqualTo(customer.getEndereco());
        assertThat(result.observacoes()).isEqualTo(customer.getObservacoes());
    }

    @Test
    void getAllClientes_deveDevolverTodosOsClientesMapeados() {
        Customer other = Customer.builder()
                .id(UUID.randomUUID())
                .nome("João Souza")
                .build();
        when(clienteRepository.findAll()).thenReturn(List.of(customer, other));

        List<ClienteResponseDto> result = clienteService.getAllClientes();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClienteResponseDto::nome)
                .containsExactly(customer.getNome(), other.getNome());
    }

    @Test
    void getAllClientes_devolveListaVaziaQuandoNaoHaClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of());

        List<ClienteResponseDto> result = clienteService.getAllClientes();

        assertThat(result).isEmpty();
    }

    @Test
    void getClienteById_deveDevolverClienteQuandoEncontrado() {
        when(clienteRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        ClienteResponseDto result = clienteService.getClienteById(customer.getId());

        assertThat(result.id()).isEqualTo(customer.getId());
        assertThat(result.nome()).isEqualTo(customer.getNome());
    }

    @Test
    void getClienteById_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.getClienteById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateCliente_deveAtualizarEDevolverClienteQuandoEncontrado() {
        UUID id = customer.getId();
        CreateCustomerDto updateDto = new CreateCustomerDto("Maria Souza", "(11) 98888-0000", "Rua Nova, 456", "Atualizado");
        when(clienteRepository.findById(id)).thenReturn(Optional.of(customer));
        when(clienteRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDto result = clienteService.updateCliente(id, updateDto);

        assertThat(result.nome()).isEqualTo("Maria Souza");
        assertThat(result.contato()).isEqualTo("(11) 98888-0000");
        assertThat(result.endereco()).isEqualTo("Rua Nova, 456");
        assertThat(result.observacoes()).isEqualTo("Atualizado");
    }

    @Test
    void updateCliente_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.updateCliente(id, createCustomerDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void deleteCliente_deveExcluirQuandoEncontrado() {
        UUID id = customer.getId();
        when(clienteRepository.findById(id)).thenReturn(Optional.of(customer));

        clienteService.deleteCliente(id);

        verify(clienteRepository, times(1)).delete(customer);
    }

    @Test
    void deleteCliente_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.deleteCliente(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());

        verify(clienteRepository, never()).delete(any());
    }
}
