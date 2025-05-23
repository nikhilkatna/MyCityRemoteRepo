package com.mycity.client.place;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/review")
@RequiredArgsConstructor
public class ClientPlaceReviewController {

    private static final Logger log = LoggerFactory.getLogger(ClientPlaceReviewController.class);

    @Autowired
    private final WebClient.Builder webClientBuilder;

    @Autowired
    private final CookieTokenExtractor extractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String REVIEW_ADDING_PATH = "/user/review/add";
    private static final String REVIEW_UPDATING_PATH = "/user/review/update/{reviewId}";
    private static final String REVIEWS_GETTING_PATH = "/user/review/get/{placeId}";
    private static final String REVIEWS_DELETING_PATH = "/user/review/delete/{reviewId}";

    @PostMapping(path = "/addreview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> addPlaceReview(
            @ModelAttribute("dto") ReviewDTO dto,
            @RequestPart("images") List<MultipartFile> images,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) throws JsonProcessingException {

        log.info("Invoked addPlaceReview for place: {}, userName: {}", dto.getPlaceName(), dto.getUserName());

        String token = extractor.extractTokenFromCookie(cookie);
        log.debug("Extracted JWT token for review submission");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String reviewDtoJson = objectMapper.writeValueAsString(dto);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("dto", reviewDtoJson)
                .header("Content-Disposition", "form-data; name=dto")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        for (MultipartFile image : images) {
            builder.part("images", image.getResource());
        }

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + REVIEW_ADDING_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        log.info("Successfully added review for place: {}", dto.getPlaceName());
                        return response.bodyToMono(String.class)
                                .map(ResponseEntity::ok);
                    } else {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("No error body")
                                .map(errorBody -> {
                                    log.error("Failed to add review: {}", errorBody);
                                    return ResponseEntity
                                            .status(response.statusCode())
                                            .body("Failed to Add Review: " + errorBody);
                                });
                    }
                })
                .onErrorResume(e -> {
                    log.error("Exception occurred while adding review: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Exception: " + e.getMessage()));
                });
    }

    @PutMapping("/updatereview/{reviewId}")
    public Mono<String> updatePlaceReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDTO dto,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        log.info("Invoked updatePlaceReview for reviewId: {}", reviewId);

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .put()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + REVIEW_UPDATING_PATH, reviewId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Failed to update review: {}", errorBody);
                                    return Mono.error(new RuntimeException(
                                            "Failed to Update Review: " + clientResponse.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("Successfully updated reviewId: {}", reviewId))
                .onErrorResume(e -> {
                    log.error("Error while updating review: {}", e.getMessage());
                    return Mono.just("Failed to Update Review: " + e.getMessage());
                });
    }

    @GetMapping("/getreview/{placeId}")
    public Mono<ResponseEntity<List<ReviewSummaryDTO>>> getReviewsByPlaceId(
            @PathVariable Long placeId,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        System.out.println("ClientPlaceReviewController.getPlacesByReviewId()");

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + REVIEWS_GETTING_PATH, placeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        // On success, get list and wrap with status code
                        return response.bodyToFlux(ReviewSummaryDTO.class)
                                .collectList()
                                .map(body -> ResponseEntity.status(response.statusCode()).body(body));
                    } else {
                        // On error, read error body and return empty list with that status
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(errorBody -> {
                                    System.err.println("Failed to get reviews: " + response.statusCode() + " - " + errorBody);
                                    return Mono.just(ResponseEntity.status(response.statusCode())
                                            .body(Collections.<ReviewSummaryDTO>emptyList()));
                                });
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Unexpected error: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.emptyList()));
                });
    }


    @DeleteMapping("/deletereview/{reviewId}")
    public Mono<ResponseEntity<String>> deleteReviewsByPlaceId(
            @PathVariable Long reviewId,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        log.info("Attempting to delete review with reviewId: {}", reviewId);

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .delete()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + REVIEWS_DELETING_PATH, reviewId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchangeToMono(clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")  // handle empty body case
                        .map(body -> new ResponseEntity<>(body, clientResponse.statusCode()))
                )
                .doOnSuccess(resp -> log.info("Delete review response status: {}", resp.getStatusCode()))
                .onErrorResume(e -> {
                    log.error("Error while deleting review: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to delete review: " + e.getMessage()));
                });
    }

}
