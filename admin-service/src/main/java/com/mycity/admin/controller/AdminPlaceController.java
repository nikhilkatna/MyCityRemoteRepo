package com.mycity.admin.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycity.admin.serviceImpl.WebClientMediaService;
import com.mycity.shared.admindto.AdminPlaceResponseDTO;
import com.mycity.shared.mediadto.AboutPlaceImageDTO;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;

@RestController
@RequestMapping("/admin")
public class AdminPlaceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlaceController.class);

    private static final String PLACE_SERVICE_NAME = "place-service";
    private static final String PATH_TO_GET_LIST_OF_PLACES = "/place/allplaces";
    private static final String PATH_TO_ADD_PLACE_WITH_IMAGES = "/place/add-place";
    private static final String PATH_TO_GET_PLACE = "/place/get/{placeId}";
    private static final String PATH_TO_UPDATE_PLACE = "/place/update/{placeId}";
    private static final String PATH_TO_DELETE_PLACE = "/place/delete/{placeId}";
    private static final String PATH_TO_GET_PLACE_IDS_AND_CATEGORIES = "/place/places/categories";

    @Autowired
    public WebClient.Builder webClientBuilder;

    @Autowired
    private WebClientMediaService mediaService;

    @GetMapping("/getallplaces")
    public ResponseEntity<List<AdminPlaceResponseDTO>> getAllPlacesForAdmin() {
        logger.info("Fetching all places for admin");

        List<PlaceResponseDTO> places = webClientBuilder
                .build()
                .get()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_GET_LIST_OF_PLACES)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PlaceResponseDTO>>() {})
                .block();

        List<AdminPlaceResponseDTO> dtos = new ArrayList<>();
        for (PlaceResponseDTO place : places) {
            AdminPlaceResponseDTO response = new AdminPlaceResponseDTO();
            response.setCurrentDate(LocalDate.now());
            response.setPlaceName(place.getPlaceName());
            response.setLocation(place.getPlaceDistrict());

            try {
                CompletableFuture<List<AboutPlaceImageDTO>> imagesFuture = mediaService.getImagesForPlace(place.getPlaceName());
                response.setPlaceRelatedImages(imagesFuture.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread was interrupted while fetching images for place: {}", place.getPlaceName(), e);
                throw new RuntimeException("Image fetch interrupted");
            } catch (ExecutionException e) {
                logger.error("Error occurred while fetching images for place: {}", place.getPlaceName(), e);
                throw new RuntimeException("Error fetching images");
            }

            dtos.add(response);
        }

        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PostMapping(path = "/place/addPlace",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addPlaceDetails(
            @RequestPart("placeDto") PlaceDTO placeDto,
            @RequestParam Map<String, MultipartFile> placeImages,
            @RequestParam Map<String, MultipartFile> cuisineImages
    ) throws JsonProcessingException {

        logger.info("Adding new place: {}", placeDto.getPlaceName());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String placeDtoJson = objectMapper.writeValueAsString(placeDto);
        builder.part("placeDto", placeDtoJson)
                .header("Content-Disposition", "form-data; name=placeDto")
                .contentType(MediaType.APPLICATION_JSON);

        placeImages.forEach((key, file) -> builder.part(key, file.getResource()));
        cuisineImages.forEach((key, file) -> builder.part(key, file.getResource()));

        String response = webClientBuilder.build()
                .post()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_ADD_PLACE_WITH_IMAGES)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getplace/{placeId}")
    public ResponseEntity<PlaceResponseDTO> getPlace(@PathVariable Long placeId) {
        logger.info("Fetching place with ID: {}", placeId);

        PlaceResponseDTO dto = webClientBuilder.build()
                .get()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_GET_PLACE, placeId)
                .retrieve()
                .bodyToMono(PlaceResponseDTO.class)
                .block();

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/updateplace/{placeId}")
    public ResponseEntity<String> updatePlace(@PathVariable Long placeId, @RequestBody PlaceDTO dto) {
        logger.info("Updating place with ID: {}", placeId);

        String result = webClientBuilder.build()
                .put()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_UPDATE_PLACE, placeId)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/deleteplace/{placeId}")
    public ResponseEntity<String> deletePlace(@PathVariable Long placeId) {
        logger.info("Deleting place with ID: {}", placeId);

        String result = webClientBuilder.build()
                .delete()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_DELETE_PLACE, placeId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/getall/category")
    public ResponseEntity<List<PlaceCategoryDTO>> getAllPlacesIdsByCategory() {
        logger.info("Fetching place IDs grouped by category");

        List<PlaceCategoryDTO> categories = webClientBuilder.build()
                .get()
                .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_GET_PLACE_IDS_AND_CATEGORIES)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PlaceCategoryDTO>>() {})
                .block();

        return new ResponseEntity<>(categories, HttpStatus.OK);
    }
}
