package com.example.foodndeliv.service;

import com.example.foodndeliv.dto.OrderRequestDTO;
import com.example.foodndeliv.dto.OrderLineDTO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

@Service
public class OrderService {

    public double calculateServerSideTotal(OrderRequestDTO orderRequest) {
        double total = 0.0;
        if (orderRequest.getItems() != null) {
            for (OrderLineDTO line : orderRequest.getItems()) {
                if (line.getPrice() != null && line.getQuantity() != null) {
                    total += line.getPrice() * line.getQuantity();
                }
            }
        }
        return total;
    }

    public void validateAccountOwner(Long requestedCustomerId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String tokenUserId = jwt.getClaimAsString("preferred_username"); 
            String expectedUserId = "customer_" + requestedCustomerId;

            if (tokenUserId != null && !tokenUserId.equals(expectedUserId)) {
                throw new AccessDeniedException("Access Denied: You are not the owner of this account.");
            }
        }
    }

    public void deactivateCustomer(Long customerId, boolean hasPendingOrders) {
        if (hasPendingOrders) {
            throw new IllegalStateException("Domain Invariant Violation: Cannot deactivate a customer with pending orders.");
        }
    }
}