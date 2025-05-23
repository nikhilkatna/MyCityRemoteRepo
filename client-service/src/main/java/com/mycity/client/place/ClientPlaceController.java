package com.mycity.client.place;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.client.exception.ClientPlaceException;  // custom exception class
import com.mycity.shared.placedto.PlaceDTO;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client/place")
@RequiredArgsConstructor
public class ClientPlaceController {

    private static final Logger log = LoggerFactory.getLogger(ClientPlaceController.class);

    @Autowired
    private final WebClient.Builder webClientBuilder;

    @Autowired
    private CookieTokenExtractor extractor;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";

    private static final String PLACE_REGISTRATION_PATH = "/admin/place/addPlace";
    private static final String PLACE_ID_FINDING_PATH = "/place/getid/{placeName}";
    private static final String PLACE_DETAILS_FINDING_PATH = "/place/get/{placeId}";
    private static final String PLACE_UPDATING_PATH = "/admin/updateplace/{placeId}";
    private static final String PLACE_DELETING_PATH = "/admin/deleteplace/{placeId}";
    private static final String ALL_PLACES_FINDING_PATH = "/place/getall";

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> addPlace(
            @ModelAttribute("placeDto") PlaceDTO placeDto,
            @RequestParam Map<String, MultipartFile> placeImages,
            @RequestParam Map<String, MultipartFile> cuisineImages,
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie
    ) throws JsonProcessingException {
        log.info("Adding place with name: {}", placeDto.getPlaceName());

        String token = extractor.extractTokenFromCookie(cookie);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String placeDtoJson = mapper.writeValueAsString(placeDto);

        builder.part("placeDto", placeDtoJson)
               .header("Content-Disposition", "form-data; name=placeDto")
               .contentType(MediaType.APPLICATION_JSON);

        placeImages.forEach((key, file) -> builder.part(key, file.getResource()));
        cuisineImages.forEach((key, file) -> builder.part(key, file.getResource()));

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_REGISTRATION_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> 
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to add place: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to add place: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Place added successfully"))
                .doOnError(e -> log.error("Error adding place: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just("Failed to Add Place: " + e.getMessage()));
    }

    @GetMapping("/getid/{placeName}")
    public Mono<String> getPlaceId(@PathVariable String placeName) {
        log.info("Getting place ID for placeName: {}", placeName);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_ID_FINDING_PATH, placeName)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to get place ID: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to get place ID: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Error getting place ID: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just("Failed to get Place Id: " + e.getMessage()));
    }

    @GetMapping("/getplace/{placeId}")
    public Mono<PlaceDTO> getPlaceDetailsById(
            @PathVariable Long placeId,
            @RequestHeader(value = HttpHeaders.COOKIE) String cookie) {
        log.info("Getting place details for placeId: {}", placeId);

        String token = extractor.extractTokenFromCookie(cookie);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_DETAILS_FINDING_PATH, placeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to get place details: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to get place details: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(PlaceDTO.class)
                .doOnError(e -> log.error("Error getting place details: {}", e.getMessage()))
                .onErrorResume(e -> Mono.error(new ClientPlaceException("Failed to get Place Details: " + e.getMessage())));
    }

    @PutMapping("/update/{placeId}")
    public Mono<String> updatePlace(@PathVariable Long placeId, @RequestBody PlaceDTO dto) {
        log.info("Updating place with id: {}", placeId);

        return webClientBuilder.build()
                .put()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_UPDATING_PATH, placeId)
                .body(Mono.just(dto), PlaceDTO.class)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to update place: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to update place: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Place updated successfully"))
                .doOnError(e -> log.error("Error updating place: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just("Failed to UPDATE Place: " + e.getMessage()));
    }

    @DeleteMapping("/deleteplace/{placeId}")
    public Mono<String> deletePlaceById(@PathVariable Long placeId) {
        log.info("Deleting place with id: {}", placeId);

        return webClientBuilder.build()
                .delete()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + PLACE_DELETING_PATH, placeId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to delete place: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to delete place: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Place deleted successfully"))
                .doOnError(e -> log.error("Error deleting place: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just("Failed to Delete Place: " + e.getMessage()));
    }

    @GetMapping("/getallplaces")
    public Mono<List<PlaceDTO>> getAllPlaces() {
        log.info("Getting all places");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + ALL_PLACES_FINDING_PATH)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        log.error("Failed to get all places: Status {} Body: {}", clientResponse.statusCode(), errorBody);
                        return Mono.error(new ClientPlaceException("Failed to get list of places: " + clientResponse.statusCode() + " - " + errorBody));
                    })
                )
                .bodyToMono(new ParameterizedTypeReference<List<PlaceDTO>>() {})
                .doOnError(e -> log.error("Error getting all places: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(Collections.emptyList()));
    }
}
