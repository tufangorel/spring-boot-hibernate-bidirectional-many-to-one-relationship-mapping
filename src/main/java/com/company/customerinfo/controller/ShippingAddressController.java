package com.company.customerinfo.controller;


import com.company.customerinfo.model.Customer;
import com.company.customerinfo.service.ShippingAddressService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shippingaddress")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    public ShippingAddressController(ShippingAddressService shippingAddressService) {
        this.shippingAddressService = shippingAddressService;
    }

    @Operation(summary = "Find a customer by shipping address id")
    @GetMapping(value = "/findcustomer/{shippingAddressID}", produces = "application/json")
    public ResponseEntity<Customer> findCustomerByShippingAddressID(@PathVariable Integer shippingAddressID){
        Customer customer = shippingAddressService.findCustomerByShippingAddressID(shippingAddressID);

        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(customer);
    }

}