package com.example.foodndeliv.controller;

import com.example.foodndeliv.service.*;
import com.example.foodndeliv.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ctrl/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponseDTO createCustomer(@Valid @RequestBody CustomerRequestDTO customerRequestDTO) {
        System.out.println("createCustomer called");
        return customerService.createCustomer(customerRequestDTO);
    }
    
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponseDTO updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerUpdateDTO customerUpdateDTO) {
        System.out.println("updateCustomer called");
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String tokenUserId = jwt.getClaimAsString("preferred_username"); 
            String expectedUserId = "customer_" + id;

            if (tokenUserId != null && !tokenUserId.equals(expectedUserId)) {
                throw new AccessDeniedException("Access Denied: You are not the owner of this account.");
            }
        }

        return customerService.updateCustomer(id, customerUpdateDTO);
    }
}