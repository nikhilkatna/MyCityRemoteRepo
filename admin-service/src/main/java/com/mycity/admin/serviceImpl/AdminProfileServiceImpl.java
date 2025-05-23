package com.mycity.admin.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.admin.entity.Admin;
import com.mycity.admin.exception.AdminNotFoundException;
import com.mycity.admin.repository.AdminAuthRepository;
import com.mycity.admin.service.AdminProfileService;
import com.mycity.shared.admindto.AdminProfileResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProfileServiceImpl implements AdminProfileService {

    private static final Logger log = LoggerFactory.getLogger(AdminProfileServiceImpl.class);

    private final AdminAuthRepository adminAuthRepository;

    @Override
    public AdminProfileResponse getAdminById(String adminIdStr) {
        log.info("Request received to fetch admin profile with ID: {}", adminIdStr);

        if (!adminIdStr.matches("\\d+")) {
            log.error("Invalid admin ID format: {}", adminIdStr);
            throw new IllegalArgumentException("Invalid Admin ID format. Must be a number.");
        }

        Long adminId = Long.parseLong(adminIdStr);

        Admin admin = adminAuthRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.warn("Admin not found with ID: {}", adminId);
                    return new AdminNotFoundException("Admin not found with ID: " + adminId);
                });

        log.info("Admin found with ID: {}", adminId);

        return new AdminProfileResponse(
                admin.getId(),
                admin.getEmail(),
                admin.getRole(),
                admin.getUsername()
        );
    }

    @Override
    public void uploadUserImage(MultipartFile file, String userId) {
        log.warn("uploadUserImage() not implemented for admin ID: {}", userId);
        throw new UnsupportedOperationException("This method is not yet implemented");
    }

    @Override
    public String getImageUrlByUserId(String userId) {
        log.warn("getImageUrlByUserId() not implemented for admin ID: {}", userId);
        throw new UnsupportedOperationException("This method is not yet implemented");
    }
}

