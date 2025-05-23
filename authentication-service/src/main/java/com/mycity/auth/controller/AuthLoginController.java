package com.mycity.auth.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.auth.config.JwtService;
import com.mycity.auth.exception.AuthenticationException;
import com.mycity.shared.admindto.AdminDetailsResponse;
import com.mycity.shared.admindto.AdminLoginRequest;
import com.mycity.shared.errordto.ErrorResponse;
import com.mycity.shared.merchantdto.MerchantDetailsResponse;
import com.mycity.shared.merchantdto.MerchantLoginRequest;
import com.mycity.shared.responsedto.LoginResponse;
import com.mycity.shared.userdto.UserLoginRequest;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthLoginController {

    private static final Logger log = LoggerFactory.getLogger(AuthLoginController.class);

    private final WebClient.Builder webClientBuilder;
    private final JwtService jwtService;

    private static final String USER_SERVICE = "lb://USER-SERVICE";
    private static final String ADMIN_SERVICE = "lb://ADMIN-SERVICE";
    private static final String MERCHANT_SERVICE = "lb://MERCHANT-SERVICE";

    @PostMapping("/login/user")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginRequest request) {
        log.info("Attempting user login for email: {}", request.getEmail());

        try {
            LoginResponse response = webClientBuilder.build()
                .post()
                .uri(USER_SERVICE + "/user/auth/internal/login")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    res -> res.bodyToMono(ErrorResponse.class)
                        .flatMap(error -> Mono.error(new AuthenticationException(error.getMessage())))
                )
                .bodyToMono(LoginResponse.class)
                .block();

            if (response != null && Boolean.TRUE.equals(response.getStatus())) {
                ResponseCookie jwtCookie = jwtService.generateJwtCookie(
                    response.getId(), request.getEmail(), response.getRole()
                );

                log.info("User login successful for email: {}", request.getEmail());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                            "message", "Login successful",
                            "role", response.getRole()
                        ));
            }

            log.warn("Invalid user credentials for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid user credentials");
        } catch (RuntimeException e) {
            log.error("User login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @PostMapping("/login/merchant")
    public ResponseEntity<?> loginMerchant(@RequestBody MerchantLoginRequest request) {
        log.info("Attempting merchant login for email: {}", request.getEmail());

        try {
            MerchantDetailsResponse response = webClientBuilder.build()
                .post()
                .uri(MERCHANT_SERVICE + "/merchant/auth/internal/login")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    res -> res.bodyToMono(ErrorResponse.class)
                        .flatMap(error -> Mono.error(new AuthenticationException(error.getMessage())))
                )
                .bodyToMono(MerchantDetailsResponse.class)
                .block();

            if (response != null) {
                ResponseCookie jwtCookie = jwtService.generateJwtCookie(
                    response.getId(), request.getEmail(), response.getRole()
                );

                log.info("Merchant login successful for email: {}", request.getEmail());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                            "message", "Login successful",
                            "role", response.getRole()
                        ));
            }

            log.warn("Invalid merchant credentials for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid merchant credentials");
        } catch (RuntimeException e) {
            log.error("Merchant login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AdminLoginRequest request) {
        log.info("Attempting admin login for email: {}", request.getEmail());

        try {
            AdminDetailsResponse response = webClientBuilder.build()
                .post()
                .uri(ADMIN_SERVICE + "/admin/auth/internal/login")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    res -> res.bodyToMono(ErrorResponse.class)
                        .flatMap(error -> Mono.error(new AuthenticationException(error.getMessage())))
                )
                .bodyToMono(AdminDetailsResponse.class)
                .block();

            if (response != null) {
                ResponseCookie jwtCookie = jwtService.generateJwtCookie(
                    response.getId(), request.getEmail(), response.getRole()
                );

                log.info("Admin login successful for email: {}", request.getEmail());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                            "message", "Login successful",
                            "role", response.getRole()
                        ));
            }

            log.warn("Invalid admin credentials for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid admin credentials");
        } catch (RuntimeException e) {
            log.error("Admin login failed for email {}: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }
}   