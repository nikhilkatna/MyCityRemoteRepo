package com.mycity.client.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.AdminProfileException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientAdminProfileController {

    private final WebClient.Builder webClientBuilder;
    private final CookieTokenExtractor cookieTokenExtractor;

    private static final Logger logger = LoggerFactory.getLogger(ClientAdminProfileController.class);

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String ADMIN_PROFILE_PATH_ON_GATEWAY = "/admin/profile";
    private static final String ADMIN_PROFILE_PICTURE = "/admin/profile/upload-picture";
    private static final String ADMIN_GET_PROFILE_PICTURE = "/admin/profile-picture";

    @GetMapping("/profile/admin")
    public Mono<ResponseEntity<String>> getAdminProfile(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);

        if (token == null || token.isEmpty()) {
            logger.error("❌ Missing Authorization token from cookie for admin profile");
            return Mono.just(ResponseEntity.status(400).body("Authorization token is missing"));
        }

        logger.info("📩 Requesting admin profile from API Gateway");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + ADMIN_PROFILE_PATH_ON_GATEWAY)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("❌ Error fetching admin profile: {}", body);
                                    return Mono.error(new AdminProfileException("Failed to fetch admin profile: " + body));
                                }))
                .toEntity(String.class)
                .doOnSuccess(response -> logger.info("✅ Successfully retrieved admin profile with status: {}", response.getStatusCode()))
                .doOnError(ex -> logger.error("❌ Failed to fetch admin profile: {}", ex.getMessage()));
    }

    @PostMapping("/admin/upload-picture")
    public Mono<ResponseEntity<String>> uploadProfilePicture(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam("image") MultipartFile imageFile) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);

        logger.info("📤 Admin attempting to upload profile picture: {}", imageFile.getOriginalFilename());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imageFile.getResource())
               .filename(imageFile.getOriginalFilename())
               .contentType(MediaType.valueOf(imageFile.getContentType()));

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + ADMIN_PROFILE_PICTURE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("❌ Failed to upload profile picture: {}", body);
                                    return Mono.error(new AdminProfileException("Failed to upload profile picture: " + body));
                                }))
                .toEntity(String.class)
                .doOnSuccess(res -> logger.info("✅ Profile picture uploaded successfully"))
                .doOnError(err -> logger.error("❌ Error uploading profile picture: {}", err.getMessage()));
    }

    @GetMapping("/admin/profile-picture")
    public Mono<ResponseEntity<String>> getProfilePicture(@RequestHeader(HttpHeaders.COOKIE) String cookie) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);

        logger.info("📥 Admin requesting to fetch profile picture");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + ADMIN_GET_PROFILE_PICTURE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("❌ Failed to fetch profile picture: {}", body);
                                    return Mono.error(new AdminProfileException("Failed to fetch profile picture: " + body));
                                }))
                .toEntity(String.class)
                .doOnSuccess(res -> logger.info("✅ Successfully fetched profile picture"))
                .doOnError(err -> logger.error("❌ Error fetching profile picture: {}", err.getMessage()));
    }
}
