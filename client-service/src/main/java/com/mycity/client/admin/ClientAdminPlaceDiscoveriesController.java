package com.mycity.client.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.PlaceDiscoveryException;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceDiscoveriesDTO;
import com.mycity.shared.placedto.PlaceDiscoveriesResponeDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/discovery")
public class ClientAdminPlaceDiscoveriesController {

    private static final Logger logger = LoggerFactory.getLogger(ClientAdminPlaceDiscoveriesController.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private CookieTokenExtractor extractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String PATH_TO_ADD_PLACE_TO_DISCOVERY = "/admin/discoveries/addPlace";
    private static final String PATH_GET_LIST_OF_PLACES = "/place/discoveries/getall";
    private static final String PATH_TO_GET_PLACE_DETAILS = "/admin/discoveries/getPlace/{placeName}";

    @PostMapping("/addplace")
    public Mono<ResponseEntity<String>> addPlaceToDiscovery(@RequestBody PlaceDiscoveriesDTO dto,
                                                            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {
    	System.out.println("ClientAdminPlaceDiscoveriesController"+dto);
        logger.info("Request to add place to discoveries: {}", dto.getPlaceName());

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PATH_TO_ADD_PLACE_TO_DISCOVERY)
                .bodyValue(dto)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("Failed to add place to discovery: {}", body);
                                    return Mono.error(new PlaceDiscoveryException("AddPlaceToDiscovery failed: " + body));
                                }))
                .toEntity(String.class)
                .doOnSuccess(res -> logger.info("Successfully added place to discovery: {}", dto.getPlaceName()))
                .doOnError(err -> logger.error("Error adding place to discovery: {}", err.getMessage()))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()));
    }

    @GetMapping("/getall")
    public Mono<ResponseEntity<List<PlaceDiscoveriesResponeDTO>>> getAllPlaces(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie) {

        logger.info("Request to fetch all discovered places");

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PATH_GET_LIST_OF_PLACES)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("Failed to fetch discoveries: {}", body);
                                    return Mono.error(new PlaceDiscoveryException("GetAllPlaces failed: " + body));
                                }))
                .toEntity(new ParameterizedTypeReference<List<PlaceDiscoveriesResponeDTO>>() {})
                .doOnSuccess(res -> logger.info("Successfully fetched {} discovered places",
                        res.getBody() != null ? res.getBody().size() : 0))
                .doOnError(err -> logger.error("Error fetching discoveries: {}", err.getMessage()))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()));
    }

    @GetMapping("/getplace/{placeName}")
    public Mono<ResponseEntity<PlaceDTO>> getPlaceByName(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @PathVariable String placeName) {

        logger.info("Request to get place by name: {}", placeName);

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PATH_TO_GET_PLACE_DETAILS, placeName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .flatMap(body -> {
                                    logger.error("Failed to fetch place {}: {}", placeName, body);
                                    return Mono.error(new PlaceDiscoveryException("GetPlaceByName failed: " + body));
                                }))
                .toEntity(PlaceDTO.class)
                .doOnSuccess(res -> logger.info("Successfully fetched place details: {}", placeName))
                .doOnError(err -> logger.error("Error fetching place '{}': {}", placeName, err.getMessage()))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()));
    }
}
