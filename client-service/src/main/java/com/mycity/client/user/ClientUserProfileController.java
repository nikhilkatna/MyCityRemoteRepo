package com.mycity.client.user;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.ClientException;
import com.mycity.shared.userdto.UserDTO;
import com.mycity.shared.userdto.UserLoginRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
@Slf4j
public class ClientUserProfileController {

    private final WebClient.Builder webClientBuilder;
    private final CookieTokenExtractor cookieTokenExtractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String USER_PROFILE_PATH_ON_GATEWAY = "/user/profile";
    private static final String USER_ID_FINDING_PATH = "/user/getuserId/{userName}";
    private static final String USER_UPDATING_PATH = "/user/updateuser/{userId}";
    private static final String USER_DELETING_PATH = "/user/deleteuser/{userId}";
    private static final String USER_PASSWORD_CHANGING_PATH = "/user/updatepassword";
    private static final String USER_PROFILE_PICTURE = "/user/profile/upload-picture"; 
    private static final String USER_GET_PROFILE_PICTURE = "/user/profile-picture";

    @PostMapping("/upload-picture")
    public Mono<ResponseEntity<String>> uploadProfilePicture(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam("image") MultipartFile imageFile) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("Uploading profile picture...");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imageFile.getResource())
                .filename(imageFile.getOriginalFilename())
                .contentType(MediaType.valueOf(imageFile.getContentType()));

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_PROFILE_PICTURE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(res -> log.info("✅ Profile picture uploaded successfully"))
                .doOnError(e -> log.error("❌ Failed to upload profile picture", e))
                .onErrorMap(e -> new ClientException("Error uploading profile picture", e));
    }

    @GetMapping("/profile-picture")
    public Mono<ResponseEntity<String>> getProfilePicture(@RequestHeader(HttpHeaders.COOKIE) String cookie) {
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("Fetching profile picture...");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_GET_PROFILE_PICTURE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(res -> log.info("✅ Profile picture fetched"))
                .doOnError(e -> log.error("❌ Failed to fetch profile picture", e))
                .onErrorMap(e -> new ClientException("Error fetching profile picture", e));
    }

    @GetMapping("/profile/user")
    public Mono<ResponseEntity<String>> getUserProfile(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        if (token == null || token.isEmpty()) {
            log.warn("❌ Missing Authorization token from cookie");
            return Mono.just(ResponseEntity.badRequest().body("Authorization token is missing"));
        }

        log.info("📩 Fetching user profile using token: {}", token);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_PROFILE_PATH_ON_GATEWAY)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class)
                .doOnSuccess(res -> log.info("✅ User profile fetched with status: {}", res.getStatusCode()))
                .doOnError(e -> log.error("❌ Failed to fetch user profile", e))
                .onErrorMap(e -> new ClientException("Error fetching user profile", e));
    }

    @GetMapping("/getid/{userName}")
    public Mono<String> getUserId(@PathVariable String userName,
                                  @RequestHeader(value = HttpHeaders.COOKIE) String cookie) {
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("🔍 Fetching user ID for username: {}", userName);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_ID_FINDING_PATH, userName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Error fetching user ID: {}", body);
                                    return Mono.error(new ClientException("Failed to fetch User ID: " + body));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(id -> log.info("✅ User ID fetched: {}", id))
                .doOnError(e -> log.error("❌ Exception fetching user ID", e));
    }

    @PutMapping("/updateuser/{userId}")
    public Mono<String> updateUser(@PathVariable String userId,
                                   @RequestBody UserDTO dto,
                                   @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("🛠️ Updating user with ID: {}", userId);

        return webClientBuilder.build()
                .put()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_UPDATING_PATH, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Error updating user: {}", body);
                                    return Mono.error(new ClientException("Failed to update user: " + body));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ User updated successfully"))
                .doOnError(e -> log.error("❌ Exception updating user", e));
    }

    @DeleteMapping("/deleteUser/{userId}")
    public Mono<String> deleteUserById(@PathVariable String userId,
                                       @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("🗑️ Deleting user with ID: {}", userId);

        return webClientBuilder.build()
                .delete()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_DELETING_PATH, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Error deleting user: {}", body);
                                    return Mono.error(new ClientException("Failed to delete user: " + body));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ User deleted successfully"))
                .doOnError(e -> log.error("❌ Exception deleting user", e));
    }

    @PatchMapping("/updatepassword")
    public Mono<String> updateUserPassword(@RequestBody UserLoginRequest request,
                                           @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.info("🔒 Updating user password");

        return webClientBuilder.build()
                .patch()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_PASSWORD_CHANGING_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Error updating password: {}", body);
                                    return Mono.error(new ClientException("Failed to update password: " + body));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ Password updated successfully"))
                .doOnError(e -> log.error("❌ Exception updating password", e));
    }
}
