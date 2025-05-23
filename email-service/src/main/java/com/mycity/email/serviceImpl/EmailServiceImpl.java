package com.mycity.email.serviceImpl;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.mycity.email.Exception.ExpiredOtpException;
import com.mycity.email.Exception.InvalidOtpException;
import com.mycity.email.Exception.OtpGenerationException;
import com.mycity.email.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final int OTP_LENGTH = 4;
    private static final long OTP_EXPIRY_MINUTES = 10;

    @Override
    public void generateAndSendOTP(String recipientEmail) {
        try {
            String otp = generateRandomOTP(OTP_LENGTH);
            log.info("Generated OTP: {} for email: {}", otp, recipientEmail);

            String redisKey = OTP_PREFIX + recipientEmail;
            redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            log.info("Stored OTP '{}' in Redis with key: {}, TTL: {} minutes",
                    otp, redisKey, OTP_EXPIRY_MINUTES);

            sendOTPEmail(recipientEmail, otp);
            log.info("OTP email sent to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Error generating/sending OTP to {}: {}", recipientEmail, e.getMessage(), e);
            throw new OtpGenerationException("Failed to generate/send OTP. Please try again later.");
        }
    }

    private void sendOTPEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP for MyCity Registration");
        message.setText("Your OTP is: " + otp + ". This OTP will expire in " + OTP_EXPIRY_MINUTES + " minutes.");
        mailSender.send(message);
    }

    private String generateRandomOTP(int length) {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Override
    public boolean verifyOTP(String email, String otp) {
        String redisKey = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);
        Long ttl = redisTemplate.getExpire(redisKey);

        log.info("Verifying OTP for email: {}", email);
        log.debug("Stored OTP: {}, TTL: {}", storedOtp, ttl);

        if (storedOtp == null) {
            log.warn("OTP not found or expired for email: {}", email);
            throw new ExpiredOtpException("OTP expired or not found.");
        }

        if (ttl == null || ttl <= 0) {
            redisTemplate.delete(redisKey);
            log.warn("OTP TTL expired for email: {}", email);
            throw new ExpiredOtpException("OTP expired.");
        }

        if (!storedOtp.equals(otp)) {
            log.warn("OTP mismatch for email: {}. Provided: {}, Stored: {}", email, otp, storedOtp);
            throw new InvalidOtpException("OTP does not match.");
        }

        redisTemplate.delete(redisKey);
        log.info("OTP verified and deleted for email: {}", email);
        return true;
    }
}
