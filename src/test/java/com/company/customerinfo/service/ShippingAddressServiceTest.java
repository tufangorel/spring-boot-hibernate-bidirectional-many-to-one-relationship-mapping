package com.company.customerinfo.service;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.repository.ShippingAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceTest {

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @InjectMocks
    private ShippingAddressService shippingAddressService;

    @Test
    void findCustomerByShippingAddressIdReturnsRepositoryResult() {
        Customer customer = new Customer();
        customer.setId(13);
        when(shippingAddressRepository.findCustomerByShippingAddressID(4)).thenReturn(customer);

        Customer result = shippingAddressService.findCustomerByShippingAddressID(4);

        assertThat(result).isSameAs(customer);
        verify(shippingAddressRepository).findCustomerByShippingAddressID(4);
    }
}
