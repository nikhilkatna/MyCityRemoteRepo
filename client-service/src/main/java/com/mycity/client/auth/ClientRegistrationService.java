package com.mycity.client.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycity.client.exception.CustomRegistrationException;
import com.mycity.shared.errordto.ErrorResponse;
import com.mycity.shared.merchantdto.MerchantRegRequest;
import com.mycity.shared.userdto.UserRegRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ClientRegistrationService.class);

    private final WebClient.Builder webClientBuilder;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/register/user")
    public Mono<String> registerUser(@RequestBody UserRegRequest request) {
        log.info("User registration request received for username/email: {}", request.getEmail());
        return forwardRegister(request, "/auth/register/user", UserRegRequest.class);
    }

    @PostMapping("/register/merchant")
    public Mono<String> registerMerchant(@RequestBody MerchantRegRequest request) {
        log.info("Merchant registration request received for username/email: {}", request.getEmail());
        return forwardRegister(request, "/auth/register/merchant", MerchantRegRequest.class);
    }

    private <T> Mono<String> forwardRegister(T request, String path, Class<T> typeclass) {
        return webClientBuilder.build()
            .post()
            .uri("lb://" + API_GATEWAY_SERVICE_NAME + path)
            .body(Mono.just(request), typeclass)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res -> 
                res.bodyToMono(String.class)
                   .flatMap(body -> {
                       try {
                           Map<String, Object> map = objectMapper.readValue(body, Map.class);
                           String message = (String) map.getOrDefault("message", "Unknown error");
                           log.error("Registration error received from API Gateway: {}", message);
                           ErrorResponse error = new ErrorResponse(message, 400);
                           return Mono.error(new CustomRegistrationException(error));
                       } catch (Exception e) {
                           log.error("Error parsing error response JSON: {}", e.getMessage());
                           return Mono.error(new CustomRegistrationException(
                               new ErrorResponse("Invalid error response format", 400)));
                       }
                   })
            )
            .bodyToMono(String.class)
            .doOnSuccess(res -> log.info("Registration successful"))
            .doOnError(err -> log.error("Registration failed: {}", err.getMessage()));
    }
}
