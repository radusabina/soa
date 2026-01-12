package com.example.webserver.config;

import com.example.webserver.DTOs.AuthResponse;
import com.example.webserver.DTOs.LoginRequest;
import com.example.webserver.DTOs.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceClient userServiceClient;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return userServiceClient.register(request); // apelează user-service
    }

    @PostMapping("/login")
    public Mono<String> login(@RequestBody LoginRequest request) {
        return userServiceClient.login(request);
    }
}
