package com.mycity.admin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.mycity.shared.placedto.PlaceDiscoveriesDTO;
import com.mycity.shared.placedto.PlaceDiscoveriesResponeDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/discoveries")
public class AdminPlaceDiscoveriesController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlaceDiscoveriesController.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String PLACE_SERVICE_NAME = "PLACE-SERVICE";
    private static final String PATH_TO_ADD_PLACE_TO_DISCOVERIES = "/place/discoveries/add";
    private static final String PATH_TO_GET_ALL_DISCOVERIES = "/place/discoveries/getall";
    private static final String PATH_TO_GET_SELECTED_PLACE_DETAILS = "/place/discoveries/getplace/{placeName}";

    @PostMapping("/addPlace")
    public ResponseEntity<String> addPlace(@RequestBody PlaceDiscoveriesDTO dto) {
        logger.info("Adding place to discoveries: {}", dto.getPlaceName());
        System.out.println("AdminPlaceDiscoveriesController.addPlace()");
        try {
            String result = webClientBuilder.build()
                    .post()
                    .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_ADD_PLACE_TO_DISCOVERIES)
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Successfully added place to discoveries: {}", result);
            return ResponseEntity.ok(result);

        } catch (WebClientResponseException ex) {
            logger.error("WebClient error while adding place to discoveries: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex; // Let GlobalExceptionHandler handle it

        } catch (Exception ex) {
            logger.error("Unexpected error while adding place to discoveries", ex);
            throw ex; // Let GlobalExceptionHandler handle it
        }
    }

    @GetMapping("/getallPlaces")
    public Mono<ResponseEntity<List<PlaceDiscoveriesResponeDTO>>> getAllPlaces() {
        logger.info("Fetching all discovered places");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_GET_ALL_DISCOVERIES)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PlaceDiscoveriesResponeDTO>>() {})
                .map(discoveries -> {
                    logger.info("Fetched {} discoveries", discoveries.size());
                    return ResponseEntity.ok(discoveries);
                })
                .doOnError(e -> logger.error("Error fetching discoveries", e));
    }

    @GetMapping("/getPlace/{placeName}")
    public ResponseEntity<PlaceResponseDTO> getPlaceDetails(@PathVariable String placeName) {
        logger.info("Fetching details for place: {}", placeName);

        try {
            PlaceResponseDTO place = webClientBuilder.build()
                    .get()
                    .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_GET_SELECTED_PLACE_DETAILS, placeName)
                    .retrieve()
                    .bodyToMono(PlaceResponseDTO.class)
                    .block();

            logger.info("Fetched place details: {}", place.getPlaceName());
            return ResponseEntity.ok(place);

        } catch (WebClientResponseException ex) {
            logger.error("WebClient error while fetching place details: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;

        } catch (Exception ex) {
            logger.error("Unexpected error while fetching place details", ex);
            throw ex;
        }
    }
}
