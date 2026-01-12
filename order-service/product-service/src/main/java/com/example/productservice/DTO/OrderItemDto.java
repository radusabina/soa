package com.example.productservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    @Getter
    @Setter
    private Long productId;

    @Getter
    @Setter
    private int quantity;

}

