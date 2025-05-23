package com.mycity.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.mycity.admin.entity.Admin;
import com.mycity.admin.service.AdminAuthInterface;
import com.mycity.shared.admindto.AdminDetailsResponse;
import com.mycity.shared.admindto.AdminLoginRequest;
import com.mycity.admin.exception.InvalidCredentialsException;

@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    private static final Logger logger = LoggerFactory.getLogger(AdminLoginController.class);

    @Autowired
    private AdminAuthInterface adminAuthService;

    @PostMapping("/auth/internal/login")
    public ResponseEntity<AdminDetailsResponse> validateAdmin(@RequestBody AdminLoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());

        Admin admin = adminAuthService.loginUser(request.getEmail(), request.getPassword());
        if (admin == null) {
            logger.warn("Login failed for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        AdminDetailsResponse response = new AdminDetailsResponse(admin.getId(), admin.getEmail(), admin.getRole());
        logger.info("Login successful for adminId: {}", admin.getId());
        return ResponseEntity.ok(response);
    }
}
