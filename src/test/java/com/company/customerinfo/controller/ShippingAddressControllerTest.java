package com.company.customerinfo.controller;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.service.ShippingAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShippingAddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShippingAddressService shippingAddressService;

    @BeforeEach
    void setUp() {
        ShippingAddressController controller = new ShippingAddressController(shippingAddressService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void findCustomerByShippingAddressIdReturnsOkWhenCustomerExists() throws Exception {
        Customer customer = new Customer();
        customer.setId(10);
        customer.setName("customer-10");
        when(shippingAddressService.findCustomerByShippingAddressID(1)).thenReturn(customer);

        mockMvc.perform(get("/shippingaddress/findcustomer/1"))
                .andExpect(status().isOk());

        verify(shippingAddressService).findCustomerByShippingAddressID(1);
    }

    @Test
    void findCustomerByShippingAddressIdReturnsNotFoundWhenMissing() throws Exception {
        when(shippingAddressService.findCustomerByShippingAddressID(2)).thenReturn(null);

        mockMvc.perform(get("/shippingaddress/findcustomer/2"))
                .andExpect(status().isNotFound());

        verify(shippingAddressService).findCustomerByShippingAddressID(2);
    }
}
