package com.mycity.email.controller;

import com.mycity.email.service.EmailService;
import com.mycity.shared.emaildto.RequestOtpDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Slf4j
public class MerchantEmailController {

    private final EmailService merchantEmailService;

    @PostMapping("/merchant/generateotp")
    public ResponseEntity<String> generateOTP(@Valid @RequestBody RequestOtpDTO request) {
        String email = request.getEmail();
        log.info("Received OTP generation request for merchant email: {}", email);

        merchantEmailService.generateAndSendOTP(email);

        log.info("OTP successfully sent to merchant email: {}", email);
        return ResponseEntity.ok("OTP has been sent to your email.");
    }
}
