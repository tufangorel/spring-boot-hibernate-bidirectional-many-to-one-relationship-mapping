package com.company.customerinfo.service;


import com.company.customerinfo.exception.ResourceNotFoundException;
import com.company.customerinfo.exception.ServiceUnavailableException;
import com.company.customerinfo.model.Customer;
import com.company.customerinfo.repository.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "saveFallback")
    @Retry(name = "customerService")
    public Customer save(Customer customer){
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        log.info("Saving customer: {}", customer.getName());
        try {
            return customerRepository.save(customer);
        } catch (Exception ex) {
            log.error("Error saving customer", ex);
            throw new ServiceUnavailableException("Failed to save customer", ex);
        }
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "customerService", fallbackMethod = "findAllFallback")
    @Retry(name = "customerService")
    public List<Customer> findAll(){
        log.info("Fetching all customers");
        try {
            return customerRepository.findAll();
        } catch (Exception ex) {
            log.error("Error fetching customers", ex);
            throw new ServiceUnavailableException("Failed to fetch customers", ex);
        }
    }

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "deleteByIdFallback")
    @Retry(name = "customerService")
    public void deleteCustomerById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }
        log.info("Deleting customer with ID: {}", id);
        try {
            if (!customerRepository.existsById(id)) {
                throw new ResourceNotFoundException("Customer with ID " + id + " not found");
            }
            customerRepository.deleteById(id);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting customer", ex);
            throw new ServiceUnavailableException("Failed to delete customer", ex);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }
        log.debug("Finding customer with ID: {}", id);
        try {
            Optional<Customer> customer = customerRepository.findById(id);
            if (customer.isEmpty()) {
                log.warn("Customer with ID {} not found", id);
            }
            return customer;
        } catch (Exception ex) {
            log.error("Error finding customer", ex);
            throw new ServiceUnavailableException("Failed to find customer", ex);
        }
    }

    // Fallback methods
    public Customer saveFallback(Customer customer, Exception ex) {
        log.error("Circuit breaker triggered for save operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }

    public List<Customer> findAllFallback(Exception ex) {
        log.error("Circuit breaker triggered for findAll operation", ex);
        return List.of();
    }

    public void deleteByIdFallback(Integer id, Exception ex) {
        log.error("Circuit breaker triggered for delete operation", ex);
        throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
    }
}