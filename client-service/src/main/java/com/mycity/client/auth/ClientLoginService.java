package com.mycity.client.auth;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycity.client.exception.LoginException;
import com.mycity.shared.admindto.AdminLoginRequest;
import com.mycity.shared.merchantdto.MerchantLoginRequest;
import com.mycity.shared.userdto.UserLoginRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
@Slf4j
public class ClientLoginService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";

    @PostMapping("/login/user")
    public Mono<ResponseEntity<String>> loginUser(@RequestBody UserLoginRequest request) {
        log.info("🔐 Attempting user login for email: {}", request.getEmail());
        return forwardLogin(request, "/auth/login/user");
    }

    @PostMapping("/login/merchant")
    public Mono<ResponseEntity<String>> loginMerchant(@RequestBody MerchantLoginRequest request) {
        log.info("🔐 Attempting merchant login for email: {}", request.getEmail());
        return forwardLogin(request, "/auth/login/merchant");
    }

    @PostMapping("/login/admin")
    public Mono<ResponseEntity<String>> loginAdmin(@RequestBody AdminLoginRequest request) {
        log.info("🔐 Attempting admin login for Maid Id: {}", request.getEmail());
        return forwardLogin(request, "/auth/login/admin");
    }

    private <T> Mono<ResponseEntity<String>> forwardLogin(T request, String path) {
        String uri = "lb://" + API_GATEWAY_SERVICE_NAME + path;

        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    List<String> cookieHeaders = response.headers().header(HttpHeaders.SET_COOKIE);
                    Mono<String> bodyMono = response.bodyToMono(String.class);

                    return bodyMono.map(body -> {
                        log.info("✅ Login success via {} with response status: {}", path, response.statusCode());
                        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.statusCode());

                        for (String cookie : cookieHeaders) {
                            builder.header(HttpHeaders.SET_COOKIE, cookie);
                        }

                        return builder.body(body);
                    });
                })
                .doOnError(e -> log.error("❌ Login failed for path {}: {}", path, e.getMessage()))
                .onErrorMap(e -> new LoginException("Login request to " + path + " failed: " + e.getMessage()));
    }
}
