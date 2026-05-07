package com.company.customerinfo.service;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.repository.ShippingAddressRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;

    public ShippingAddressService(ShippingAddressRepository shippingAddressRepository) {
        this.shippingAddressRepository = shippingAddressRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customerByShippingAddress", key = "#shippingAddressID", unless = "#result == null")
    public Customer findCustomerByShippingAddressID(Integer shippingAddressID) {
        return shippingAddressRepository.findCustomerByShippingAddressID(shippingAddressID);
    }
}
