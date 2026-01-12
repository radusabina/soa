package com.example.webserver.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
@Getter
public class AuthResponse {
    private String token;
}
