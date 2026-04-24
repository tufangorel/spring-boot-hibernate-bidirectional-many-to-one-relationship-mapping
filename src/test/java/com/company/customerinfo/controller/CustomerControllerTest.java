package com.company.customerinfo.controller;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(customerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void saveReturnsCreated() throws Exception {
        Customer customer = new Customer();
        customer.setName("name-1");
        when(customerService.save(any(Customer.class))).thenReturn(customer);

        mockMvc.perform(post("/customer/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"name-1\",\"age\":20}"))
                .andExpect(status().isCreated());

        verify(customerService).save(any(Customer.class));
    }

    @Test
    void listReturnsOk() throws Exception {
        when(customerService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/customer/list"))
                .andExpect(status().isOk());

        verify(customerService).findAll();
    }

    @Test
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/customer/delete/1"))
                .andExpect(status().isOk());

        verify(customerService).deleteCustomerById(1);
    }

    @Test
    void updateReturnsOk() throws Exception {
        Customer existing = new Customer();
        existing.setId(3);
        existing.setAge(10);
        when(customerService.findCustomerById(3)).thenReturn(Optional.of(existing));
        when(customerService.save(any(Customer.class))).thenReturn(existing);

        mockMvc.perform(put("/customer/update/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":25}"))
                .andExpect(status().isOk());

        verify(customerService).findCustomerById(3);
        verify(customerService).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenCustomerMissing() throws Exception {
        when(customerService.findCustomerById(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/customer/update/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":25}"))
                .andExpect(status().isNotFound());

        verify(customerService).findCustomerById(99);
    }
}
