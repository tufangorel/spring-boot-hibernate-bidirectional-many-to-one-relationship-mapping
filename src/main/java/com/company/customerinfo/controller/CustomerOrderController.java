package com.company.customerinfo.controller;

import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.service.CustomerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;


@RestController
@RequestMapping("/customerorder")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @Operation(summary = "Create a new customer order record")
    @ApiResponses(value = {
            @ApiResponse( responseCode = "201", description = "customer order created", content = { @Content(mediaType = "application/json")} ),
            @ApiResponse(responseCode = "404", description = "Bad request") })
    @PostMapping("/save")
    public ResponseEntity<CustomerOrder> save(@RequestBody CustomerOrder customerOrder) {
        CustomerOrder response = customerOrderService.save(customerOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View a list of customer orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    })
    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity<Iterable<CustomerOrder>> list(){
        Iterable<CustomerOrder> customerOrders = customerOrderService.findAll();
        return ResponseEntity.ok(customerOrders);
    }

    @Operation(summary = "Delete a customer order")
    @DeleteMapping(value="/delete/{id}", produces = "application/json")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        customerOrderService.deleteCustomerOrderById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update a customer order")
    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<Void> updateCustomer(@PathVariable Integer id, @RequestBody CustomerOrder customerOrder){
        return customerOrderService.findById(id)
                .map(storedCustomerOrder -> {
                    storedCustomerOrder.setOrderDate(LocalDateTime.now());
                    customerOrderService.save(storedCustomerOrder);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}