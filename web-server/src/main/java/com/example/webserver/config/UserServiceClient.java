package com.example.webserver.config;

import com.example.webserver.DTOs.LoginRequest;
import com.example.webserver.DTOs.RegisterRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://user-service:8081")
                                .build();
    }

    public String register(RegisterRequest request) {
        Mono<String> responseMono = webClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class);

        responseMono.map(token -> "User registered succesfully").subscribe();
        return "";
    }

    public Mono<String> login(LoginRequest request) {
        return webClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class);
    }
}
