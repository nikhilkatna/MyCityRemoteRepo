package com.mycity.client.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.shared.ratingdto.RatingDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/rating")
public class ClientPlaceRatingController {

    private static final Logger log = LoggerFactory.getLogger(ClientPlaceRatingController.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private CookieTokenExtractor extractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String USER_RATING_ADDING_PATH = "/user/rating/add";

    @PostMapping("/addrating")
    public Mono<String> addPlaceRating(
            @RequestBody RatingDTO dto,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        log.info("Invoked addPlaceRating for placeName={} by userName={}", dto.getPlaceName(), dto.getUserName());

        String token = extractor.extractTokenFromCookie(cookie);
        log.debug("Extracted token from cookie");

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + USER_RATING_ADDING_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Failed to add rating. Status: {}, ErrorBody: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Failed to Add Rating: "
                                            + clientResponse.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Successfully added rating for placeName={}", dto.getPlaceName()))
                .doOnError(e -> log.error("Error while adding rating: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just("Failed to Add Rating To The Place: " + e.getMessage()));
    }
}
