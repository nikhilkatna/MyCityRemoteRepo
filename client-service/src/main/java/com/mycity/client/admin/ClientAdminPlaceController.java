package com.mycity.client.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.AdminPlaceFetchException;
import com.mycity.shared.admindto.AdminPlaceResponseDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/admin")
public class ClientAdminPlaceController {

    private static final Logger logger = LoggerFactory.getLogger(ClientAdminPlaceController.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private CookieTokenExtractor extractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String PATH_TO_GET_ALL_PLACES = "/admin/getallplaces";

    @GetMapping("/getallplaces")
    public Mono<ResponseEntity<List<AdminPlaceResponseDTO>>> getAllPlaces(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        logger.info("Received request to fetch all places for admin.");

        String token = extractor.extractTokenFromCookie(cookie);
        logger.debug("Extracted token from cookie: {}", token != null ? "[REDACTED]" : "null");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PATH_TO_GET_ALL_PLACES)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> {
                        logger.error("Failed to fetch places. Status code: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                            .defaultIfEmpty("No error body")
                            .flatMap(errorBody -> Mono.error(
                                new AdminPlaceFetchException("Failed to fetch places from API Gateway: " + errorBody)));
                    })
                .toEntity(new ParameterizedTypeReference<List<AdminPlaceResponseDTO>>() {})
                .doOnSuccess(response -> logger.info("Successfully retrieved {} places.",
                        response.getBody() != null ? response.getBody().size() : 0))
                .doOnError(e -> logger.error("Exception occurred while fetching admin places: {}", e.getMessage()))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()));
    }
}
