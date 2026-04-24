package com.company.customerinfo.controller;

import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.service.CustomerOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerOrderService customerOrderService;

    @BeforeEach
    void setUp() {
        CustomerOrderController controller = new CustomerOrderController(customerOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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

        verify(customerOrderService).deleteCustomerOrderById(4);
    }

    @Test
    void updateReturnsOk() throws Exception {
        CustomerOrder existing = new CustomerOrder();
        existing.setId(9);
        existing.setOrderDate(LocalDateTime.now().minusDays(1));
        when(customerOrderService.findById(9)).thenReturn(Optional.of(existing));
        when(customerOrderService.save(any(CustomerOrder.class))).thenReturn(existing);

        mockMvc.perform(put("/customerorder/update/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated\"}"))
                .andExpect(status().isOk());

        verify(customerOrderService).findById(9);
        verify(customerOrderService).findById(9);
    }

    @Test
    void updateReturnsNotFoundWhenOrderMissing() throws Exception {
        when(customerOrderService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/customerorder/update/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated\"}"))
                .andExpect(status().isNotFound());

        verify(customerOrderService).findById(99);
    }
}
