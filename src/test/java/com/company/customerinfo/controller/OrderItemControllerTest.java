package com.company.customerinfo.controller;

import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.service.OrderItemService;
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
class OrderItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderItemService orderItemService;

    @BeforeEach
    void setUp() {
        OrderItemController controller = new OrderItemController(orderItemService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void saveReturnsCreated() throws Exception {
        OrderItem item = new OrderItem();
        item.setId(12);
        when(orderItemService.save(any(OrderItem.class))).thenReturn(item);

        mockMvc.perform(post("/orderitem/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isCreated());

        verify(orderItemService).save(any(OrderItem.class));
    }

    @Test
    void listReturnsOk() throws Exception {
        when(orderItemService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orderitem/list"))
                .andExpect(status().isOk());

        verify(orderItemService).findAll();
    }

    @Test
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/orderitem/delete/5"))
                .andExpect(status().isOk());

        verify(orderItemService).deleteOrderItemById(5);
    }

    @Test
    void updateReturnsOk() throws Exception {
        OrderItem existing = new OrderItem();
        existing.setId(7);
        existing.setQuantity(1);
        when(orderItemService.findById(7)).thenReturn(Optional.of(existing));
        when(orderItemService.save(any(OrderItem.class))).thenReturn(existing);

        mockMvc.perform(put("/orderitem/update/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8}"))
                .andExpect(status().isOk());

        verify(orderItemService).findById(7);
        verify(orderItemService).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenItemMissing() throws Exception {
        when(orderItemService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/orderitem/update/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8}"))
                .andExpect(status().isNotFound());

        verify(orderItemService).findById(99);
    }
}
