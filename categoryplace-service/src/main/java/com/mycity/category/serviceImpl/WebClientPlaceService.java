package com.mycity.category.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.category.exception.PlaceServiceException;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class WebClientPlaceService {

    private static final Logger logger = LoggerFactory.getLogger(WebClientPlaceService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String PLACE_SERVICE = "PLACE-SERVICE";
    private static final String PLACE_FETCH_PATH = "/place/placeby/categories";
    private static final String PLACE_BY_CATEGORY_PATH = "/place/bycategory/";

    public Flux<PlaceCategoryDTO> fetchPlaceCategories() {
        logger.info("Calling PLACE-SERVICE to fetch categories with place mapping.");

        return webClientBuilder.baseUrl("lb://" + PLACE_SERVICE)
            .build()
            .get()
            .uri(PLACE_FETCH_PATH)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                response -> {
                    logger.error("Failed to fetch place categories. Status: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .defaultIfEmpty("No error body")
                        .flatMap(errorBody -> Mono.error(
                            new PlaceServiceException("Failed to fetch place categories: " + errorBody)
                        ));
                })
            .bodyToFlux(PlaceCategoryDTO.class)
            .doOnNext(dto -> logger.debug("Fetched category: {}", dto.getCategoryName()))
            .doOnError(e -> logger.error("Exception while fetching place categories: {}", e.getMessage()));
    }

    public Mono<List<PlaceDTO>> getPlacesByCategoryId(String categoryId) {
        logger.info("Calling PLACE-SERVICE to get places for category ID: {}", categoryId);

        return webClientBuilder.baseUrl("lb://" + PLACE_SERVICE)
            .build()
            .get()
            .uri(PLACE_BY_CATEGORY_PATH + categoryId)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                response -> {
                    logger.error("Failed to fetch places for category ID {}. Status: {}", categoryId, response.statusCode());
                    return response.bodyToMono(String.class)
                        .defaultIfEmpty("No error body")
                        .flatMap(errorBody -> Mono.error(
                            new PlaceServiceException("Failed to fetch places: " + errorBody)
                        ));
                })
            .bodyToFlux(PlaceDTO.class)
            .collectList()
            .doOnNext(places -> logger.info("Fetched {} places for category ID: {}", places.size(), categoryId))
            .doOnError(e -> logger.error("Exception while fetching places for category ID {}: {}", categoryId, e.getMessage()));
    }
}
