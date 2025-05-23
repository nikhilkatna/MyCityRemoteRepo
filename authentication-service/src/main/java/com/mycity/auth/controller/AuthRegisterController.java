package com.mycity.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.mycity.auth.exception.MerchantRegistrationException;
import com.mycity.auth.exception.UserRegistrationException;
import com.mycity.shared.merchantdto.MerchantRegRequest;
import com.mycity.shared.userdto.UserRegRequest;

@RestController
@RequestMapping("/auth")
public class AuthRegisterController {

    private static final Logger log = LoggerFactory.getLogger(AuthRegisterController.class);

    @Autowired
    private WebClient webClient;  // Inject WebClient instance directly.

    private static final String USER_SERVICE = "lb://USER-SERVICE";
    private static final String MERCHANT_SERVICE = "lb://MERCHANT-SERVICE";

    @PostMapping("/register/user")
    public ResponseEntity<?> registerUser(@RequestBody UserRegRequest user) {
        log.info("Received user registration request for email: {}", user.getEmail());
        try {
            String response = webClient
                .post()
                .uri(USER_SERVICE + "/user/auth/internal/register")
                .bodyValue(user)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("User registration successful for email: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (WebClientResponseException ex) {
            log.error("User service returned error for email {}: status={}, body={}", user.getEmail(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new UserRegistrationException("User registration failed: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during user registration for email {}: {}", user.getEmail(), ex.getMessage(), ex);
            throw new UserRegistrationException("User registration failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/register/merchant")
    public ResponseEntity<?> registerMerchant(@RequestBody MerchantRegRequest merchant) {
        log.info("Received merchant registration request for email: {}", merchant.getEmail());
        try {
            String response = webClient
                .post()
                .uri(MERCHANT_SERVICE + "/merchant/auth/internal/register")
                .bodyValue(merchant)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("Merchant registration successful for email: {}", merchant.getEmail());
            return ResponseEntity.ok(response);

        } catch (WebClientResponseException ex) {
            log.error("Merchant service returned error for email {}: status={}, body={}", merchant.getEmail(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new MerchantRegistrationException("Merchant registration failed: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during merchant registration for email {}: {}", merchant.getEmail(), ex.getMessage(), ex);
            throw new MerchantRegistrationException("Merchant registration failed: " + ex.getMessage(), ex);
        }
    }
}
