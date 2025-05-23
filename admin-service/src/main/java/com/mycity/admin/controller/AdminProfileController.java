package com.mycity.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.admin.service.AdminProfileService;
import com.mycity.shared.admindto.AdminProfileResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminProfileController {

    private static final Logger log = LoggerFactory.getLogger(AdminProfileController.class);

    private final AdminProfileService adminProfile;
    private final WebClient.Builder webClientBuilder;

    @PostMapping("/profile/upload-picture")
    public ResponseEntity<String> uploadPictureToMediaService(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("image") MultipartFile imageFile) throws Exception {

        log.info("Admin with ID {} is uploading a profile picture", userId);

        try {
            ByteArrayResource imageResource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentDisposition(ContentDisposition.builder("form-data")
                    .name("image")
                    .filename(imageFile.getOriginalFilename())
                    .build());
            imageHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(imageResource, imageHeaders);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imagePart);
            body.add("userId", userId);

            String response = webClientBuilder.build()
                    .post()
                    .uri("lb://media-service/media/admin/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Admin profile picture uploaded successfully for ID {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload profile picture for admin ID {}: {}", userId, e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @GetMapping("/profile-picture")
    public Mono<ResponseEntity<String>> getProfilePictureUrl(@RequestHeader("X-User-Id") String userId) {
        log.info("Fetching profile picture URL for admin ID {}", userId);

        return webClientBuilder.build()
                .get()
                .uri("lb://media-service/media/admin/get-image/" + userId)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(response -> log.info("Profile picture URL fetched for admin ID {}", userId))
                .doOnError(error -> log.error("Failed to fetch profile picture URL for admin ID {}: {}", userId, error.getMessage()));
    }

    @GetMapping("/profile")
    public ResponseEntity<AdminProfileResponse> getAdminProfile(@RequestHeader("X-User-Id") String adminId) {
        log.info("Retrieving admin profile for ID {}", adminId);

        AdminProfileResponse admin = adminProfile.getAdminById(adminId);

        log.info("Admin profile retrieved successfully for ID {}", adminId);
        return ResponseEntity.ok(admin);
    }
}
