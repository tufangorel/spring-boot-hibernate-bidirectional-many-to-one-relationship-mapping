package com.company.customerinfo.controller;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.service.ShippingAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShippingAddressController.class)
class ShippingAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShippingAddressService shippingAddressService;

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
    void findCustomerByShippingAddressIdReturnsBadRequestWhenMissing() throws Exception {
        when(shippingAddressService.findCustomerByShippingAddressID(2)).thenReturn(null);

        mockMvc.perform(get("/shippingaddress/findcustomer/2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Customer at this shipping address not found!"));

        verify(shippingAddressService).findCustomerByShippingAddressID(2);
    }
}
