package com.example.orderservice.DTO;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long productId;

    private int quantity;

    private int price;

}

