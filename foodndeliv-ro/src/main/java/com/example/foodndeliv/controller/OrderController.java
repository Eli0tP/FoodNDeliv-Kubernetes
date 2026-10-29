package com.example.foodndeliv.controller;

import com.example.foodndeliv.service.*;
import com.example.foodndeliv.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ctrl/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseDTO> getAllOrders() {
        System.out.println("getAllOrders called from Read-Only replica");
        return orderService.getAllOrders();
    }
}