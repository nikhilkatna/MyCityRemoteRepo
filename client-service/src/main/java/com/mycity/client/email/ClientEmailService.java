package com.mycity.client.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.shared.emaildto.RequestOtpDTO;
import com.mycity.shared.emaildto.VerifyOtpDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/email")
public class ClientEmailService {

    private static final Logger log = LoggerFactory.getLogger(ClientEmailService.class);

    private final WebClient.Builder webClientBuilder;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String OTP_REQUEST_PATH = "/auth/email/send";
    private static final String OTP_VERIFY_PATH = "/auth/otp/verifyotp";

    // Constructor injection to initialize the WebClient.Builder
    public ClientEmailService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostMapping("/request-otp")
    public Mono<String> requestOTP(@RequestBody RequestOtpDTO request) {
        log.info("[requestOTP] Received OTP request with body: {}", request);

        String fullUri = "lb://" + API_GATEWAY_SERVICE_NAME + OTP_REQUEST_PATH;
        log.debug("[requestOTP] Target URI: {}", fullUri);

        return createWebClient()
                .post()
                .uri(fullUri)
                .body(Mono.just(request), RequestOtpDTO.class)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.warn("[requestOTP] Error Status Detected: {}", clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.warn("[requestOTP] Error Response Body: {}", errorBody);
                        return Mono.error(new RuntimeException(
                                "OTP request failed: " + clientResponse.statusCode() + " - " + errorBody));
                    });
                })
                .bodyToMono(String.class)
                .doOnNext(responseBody -> log.info("[requestOTP] Successful Response Body: {}", responseBody))
                .doOnError(error -> log.error("[requestOTP] Exception during WebClient call: {}", error.getMessage(), error))
                .doOnTerminate(() -> log.debug("[requestOTP] WebClient request completed (success or fail)"))
                .onErrorResume(e -> {
                    log.error("[requestOTP] Returning fallback error: {}", e.getMessage());
                    return Mono.just("OTP request failed: " + e.getMessage());
                });
    }

    @PostMapping("/verify-otp")
    public Mono<String> verifyOTP(@RequestBody VerifyOtpDTO request) {
        log.info("[verifyOTP] Received OTP verification request with body: {}", request);

        String fullUri = "lb://" + API_GATEWAY_SERVICE_NAME + OTP_VERIFY_PATH;
        log.debug("[verifyOTP] Target URI: {}", fullUri);

        return createWebClient()
                .post()
                .uri(fullUri)
                .body(Mono.just(request), VerifyOtpDTO.class)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> 
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.warn("[verifyOTP] Error Status Detected: {} - {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new RuntimeException(
                                "OTP verification failed: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(String.class)
                .doOnNext(responseBody -> log.info("[verifyOTP] Successful Response Body: {}", responseBody))
                .doOnError(error -> log.error("[verifyOTP] Exception during WebClient call: {}", error.getMessage(), error))
                .onErrorResume(e -> {
                    log.error("[verifyOTP] Returning fallback error: {}", e.getMessage());
                    return Mono.just("OTP verification failed: " + e.getMessage());
                });
    }

    // Method to create a WebClient instance
    private WebClient createWebClient() {
        return webClientBuilder.baseUrl(API_GATEWAY_SERVICE_NAME).build();
    }
}
