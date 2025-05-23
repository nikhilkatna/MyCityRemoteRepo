package com.mycity.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.auth.exception.UserNotFoundException;
import com.mycity.shared.emaildto.ForgotPasswordDTO;
import com.mycity.shared.emaildto.ResetPasswordRequest;
import com.mycity.shared.userdto.UserDetailsResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth/forgot-password")
@RequiredArgsConstructor
public class AuthForgotPasswordAuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthForgotPasswordAuthController.class);

    private final WebClient.Builder webClientBuilder;

    private static final String USER_SERVICE_NAME = "USER-SERVICE";
    private static final String USER_BY_EMAIL_PATH = "/users/details/by-email";
    private static final String RESET_PASSWORD_PATH = "/users/reset-password";

    @PostMapping("/initiate")
    public Mono<ResponseEntity<String>> initiateForgotPassword(@RequestBody ForgotPasswordDTO request) {
        log.info("Initiating forgot password process for email: {}", request.getEmail());

        WebClient userServiceClient = webClientBuilder.baseUrl("lb://" + USER_SERVICE_NAME).build();

        return userServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(USER_BY_EMAIL_PATH)
                        .queryParam("email", request.getEmail())
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.warn("4xx error from user-service for email {}: {}", request.getEmail(), response.statusCode());
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new UserNotFoundException("User with email " + request.getEmail() + " not found."));
                    }
                    return response.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Client error from user-service: {}", errorBody);
                        return Mono.error(new RuntimeException("Client error: " + errorBody));
                    });
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("5xx error from user-service for email {}: {}", request.getEmail(), response.statusCode());
                    return response.bodyToMono(String.class).flatMap(errorBody ->
                            Mono.error(new RuntimeException("Server error from user-service: " + errorBody)));
                })
                .bodyToMono(UserDetailsResponse.class)
                .map(userDetail -> {
                    log.info("User found for email {}. Proceeding with OTP flow.", request.getEmail());
                    return ResponseEntity.ok("User found. Proceed with OTP.");
                })
                .onErrorResume(RuntimeException.class, e -> {
                    log.error("Error during forgot password initiation: {}", e.getMessage(), e);
                    if (e instanceof UserNotFoundException) {
                        return Mono.just(ResponseEntity.badRequest().body(e.getMessage()));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error checking user existence: " + e.getMessage()));
                });
    }

    @PostMapping("/reset")
    public Mono<ResponseEntity<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Resetting password for user with Mail Id: {}", request.getEmail());

        WebClient userServiceClient = webClientBuilder.baseUrl("lb://" + USER_SERVICE_NAME).build();

        return userServiceClient.post()
                .uri(RESET_PASSWORD_PATH)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.warn("Client error during password reset: {}", errorBody);
                            return Mono.error(new RuntimeException("Client error: " + errorBody));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Server error during password reset: {}", errorBody);
                            return Mono.error(new RuntimeException("Server error: " + errorBody));
                        }))
                .bodyToMono(String.class)
                .map(body -> {
                    log.info("Password reset successful for user with Mail ID: {}", request.getEmail());
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(e -> {
                    log.error("Password reset failed: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Password reset failed: " + e.getMessage()));
                });
    } 
    
    
    
 }