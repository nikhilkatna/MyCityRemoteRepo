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
public class UserEmailController {

    private final EmailService userEmailService;

    @PostMapping("/user/generateotp")
    public ResponseEntity<String> generateOTP(@Valid @RequestBody RequestOtpDTO request) {
        String email = request.getEmail();
        log.info("Received OTP generation request for user email: {}", email);

        userEmailService.generateAndSendOTP(email);

        log.info("OTP successfully sent to user email: {}", email);
        return ResponseEntity.ok("OTP has been sent to your email.");
    }

    // Optional: If you later want to enable OTP verification again
    /*
    @PostMapping("/user/verifyotp")
    public ResponseEntity<OTPResponse> verifyOTP(@RequestBody VerifyOtpDTO request) {
        log.info("Received OTP verification request for user email: {}", request.getEmail());
        boolean isVerified = userEmailService.verifyOTP(request.getEmail(), request.getOtp());
        if (isVerified) {
            log.info("OTP verification succeeded for user email: {}", request.getEmail());
            return ResponseEntity.ok(new OTPResponse("OTP verified successfully", true));
        } else {
            log.warn("OTP verification failed for user email: {}", request.getEmail());
            return ResponseEntity.badRequest().body(new OTPResponse("Invalid or expired OTP", false));
        }
    }
    */
}
