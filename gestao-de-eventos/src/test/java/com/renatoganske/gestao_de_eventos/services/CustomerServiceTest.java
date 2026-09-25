package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CreateCustomerDto createCustomerDto;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Maria Silva")
                .contact("(11) 99999-0000")
                .address("Rua das Flores, 123")
                .notes("Cliente preferencial")
                .build();

        createCustomerDto = new CreateCustomerDto(
                customer.getName(),
                customer.getContact(),
                customer.getAddress(),
                customer.getNotes());
    }

    @Test
    void createCustomer_savesAndReturnsResponseDto() {
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        when(customerRepository.save(captor.capture())).thenReturn(customer);

        CustomerResponseDto result = customerService.createCustomer(createCustomerDto);

        assertThat(captor.getValue().getName()).isEqualTo(createCustomerDto.name());
        assertThat(captor.getValue().getContact()).isEqualTo(createCustomerDto.contact());
        assertThat(captor.getValue().getAddress()).isEqualTo(createCustomerDto.address());
        assertThat(captor.getValue().getNotes()).isEqualTo(createCustomerDto.notes());

        assertThat(result.id()).isEqualTo(customer.getId());
        assertThat(result.name()).isEqualTo(customer.getName());
        assertThat(result.contact()).isEqualTo(customer.getContact());
        assertThat(result.address()).isEqualTo(customer.getAddress());
        assertThat(result.notes()).isEqualTo(customer.getNotes());
    }

    @Test
    void getAllCustomers_returnsAllMappedCustomers() {
        Customer other = Customer.builder()
                .id(UUID.randomUUID())
                .name("João Souza")
                .build();
        when(customerRepository.findAll()).thenReturn(List.of(customer, other));

        List<CustomerResponseDto> result = customerService.getAllCustomers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CustomerResponseDto::name)
                .containsExactly(customer.getName(), other.getName());
    }

    @Test
    void getAllCustomers_returnsEmptyListWhenNoCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of());

        List<CustomerResponseDto> result = customerService.getAllCustomers();

        assertThat(result).isEmpty();
    }

    @Test
    void getCustomerById_returnsCustomerWhenFound() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        CustomerResponseDto result = customerService.getCustomerById(customer.getId());

        assertThat(result.id()).isEqualTo(customer.getId());
        assertThat(result.name()).isEqualTo(customer.getName());
    }

    @Test
    void getCustomerById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateCustomer_updatesAndReturnsCustomerWhenFound() {
        UUID id = customer.getId();
        CreateCustomerDto updateDto = new CreateCustomerDto("Maria Souza", "(11) 98888-0000", "Rua Nova, 456", "Atualizado");
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponseDto result = customerService.updateCustomer(id, updateDto);

        assertThat(result.name()).isEqualTo("Maria Souza");
        assertThat(result.contact()).isEqualTo("(11) 98888-0000");
        assertThat(result.address()).isEqualTo("Rua Nova, 456");
        assertThat(result.notes()).isEqualTo("Atualizado");
    }

    @Test
    void updateCustomer_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(id, createCustomerDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteCustomer_deletesWhenFound() {
        UUID id = customer.getId();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(id);

        verify(customerRepository, times(1)).delete(customer);
    }

    @Test
    void deleteCustomer_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());

        verify(customerRepository, never()).delete(any());
    }
}
