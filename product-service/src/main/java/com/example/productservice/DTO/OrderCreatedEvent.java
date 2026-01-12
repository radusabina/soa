package com.example.productservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    @Getter
    @Setter
    private Long orderId;
    @Getter
    @Setter
    private List<OrderItemDto> items;

}

