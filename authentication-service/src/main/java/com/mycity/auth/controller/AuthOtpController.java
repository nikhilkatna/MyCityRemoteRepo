package com.mycity.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.mycity.auth.exception.EmailServiceException;
import com.mycity.auth.exception.OtpServiceException;
import com.mycity.shared.emaildto.RequestOtpDTO;
import com.mycity.shared.emaildto.VerifyOtpDTO;
import com.mycity.shared.responsedto.OTPResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthOtpController {

    private static final Logger log = LoggerFactory.getLogger(AuthOtpController.class);

    private final WebClient.Builder webClientBuilder;

    private static final String EMAIL_SERVICE_URL = "lb://EMAIL-SERVICE";
    private static final String OTP_SERVICE_URL = "lb://OTP-SERVICE";

    @PostMapping("/email/send")
    public ResponseEntity<?> sendOtp(@RequestBody RequestOtpDTO request) {
        log.info("Received OTP send request for email: {}", request.getEmail());
        try {
            String response = webClientBuilder.build()
                .post()
                .uri(EMAIL_SERVICE_URL + "/email/user/generateotp")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("OTP send successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            log.error("Email service error for email {}: status={}, body={}", request.getEmail(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailServiceException("Email service error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to send OTP for email {}: {}", request.getEmail(), e.getMessage(), e);
            throw new EmailServiceException("Failed to send OTP: " + e.getMessage(), e);
        }
    }

    @PostMapping("/otp/verifyotp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpDTO request) {
        log.info("Received OTP verification request for email: {}", request.getEmail());
        try {
            OTPResponse response = webClientBuilder.build()
                .post()
                .uri(OTP_SERVICE_URL + "/otp/auth/verifyotp")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OTPResponse.class)
                .block();

            if (response.isOtpVerified()) {
                log.info("OTP verified successfully for email: {}", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                log.warn("OTP verification failed for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (WebClientResponseException e) {
            log.error("OTP service error for email {}: status={}, body={}", request.getEmail(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new OtpServiceException("OTP service error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to verify OTP for email {}: {}", request.getEmail(), e.getMessage(), e);
            throw new OtpServiceException("Failed to verify OTP: " + e.getMessage(), e);
        }
    }
}
