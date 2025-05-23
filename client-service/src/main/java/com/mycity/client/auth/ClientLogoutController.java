package com.mycity.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
public class ClientLogoutController {

    private static final Logger logger = LoggerFactory.getLogger(ClientLogoutController.class);

    private static final String APIGATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String LOGOUT_PATH = "/auth/logout";

    private final WebClient.Builder webClientBuilder;

    public ClientLogoutController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout() {
        logger.info("🔐 Logout requested - forwarding to API Gateway");

        return webClientBuilder.build()
                .post()
                .uri("lb://" + APIGATEWAY_SERVICE_NAME + LOGOUT_PATH)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(response -> logger.info("✅ Logout successful with status: {}", response.getStatusCode()))
                .doOnError(error -> logger.error("❌ Error during logout: {}", error.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("{\"error\":\"Logout failed: " + e.getMessage() + "\"}")));
    }
}
