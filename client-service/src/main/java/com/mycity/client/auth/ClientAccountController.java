package com.mycity.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.UserPhoneUpdateException;
import com.mycity.shared.updatedto.UpdatePhoneRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientAccountController {

    private final WebClient.Builder webClientBuilder;
    private final CookieTokenExtractor cookieTokenExtractor;

    private static final Logger logger = LoggerFactory.getLogger(ClientAccountController.class);
    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String UPDATE_PHONE_PATH = "/user/account/updatephone";

    @PatchMapping("/account/updatephone")
    public Mono<ResponseEntity<String>> updateUserPhone(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestBody UpdatePhoneRequest request) {

        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);

        if (token == null || token.isEmpty()) {
            logger.error("❌ Missing Authorization token from cookie");
            return Mono.just(ResponseEntity.status(400).body("Authorization token is missing"));
        }

        logger.info("📩 Forwarding phone update to API Gateway with token: {}", token);
        logger.info("📞 Requested new phone number: {}", request.getPhoneNumber());

        return webClientBuilder.build()
                .patch()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + UPDATE_PHONE_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("❌ Phone update failed with response: {}", body);
                                    return Mono.error(new UserPhoneUpdateException("Failed to update phone number: " + body));
                                }))
                .toEntity(String.class)
                .doOnSuccess(res -> logger.info("✅ Phone number update successful with status: {}", res.getStatusCode()))
                .doOnError(err -> logger.error("❌ Exception occurred during phone update: {}", err.getMessage()));
    }
}
