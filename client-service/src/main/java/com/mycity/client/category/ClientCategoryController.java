package com.mycity.client.category;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.serviceImpl.ClientCategoryService;
import com.mycity.shared.categorydto.CategoryImageDTO;
import com.mycity.shared.categorydto.CategoryWithPlacesDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")  
public class ClientCategoryController {

    private static final Logger log = LoggerFactory.getLogger(ClientCategoryController.class);

    private final CookieTokenExtractor cookieTokenExtractor;

    @Autowired
    private ClientCategoryService clientCategoryService;

    ClientCategoryController(CookieTokenExtractor cookieTokenExtractor) {
        this.cookieTokenExtractor = cookieTokenExtractor;
    }  

    @GetMapping("/bycategory/unique/images")
    public Mono<ResponseEntity<List<CategoryImageDTO>>> getCategoriesWithImages() {
        log.info("Request received: fetch all categories with images");
        return clientCategoryService.fetchCategoriesWithImages()
            .doOnNext(categories -> log.info("Fetched {} categories with images", categories.size()))
            .doOnError(err -> log.error("Error fetching categories with images: {}", err.getMessage()))
            .map(ResponseEntity::ok);
    }
    
    @GetMapping("/bycategory/{categoryName}/places")
    public Mono<ResponseEntity<List<CategoryWithPlacesDTO>>> getSingleCategoryWithPlacesAndImages(@PathVariable String categoryName) {
        log.info("Request received: fetch category with places and images for category '{}'", categoryName);
        return clientCategoryService.fetchCategoryWithPlacesAndImages(categoryName)
                .doOnNext(places -> log.info("Fetched {} places for category '{}'", places.size(), categoryName))
                .doOnError(err -> log.error("Error fetching places for category '{}': {}", categoryName, err.getMessage()))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
