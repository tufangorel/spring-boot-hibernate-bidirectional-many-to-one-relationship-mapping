package com.company.customerinfo.service;


import com.company.customerinfo.exception.ResourceNotFoundException;
import com.company.customerinfo.exception.ServiceUnavailableException;
import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.repository.OrderItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "saveFallback")
    @Retry(name = "customerService")
    public OrderItem save(OrderItem orderItem){
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        log.info("Saving order item with quantity: {}", orderItem.getQuantity());
        try {
            return orderItemRepository.save(orderItem);
        } catch (Exception ex) {
            log.error("Error saving order item", ex);
            throw new ServiceUnavailableException("Failed to save order item", ex);
        }
    }

    @Transactional(readOnly = true)
    public Optional<OrderItem> findById(Integer id){
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid order item ID");
        }
        log.debug("Finding order item with ID: {}", id);
        try {
            Optional<OrderItem> item = orderItemRepository.findById(id);
            if (item.isEmpty()) {
                log.warn("Order item with ID {} not found", id);
            }
            return item;
        } catch (Exception ex) {
            log.error("Error finding order item", ex);
            throw new ServiceUnavailableException("Failed to find order item", ex);
        }
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "customerService", fallbackMethod = "findAllFallback")
    @Retry(name = "customerService")
    public List<OrderItem> findAll() {
        log.info("Fetching all order items");
        try {
            return orderItemRepository.findAll();
        } catch (Exception ex) {
            log.error("Error fetching order items", ex);
            throw new ServiceUnavailableException("Failed to fetch order items", ex);
        }
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "deleteByIdFallback")
    @Retry(name = "customerService")
    public void deleteOrderItemById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid order item ID");
        }
        log.info("Deleting order item with ID: {}", id);
        try {
            if (!orderItemRepository.existsById(id)) {
                throw new ResourceNotFoundException("Order item with ID " + id + " not found");
            }
            orderItemRepository.deleteById(id);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting order item", ex);
            throw new ServiceUnavailableException("Failed to delete order item", ex);
        }
    }

    // Fallback methods
    public OrderItem saveFallback(OrderItem orderItem, Exception ex) {
        log.error("Circuit breaker triggered for save operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }

    public List<OrderItem> findAllFallback(Exception ex) {
        log.error("Circuit breaker triggered for findAll operation", ex);
        return List.of();
    }

    public void deleteByIdFallback(Integer id, Exception ex) {
        log.error("Circuit breaker triggered for delete operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }
}