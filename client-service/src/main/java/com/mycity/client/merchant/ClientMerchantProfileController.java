package com.mycity.client.merchant;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.MissingTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientMerchantProfileController {

    private final WebClient.Builder webClientBuilder;
    private final CookieTokenExtractor cookieTokenExtractor;

    private static final Logger logger = LoggerFactory.getLogger(ClientMerchantProfileController.class);

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String MERCHANT_PROFILE_PATH_ON_GATEWAY = "/merchant/profile";

    @GetMapping("/profile/merchant")
    public Mono<ResponseEntity<String>> getMerchantProfile(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);

        if (token == null || token.isEmpty()) {
            logger.error("❌ Missing Authorization token from cookie");
            // Throw custom exception instead of returning directly
            throw new MissingTokenException("Authorization token is missing");
        }

        logger.info("📩 Forwarding Authorization token to API Gateway: {}", token);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + MERCHANT_PROFILE_PATH_ON_GATEWAY)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class)
                .map(response -> {
                    logger.info("✅ Received response from API Gateway with status: {}", response.getStatusCode());
                    return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
                })
                .onErrorResume(ex -> {
                    logger.error("❌ Error forwarding to API Gateway: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(500).body("Something went wrong...."));
                });
    }
}
