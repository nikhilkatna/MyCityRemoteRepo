package com.mycity.auth.controller;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycity.auth.config.JwtService;
import com.mycity.auth.exception.LogoutException;

import lombok.RequiredArgsConstructor;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthLogoutController {

    private static final Logger log = LoggerFactory.getLogger(AuthLogoutController.class);

    private final JwtService jwtService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        log.info("Logout request received");

        try {
            // Remove the JWT token cookie by setting an empty cookie with no value
            Cookie emptyCookie = new Cookie("token", "");
            emptyCookie.setPath("/");
            emptyCookie.setHttpOnly(true);
            emptyCookie.setMaxAge(0);  // expire immediately
            response.addCookie(emptyCookie);

            // Also set the cleared JWT cookie from JwtService (if it has special flags etc)
            response.addHeader("Set-Cookie", jwtService.getCleanJwtCookie().toString());

            log.info("User logged out successfully, JWT cookie cleared");
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);
            throw new LogoutException("Logout failed: " + e.getMessage());
        }
    }

}
