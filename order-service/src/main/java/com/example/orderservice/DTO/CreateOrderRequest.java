package com.example.orderservice.DTO;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    private long userId;
    private List<OrderItemDto> items;
}