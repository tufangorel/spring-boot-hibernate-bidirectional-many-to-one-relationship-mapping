package com.company.customerinfo.service;

import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.repository.CustomerOrderRepository;
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
class CustomerOrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @InjectMocks
    private CustomerOrderService customerOrderService;

    @Test
    void saveAssignsBackReferenceForAllOrderItems() {
        CustomerOrder order = new CustomerOrder();
        OrderItem first = new OrderItem();
        OrderItem second = new OrderItem();
        order.addOrderItem(first);
        order.addOrderItem(second);
        when(customerOrderRepository.save(order)).thenReturn(order);

        CustomerOrder saved = customerOrderService.save(order);

        assertThat(saved).isSameAs(order);
        assertThat(first.getCustomerOrder()).isSameAs(order);
        assertThat(second.getCustomerOrder()).isSameAs(order);
        verify(customerOrderRepository).save(order);
    }

    @Test
    void findAllReturnsRepositoryResult() {
        when(customerOrderRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(customerOrderService.findAll()).isEmpty();
        verify(customerOrderRepository).findAll();
    }

    @Test
    void deleteDelegatesToRepository() {
        customerOrderService.deleteCustomerOrderById(9);

        verify(customerOrderRepository).deleteById(9);
    }

    @Test
    void findByIdReturnsRepositoryResult() {
        CustomerOrder order = new CustomerOrder();
        order.setId(6);
        when(customerOrderRepository.findById(6)).thenReturn(Optional.of(order));

        Optional<CustomerOrder> result = customerOrderService.findById(6);

        assertThat(result).contains(order);
        verify(customerOrderRepository).findById(6);
    }
}
