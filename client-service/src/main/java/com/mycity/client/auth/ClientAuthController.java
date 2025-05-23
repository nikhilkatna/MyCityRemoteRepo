package com.mycity.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.exception.MerchantRegistrationException;
import com.mycity.client.exception.OtpVerificationException;
import com.mycity.client.exception.UserRegistrationException;
import com.mycity.shared.emaildto.RequestOtpDTO;
import com.mycity.shared.emaildto.VerifyOtpDTO;
import com.mycity.shared.merchantdto.MerchantRegRequest;
import com.mycity.shared.userdto.UserRegRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientAuthController {

    private final WebClient.Builder webClientBuilder;
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthController.class);
    private static final String API_GATEWAY_URL = "lb://API-GATEWAY";

    @PostMapping("/auth/startreg")
    public Mono<String> startRegistration(@RequestBody RequestOtpDTO request) {
        logger.info("📤 Sending OTP to email: {}", request.getEmail());

        return forwardRequest(
            request,
            "/auth/email/send",
            RequestOtpDTO.class
        );
    }

    @PostMapping("/auth/verifyotp")
    public Mono<String> verifyOtpMono(@RequestBody VerifyOtpDTO verifyOtpDTO) {
        logger.info("🔐 Verifying OTP for email: {}", verifyOtpDTO.getEmail());

        return webClientBuilder.build()
            .post()
            .uri(API_GATEWAY_URL + "/auth/otp/verifyotp")
            .bodyValue(verifyOtpDTO)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error("❌ OTP verification failed: {}", body);
                        return Mono.error(new OtpVerificationException("OTP verification failed: " + body));
                    })
            )
            .bodyToMono(String.class)
            .doOnSuccess(response -> logger.info("✅ OTP verification successful"))
            .doOnError(err -> logger.error("❌ OTP verification error: {}", err.getMessage()));
    }

    @PostMapping("/auth/completereg/user")
    public Mono<String> completeUserRegistration(@RequestBody UserRegRequest request) {
        logger.info("🧍 Completing user registration for: {}", request.getEmail());

        if (!Boolean.TRUE.equals(request.isOtpVerified())) {
            logger.warn("❌ Attempt to register without OTP verification for: {}", request.getEmail());
            return Mono.just("{\"error\":\"OTP not verified. Please verify OTP first.\"}");
        }

        return webClientBuilder.build()
            .post()
            .uri(API_GATEWAY_URL + "/auth/register/user")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error("❌ User registration failed: {}", body);
                        return Mono.error(new UserRegistrationException("User registration failed: " + body));
                    })
            )
            .bodyToMono(String.class)
            .doOnSuccess(response -> logger.info("✅ User registration successful for: {}", request.getEmail()))
            .doOnError(err -> logger.error("❌ Error during user registration: {}", err.getMessage()));
    }

    @PostMapping("/auth/completereg/merchant")
    public Mono<String> completeMerchantRegistration(@RequestBody MerchantRegRequest request) {
        logger.info("🏬 Completing merchant registration for: {}", request.getEmail());

        if (!Boolean.TRUE.equals(request.isOtpVerified())) {
            logger.warn("❌ Attempt to register merchant without OTP verification for: {}", request.getEmail());
            return Mono.just("{\"error\":\"OTP not verified. Please verify OTP first.\"}");
        }

        return webClientBuilder.build()
            .post()
            .uri(API_GATEWAY_URL + "/auth/register/merchant")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error("❌ Merchant registration failed: {}", body);
                        return Mono.error(new MerchantRegistrationException("Merchant registration failed: " + body));
                    })
            )
            .bodyToMono(String.class)
            .doOnSuccess(response -> logger.info("✅ Merchant registration successful for: {}", request.getEmail()))
            .doOnError(err -> logger.error("❌ Error during merchant registration: {}", err.getMessage()));
    }

    private <T> Mono<String> forwardRequest(T request, String path, Class<T> typeClass) {
        logger.info("🔁 Forwarding request to path: {}", path);

        return webClientBuilder.build()
            .post()
            .uri(API_GATEWAY_URL + path)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error("❌ Request failed: {}", body);
                        return Mono.error(new RuntimeException("Request failed: " + body));
                    })
            )
            .bodyToMono(String.class)
            .doOnSuccess(response -> logger.info("✅ Request successful to path: {}", path))
            .doOnError(err -> logger.error("❌ Request failed to path: {}. Error: {}", path, err.getMessage()));
    }
}
