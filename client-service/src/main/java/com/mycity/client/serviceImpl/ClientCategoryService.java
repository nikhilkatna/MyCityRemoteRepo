package com.mycity.client.serviceImpl;

import com.mycity.client.exception.ClientException;
import com.mycity.shared.categorydto.CategoryImageDTO;
import com.mycity.shared.categorydto.CategoryWithPlacesDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class ClientCategoryService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String CATEGORY_SERVICE = "CATEGORY-SERVICE";
    private static final String CATEGORY_FETCH_PATH = "/category/unique/images";
    private static final String CATEGORY_BY_NAME_PATH = "/category/category-by-name/{categoryName}";

    public Mono<List<CategoryImageDTO>> fetchCategoriesWithImages() {
        log.info("Fetching all categories with images");

        return webClientBuilder
                .baseUrl("lb://" + CATEGORY_SERVICE)
                .build()
                .get()
                .uri(CATEGORY_FETCH_PATH)
                .retrieve()
                .bodyToFlux(CategoryImageDTO.class)
                .collectList()
                .doOnSuccess(result -> log.info("Fetched {} categories", result.size()))
                .doOnError(e -> log.error("Failed to fetch categories with images", e))
                .onErrorMap(WebClientResponseException.class, e -> {
                    String errorMsg = "Error from CATEGORY-SERVICE: " + e.getResponseBodyAsString();
                    return new ClientException(errorMsg, e);
                })
                .onErrorMap(e -> new ClientException("Internal error while fetching categories", e));
    }

    public Mono<List<CategoryWithPlacesDTO>> fetchCategoryWithPlacesAndImages(String categoryName) {
        log.info("Fetching category with places and images for category: {}", categoryName);

        return webClientBuilder
                .baseUrl("lb://" + CATEGORY_SERVICE)
                .build()
                .get()
                .uri(CATEGORY_BY_NAME_PATH, categoryName)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CategoryWithPlacesDTO>>() {})
                .doOnSuccess(result -> log.info("Fetched {} places under category '{}'", result.size(), categoryName))
                .doOnError(e -> log.error("Failed to fetch category with places for '{}'", categoryName, e))
                .onErrorMap(WebClientResponseException.class, e -> {
                    String errorMsg = "Error from CATEGORY-SERVICE: " + e.getResponseBodyAsString();
                    return new ClientException(errorMsg, e);
                })
                .onErrorMap(e -> new ClientException("Internal error while fetching category with places", e));
    }
}
