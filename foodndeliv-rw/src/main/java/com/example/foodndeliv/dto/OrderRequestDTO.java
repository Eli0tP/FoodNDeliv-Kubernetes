package com.example.foodndeliv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class OrderRequestDTO {

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderLineDTO> items;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public List<OrderLineDTO> getItems() { return items; }
    public void setItems(List<OrderLineDTO> items) { this.items = items; }
}