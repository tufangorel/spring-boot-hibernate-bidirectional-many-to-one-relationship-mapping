package com.company.customerinfo.service;

import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.repository.OrderItemRepository;
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
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void saveDelegatesToRepository() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        when(orderItemRepository.save(item)).thenReturn(item);

        OrderItem saved = orderItemService.save(item);

        assertThat(saved).isSameAs(item);
        verify(orderItemRepository).save(item);
    }

    @Test
    void findByIdReturnsRepositoryResult() {
        OrderItem item = new OrderItem();
        item.setId(7);
        when(orderItemRepository.findById(7)).thenReturn(Optional.of(item));

        Optional<OrderItem> result = orderItemService.findByID(7);

        assertThat(result).contains(item);
        verify(orderItemRepository).findById(7);
    }

    @Test
    void findAllReturnsRepositoryResult() {
        when(orderItemRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(orderItemService.findAll()).isEmpty();
        verify(orderItemRepository).findAll();
    }

    @Test
    void deleteDelegatesToRepository() {
        orderItemService.deleteOrderItemByID(5);

        verify(orderItemRepository).deleteById(5);
    }
}
