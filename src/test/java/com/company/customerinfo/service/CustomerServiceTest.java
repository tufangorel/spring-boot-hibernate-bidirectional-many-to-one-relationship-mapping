package com.company.customerinfo.service;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void saveDelegatesToRepository() {
        Customer customer = new Customer();
        customer.setName("test");
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer saved = customerService.save(customer);

        assertThat(saved).isSameAs(customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void findAllReturnsRepositoryResult() {
        when(customerRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(customerService.findAll()).isEmpty();
        verify(customerRepository).findAll();
    }

    @Test
    void deleteCustomerDelegatesToRepository() {
        customerService.deleteCustomerById(11);

        verify(customerRepository).deleteById(11);
    }

    @Test
    void findCustomerByIdReturnsRepositoryResult() {
        Customer customer = new Customer();
        customer.setId(11);
        when(customerRepository.findById(11)).thenReturn(Optional.of(customer));

        Optional<Customer> result = customerService.findCustomerById(11);

        assertThat(result).contains(customer);
        verify(customerRepository).findById(11);
    }
}
