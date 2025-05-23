package com.mycity.category.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.category.exception.MediaServiceException;

import reactor.core.publisher.Mono;

@Service
public class WebClientMediaService {

    private static final Logger logger = LoggerFactory.getLogger(WebClientMediaService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String MEDIA_SERVICE = "MEDIA-SERVICE";
    private static final String MEDIA_FETCH_PATH = "/media/bycategory/image?category=";
    private static final String MEDIA_FETCHBY_PLACE_PATH = "/media/findby/place";

    public Mono<String> fetchCategoryImage(String categoryName) {
        logger.info("Fetching image for category: {}", categoryName);

        return webClientBuilder.baseUrl("lb://" + MEDIA_SERVICE)
            .build()
            .get()
            .uri(MEDIA_FETCH_PATH + categoryName)
            .retrieve()
            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                logger.error("Failed to fetch category image for '{}'. Status code: {}", categoryName, response.statusCode());
                return response.bodyToMono(String.class)
                        .defaultIfEmpty("No error body")
                        .flatMap(errorBody -> Mono.error(new MediaServiceException("Failed to fetch category image: " + errorBody)));
            })
            .bodyToMono(String.class)
            .doOnNext(image -> logger.info("Received image URL for category '{}': {}", categoryName, image))
            .doOnError(e -> logger.error("Exception while fetching category image for '{}': {}", categoryName, e.getMessage()));
    }

    public Mono<List<String>> getImagesByPlaceId(Long placeId) {
        logger.info("Fetching images for place ID: {}", placeId);

        return webClientBuilder.baseUrl("lb://" + MEDIA_SERVICE)
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder.path(MEDIA_FETCHBY_PLACE_PATH)
                                         .queryParam("placeId", placeId)
                                         .build())
            .retrieve()
            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                logger.error("Failed to fetch images for placeId {}. Status: {}", placeId, response.statusCode());
                return response.bodyToMono(String.class)
                        .defaultIfEmpty("No error body")
                        .flatMap(errorBody -> Mono.error(new MediaServiceException("Failed to fetch place images: " + errorBody)));
            })
            .bodyToFlux(String.class)
            .collectList()
            .doOnNext(images -> logger.info("Fetched {} images for place ID: {}", images.size(), placeId))
            .doOnError(e -> logger.error("Exception while fetching images for placeId {}: {}", placeId, e.getMessage()));
    }
}
