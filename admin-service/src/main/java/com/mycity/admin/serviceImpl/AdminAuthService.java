package com.mycity.admin.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mycity.admin.entity.Admin;
import com.mycity.admin.exception.AdminNotFoundException;
import com.mycity.admin.exception.InvalidCredentialsException;
import com.mycity.admin.repository.AdminAuthRepository;
import com.mycity.admin.service.AdminAuthInterface;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuthService implements AdminAuthInterface {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminAuthRepository adminAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Admin loginUser(String email, String password) {
        log.info("Attempting to log in admin with email: {}", email);

        Admin admin = adminAuthRepository.findByEmail(email);
        if (admin == null) {
            log.warn("Admin not found with email: {}", email);
            throw new AdminNotFoundException("Admin not found for email: " + email);
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("Invalid password attempt for email: {}", email);
            throw new InvalidCredentialsException("Invalid password for email: " + email);
        }

        log.info("Admin login successful for email: {}", email);
        return admin;
    }
}

