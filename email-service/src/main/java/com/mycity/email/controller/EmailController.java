package com.mycity.email.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mycity.email.service.EmailService;
import com.mycity.shared.emaildto.RequestOtpDTO;
import com.mycity.shared.emaildto.VerifyOtpDTO;
import com.mycity.shared.responsedto.OTPResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/auth/generateotp")
    public ResponseEntity<String> requestOtp(@RequestBody RequestOtpDTO otpRequest) {
        String email = otpRequest.getEmail();
        log.info("Received OTP generation request for email: {}", email);

        emailService.generateAndSendOTP(email);
        log.info("OTP sent successfully to email: {}", email);
        return ResponseEntity.ok("OTP sent to email successfully.");
    }

    @PostMapping("/auth/verifyotp")
    public ResponseEntity<OTPResponse> verifyOtp(@RequestBody VerifyOtpDTO verificationRequest) {
        String email = verificationRequest.getEmail();
        String otp = verificationRequest.getOtp();

        log.info("Received OTP verification request for email: {}", email);

        boolean isValid = emailService.verifyOTP(email, otp);
        if (isValid) {
            log.info("OTP verification succeeded for email: {}", email);
            return ResponseEntity.ok(new OTPResponse("OTP verified successfully", true));
        } else {
            log.warn("OTP verification failed for email: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new OTPResponse("Invalid or expired OTP.", false));
        }
    }
}
