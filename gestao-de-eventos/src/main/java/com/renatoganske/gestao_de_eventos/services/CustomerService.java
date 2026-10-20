package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventCountProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponseDto createCustomer(CreateCustomerDto createCustomerDto) {
        // A just-created customer has no events yet.
        return customerRepository.save(createCustomerDto.toEntity()).toResponseDto(0);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        // One GROUP BY for every customer's event count (GDE-58), not one count query per customer.
        Map<UUID, Integer> eventCounts = EventCountProjection.toMap(customerRepository.countEventsGroupedByCustomer());
        return customerRepository.findAll().stream()
                .map(customer -> customer.toResponseDto(EventCountProjection.countOf(eventCounts, customer.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(UUID id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.map(customer -> customer.toResponseDto(countEvents(customer)))
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerResponseDto updateCustomer(UUID id, CreateCustomerDto createCustomerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setName(createCustomerDto.name());
        customer.setContact(createCustomerDto.contact());
        customer.setAddress(createCustomerDto.address());
        customer.setNotes(createCustomerDto.notes());

        Customer updatedCustomer = customerRepository.save(customer);
        return updatedCustomer.toResponseDto(countEvents(updatedCustomer));
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }

    private int countEvents(Customer customer) {
        return Math.toIntExact(customerRepository.countEventsByCustomerId(customer.getId()));
    }

}
