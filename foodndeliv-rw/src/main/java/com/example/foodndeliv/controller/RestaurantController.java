package com.example.foodndeliv.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String createRestaurant() {
        return "Restaurant created successfully by Admin.";
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String deactivateRestaurant(@PathVariable Long id) {
        boolean hasPendingOrders = checkPendingOrdersServerSide(id); 
        if (hasPendingOrders) {
            throw new IllegalStateException("Domain Invariant Violation: Cannot close a restaurant with pending orders.");
        }
        return "Restaurant deactivated successfully.";
    }

    private boolean checkPendingOrdersServerSide(Long restaurantId) {
        return false; 
    }
}