package com.mycity.client.place;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycity.client.exception.MediaServiceUnavailableException;
import com.mycity.client.exception.PlaceNotFoundException;
import com.mycity.client.exception.ReviewServiceUnavailableException;
import com.mycity.client.exception.EventServiceUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/client")
public class ClientAboutPlaceDetailController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final Logger logger = LoggerFactory.getLogger(ClientAboutPlaceDetailController.class);

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String PLACE_GETTING_PATH = "/place/about/{placeName}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/about/place/{placeName}")
    public Mono<ResponseEntity<Map<String, Object>>> getPlaceDetails(@PathVariable String placeName) {
        logger.info("Request to get place details for: {}", placeName);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_GETTING_PATH, placeName)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                
                .doOnSuccess(response -> 
                    logger.info("Received successful response with status: {}", response.getStatusCode())
                )

                // Handle 404 Not Found from place-service
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    logger.warn("Place not found: {}", placeName);
                    throw new PlaceNotFoundException("Place not found: " + placeName);
                })

                // Handle other structured error responses
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("Error response from place-service: status {}, body: {}", 
                            ex.getStatusCode(), ex.getResponseBodyAsString());

                    try {
                        Map<String, Object> errorMap = objectMapper.readValue(
                                ex.getResponseBodyAsString(),
                                new TypeReference<>() {}
                        );

                        String errorType = (String) errorMap.get("error");

                        if ("Review Service Unavailable".equalsIgnoreCase(errorType)) {
                            throw new ReviewServiceUnavailableException((String) errorMap.get("message"));
                        } else if ("Media Service Unavailable".equalsIgnoreCase(errorType)) {
                            throw new MediaServiceUnavailableException((String) errorMap.get("message"));
                        } else if ("Event Service Unavailable".equalsIgnoreCase(errorType)) {
                            throw new EventServiceUnavailableException((String) errorMap.get("message"));
                        }

                        // If not one of the known error types, just return errorMap in response entity
                        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(errorMap));
                    } catch (Exception parseEx) {
                        logger.error("Failed to parse error response body", parseEx);
                        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(
                                Map.of(
                                    "error", "Failed to retrieve place details",
                                    "status", ex.getStatusCode().value(),
                                    "message", ex.getMessage()
                                )));
                    }
                })

                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error while retrieving place details", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            Map.of(
                                    "error", "Internal Server Error",
                                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "message", ex.getMessage()
                            )));
                });
    }
}
