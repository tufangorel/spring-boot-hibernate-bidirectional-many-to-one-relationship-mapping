package com.company.customerinfo.service;


import com.company.customerinfo.exception.ServiceUnavailableException;
import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.ratelimit.RateLimited;
import com.company.customerinfo.repository.CustomerOrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final IdempotencyService idempotencyService;
    private CustomerOrderService self;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository, IdempotencyService idempotencyService) {
        this.customerOrderRepository = customerOrderRepository;
        this.idempotencyService = idempotencyService;
    }

    @Autowired
    public void setSelf(@Lazy CustomerOrderService self) {
        this.self = self;
    }

    public CustomerOrder save(CustomerOrder customerOrder) {
        return self.save(customerOrder, null);
    }

    @RateLimited(key = "customerOrder.write")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "customerOrders", allEntries = true),
            @CacheEvict(value = "orderItems", allEntries = true)
    })
    @CircuitBreaker(name = "customerService", fallbackMethod = "saveFallback")
    @Retry(name = "customerService")
    public CustomerOrder save(CustomerOrder customerOrder, String idempotencyKey) {
        if (customerOrder == null) {
            throw new IllegalArgumentException("Customer order cannot be null");
        }
        log.info("Saving customer order: {}", customerOrder.getTitle());

        if (StringUtils.hasText(idempotencyKey)) {
            Optional<Integer> existingId = idempotencyService.findResourceId(idempotencyKey, "customerOrder");
            if (existingId.isPresent()) {
                return customerOrderRepository.findByIdWithAssociations(existingId.get())
                        .orElseThrow(() -> new IllegalStateException("Idempotency key exists but customer order resource is missing."));
            }
        }

        try {
            for (OrderItem orderItem : customerOrder.getOrderItems()) {
                orderItem.setCustomerOrder(customerOrder);
            }
            CustomerOrder savedCustomerOrder = customerOrderRepository.save(customerOrder);
            idempotencyService.saveRecord(idempotencyKey, "customerOrder", savedCustomerOrder.getId());
            return savedCustomerOrder;
        } catch (Exception ex) {
            log.error("Error saving customer order", ex);
            throw new ServiceUnavailableException("Failed to save customer order", ex);
        }
    }

    @RateLimited(key = "customerOrder.read")
    @Transactional(readOnly = true)
    @Cacheable(value = "customerOrders", key = "'all'", sync = true)
    @CircuitBreaker(name = "customerService", fallbackMethod = "findAllFallback")
    @Retry(name = "customerService")
    public List<CustomerOrder> findAll() {
        log.info("Fetching all customer orders");
        try {
            return customerOrderRepository.findAllWithAssociations();
        } catch (Exception ex) {
            log.error("Error fetching customer orders", ex);
            throw new ServiceUnavailableException("Failed to fetch customer orders", ex);
        }
    }

    @RateLimited(key = "customerOrder.write")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "customerOrders", allEntries = true),
            @CacheEvict(value = "orderItems", allEntries = true)
    })
    @CircuitBreaker(name = "customerService", fallbackMethod = "deleteByIdFallback")
    @Retry(name = "customerService")
    public void deleteCustomerOrderById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer order ID");
        }
        log.info("Deleting customer order with ID: {}", id);
        try {
            if (!customerOrderRepository.existsById(id)) {
                log.info("Customer order with ID {} not found, delete is idempotent", id);
                return;
            }
            customerOrderRepository.deleteById(id);
        } catch (Exception ex) {
            log.error("Error deleting customer order", ex);
            throw new ServiceUnavailableException("Failed to delete customer order", ex);
        }
    }

    @RateLimited(key = "customerOrder.read")
    @Transactional(readOnly = true)
    @Cacheable(value = "customerOrders", key = "#id", unless = "#result == null || #result.isEmpty()")
    public Optional<CustomerOrder> findById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer order ID");
        }
        log.debug("Finding customer order with ID: {}", id);
        try {
            Optional<CustomerOrder> order = customerOrderRepository.findByIdWithAssociations(id);
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
    public CustomerOrder saveFallback(CustomerOrder customerOrder, String idempotencyKey, Exception ex) {
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
