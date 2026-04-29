package com.company.customerinfo.service;


import com.company.customerinfo.exception.ResourceNotFoundException;
import com.company.customerinfo.exception.ServiceUnavailableException;
import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.repository.CustomerOrderRepository;
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
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "saveFallback")
    @Retry(name = "customerService")
    public CustomerOrder save(CustomerOrder customerOrder){
        if (customerOrder == null) {
            throw new IllegalArgumentException("Customer order cannot be null");
        }
        log.info("Saving customer order: {}", customerOrder.getTitle());
        try {
            for( OrderItem orderItem: customerOrder.getOrderItems() ) {
                orderItem.setCustomerOrder(customerOrder);
            }
            return customerOrderRepository.save(customerOrder);
        } catch (Exception ex) {
            log.error("Error saving customer order", ex);
            throw new ServiceUnavailableException("Failed to save customer order", ex);
        }
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "customerService", fallbackMethod = "findAllFallback")
    @Retry(name = "customerService")
    public List<CustomerOrder> findAll(){
        log.info("Fetching all customer orders");
        try {
            return customerOrderRepository.findAll();
        } catch (Exception ex) {
            log.error("Error fetching customer orders", ex);
            throw new ServiceUnavailableException("Failed to fetch customer orders", ex);
        }
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "deleteByIdFallback")
    @Retry(name = "customerService")
    public void deleteCustomerOrderById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer order ID");
        }
        log.info("Deleting customer order with ID: {}", id);
        try {
            if (!customerOrderRepository.existsById(id)) {
                throw new ResourceNotFoundException("Customer order with ID " + id + " not found");
            }
            customerOrderRepository.deleteById(id);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting customer order", ex);
            throw new ServiceUnavailableException("Failed to delete customer order", ex);
        }
    }

    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer order ID");
        }
        log.debug("Finding customer order with ID: {}", id);
        try {
            Optional<CustomerOrder> order = customerOrderRepository.findById(id);
            if (order.isEmpty()) {
                log.warn("Customer order with ID {} not found", id);
            }
            return order;
        } catch (Exception ex) {
            log.error("Error finding customer order", ex);
            throw new ServiceUnavailableException("Failed to find customer order", ex);
        }
    }

    // Fallback methods
    public CustomerOrder saveFallback(CustomerOrder customerOrder, Exception ex) {
        log.error("Circuit breaker triggered for save operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }

    public List<CustomerOrder> findAllFallback(Exception ex) {
        log.error("Circuit breaker triggered for findAll operation", ex);
        return List.of();
    }

    public void deleteByIdFallback(Integer id, Exception ex) {
        log.error("Circuit breaker triggered for delete operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }
}