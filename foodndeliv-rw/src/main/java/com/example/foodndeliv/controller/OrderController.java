package com.example.foodndeliv.controller;

import com.example.foodndeliv.dto.OrderRequestDTO;
import com.example.foodndeliv.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequestDTO orderRequest) {
        orderService.validateAccountOwner(orderRequest.getCustomerId());
        double calculatedTotal = orderService.calculateServerSideTotal(orderRequest);
        return ResponseEntity.ok("Order processed successfully. Server-verified total: EUR " + calculatedTotal);
    }
    }