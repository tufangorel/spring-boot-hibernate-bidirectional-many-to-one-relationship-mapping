package com.company.customerinfo.controller;

import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.service.CustomerOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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

@WebMvcTest(CustomerOrderController.class)
class CustomerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerOrderService customerOrderService;

    @Test
    void saveReturnsCreated() throws Exception {
        CustomerOrder order = new CustomerOrder();
        order.setId(2);
        when(customerOrderService.save(any(CustomerOrder.class))).thenReturn(order);

        mockMvc.perform(post("/customerorder/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"order-1\"}"))
                .andExpect(status().isCreated());

        verify(customerOrderService).save(any(CustomerOrder.class));
    }

    @Test
    void listReturnsOk() throws Exception {
        when(customerOrderService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/customerorder/list"))
                .andExpect(status().isOk());

        verify(customerOrderService).findAll();
    }

    @Test
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/customerorder/delete/4"))
                .andExpect(status().isOk());

        verify(customerOrderService).deleteCustomerOrder(4);
    }

    @Test
    void updateReturnsOk() throws Exception {
        CustomerOrder existing = new CustomerOrder();
        existing.setId(9);
        existing.setOrderDate(LocalDateTime.now().minusDays(1));
        when(customerOrderService.findByID(9)).thenReturn(Optional.of(existing));
        when(customerOrderService.save(any(CustomerOrder.class))).thenReturn(existing);

        mockMvc.perform(put("/customerorder/update/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated\"}"))
                .andExpect(status().isOk());

        verify(customerOrderService).findByID(9);
        verify(customerOrderService).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenOrderMissing() throws Exception {
        when(customerOrderService.findByID(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/customerorder/update/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated\"}"))
                .andExpect(status().isNotFound());

        verify(customerOrderService).findByID(99);
    }
}
