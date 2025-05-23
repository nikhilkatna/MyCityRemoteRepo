package com.mycity.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.exception.ForgotPasswordInitiationException;
import com.mycity.client.exception.PasswordResetException;
import com.mycity.shared.emaildto.ForgotPasswordDTO;
import com.mycity.shared.emaildto.ResetPasswordRequest;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/forgotpassword")
@AllArgsConstructor
public class ClientForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ClientForgotPasswordController.class);

    private final WebClient.Builder webClientBuilder;

    private static final String APIGATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String INITIATE_PATH = "/auth/forgot-password/initiate";
    private static final String RESET_PATH = "/auth/forgot-password/reset";

    @PostMapping("/initiate")
    public Mono<ResponseEntity<String>> initiateForgotPassword(@RequestBody ForgotPasswordDTO request) {
        String uri = "lb://" + APIGATEWAY_SERVICE_NAME + INITIATE_PATH;
        log.info("📩 Initiating forgot password for email: {}", request.getEmail());

        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ Forgot password initiation failed for {}: {}", request.getEmail(), errorBody);
                                    return Mono.error(new ForgotPasswordInitiationException("Forgot password initiation failed: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .map(ResponseEntity::ok)
                .doOnSuccess(res -> log.info("✅ Forgot password initiation successful for: {}", request.getEmail()))
                .doOnError(e -> log.error("❌ Error during forgot password initiation for {}: {}", request.getEmail(), e.getMessage()));
    }

    @PostMapping("/reset")
    public Mono<ResponseEntity<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        String uri = "lb://" + APIGATEWAY_SERVICE_NAME + RESET_PATH;
        log.info("🔐 Resetting password for email: {}", request.getEmail());

        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ Password reset failed for {}: {}", request.getEmail(), errorBody);
                                    return Mono.error(new PasswordResetException("Password reset failed: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .map(ResponseEntity::ok)
                .doOnSuccess(res -> log.info("✅ Password reset successful for: {}", request.getEmail()))
                .doOnError(e -> log.error("❌ Error during password reset for {}: {}", request.getEmail(), e.getMessage()));
    }
}

