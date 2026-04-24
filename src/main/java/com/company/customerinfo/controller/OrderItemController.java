package com.company.customerinfo.controller;

import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/orderitem")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @Operation(summary = "Create a new order item record")
    @ApiResponses(value = {
            @ApiResponse( responseCode = "201", description = "order item created", content = { @Content(mediaType = "application/json")} ),
            @ApiResponse(responseCode = "404", description = "Bad request") })
    @PostMapping("/save")
    public ResponseEntity<OrderItem> save(@RequestBody OrderItem orderItem) {
        OrderItem response = orderItemService.save(orderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View a list of order items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    })
    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity<List<OrderItem>> list(){
        List<OrderItem> orderItems = orderItemService.findAll();
        return ResponseEntity.ok(orderItems);
    }

    @Operation(summary = "Delete an order item")
    @DeleteMapping(value="/delete/{id}", produces = "application/json")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        orderItemService.deleteOrderItemById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update an order item")
    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<Void> updateOrderItem(@PathVariable Integer id, @RequestBody OrderItem orderItem){
        return orderItemService.findById(id)
                .map(storedOrderItem -> {
                    storedOrderItem.setQuantity(orderItem.getQuantity());
                    orderItemService.save(storedOrderItem);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}