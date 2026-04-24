package com.company.customerinfo.controller;

import com.company.customerinfo.model.Customer;
import com.company.customerinfo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Create a new customer record")
    @ApiResponses(value = {
            @ApiResponse( responseCode = "201", description = "customer created", content = { @Content(mediaType = "application/json")} ),
            @ApiResponse(responseCode = "404", description = "Bad request") })
    @PostMapping("/save")
    public ResponseEntity<Customer> save(@RequestBody Customer customer) {
        Customer response = customerService.save(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View a list of customers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    })
    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity<Iterable<Customer>> list(){
        Iterable<Customer> customers = customerService.findAll();
        return ResponseEntity.ok(customers);
    }

    @Operation(summary = "Delete a customer")
    @DeleteMapping(value="/delete/{id}", produces = "application/json")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        customerService.deleteCustomerById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update a customer")
    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<Void> updateCustomer(@PathVariable Integer id, @RequestBody Customer customer){
        return customerService.findCustomerById(id)
                .map(storedCustomer -> {
                    storedCustomer.setAge(customer.getAge());
                    customerService.save(storedCustomer);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}
