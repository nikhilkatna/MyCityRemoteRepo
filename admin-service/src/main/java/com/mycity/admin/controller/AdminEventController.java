package com.mycity.admin.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.admin.config.MultipartInputStreamFileResource;
import com.mycity.shared.eventsdto.EventsDTO;
import com.mycity.shared.response.ApiResponse;
import com.mycity.admin.exception.CustomClientException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminEventController {

    private static final Logger logger = LoggerFactory.getLogger(AdminEventController.class);

    private static final String EVENT_SERVICE_NAME = "eventsplace-service";
    private static final String ADD_EVENT_PATH = "/event/internal/add";
    private static final String UPDATE_EVENT_PATH = "/event/internal/update/";
    private static final String DELETE_EVENT_PATH = "/event/internal/delete/";

    @Autowired
    private final WebClient.Builder webClientBuilder;

    @PostMapping(value = "/event/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> addEvent(
            @RequestHeader("Authorization") String authHeader,
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart("galleryImages") List<MultipartFile> galleryImages
    ) {
        String token = extractToken(authHeader);
        logger.info("Received request to add event: {}", eventDTO.getEventName());

        try {
            MultiValueMap<String, Object> body = buildMultipartBody(eventDTO, imageNames, galleryImages);
            return webClientBuilder.build()
                    .post()
                    .uri("lb://" + EVENT_SERVICE_NAME + ADD_EVENT_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        logger.info("Event added successfully: {}", response);
                        return ResponseEntity.ok(new ApiResponse<>(200, "Event added successfully", response));
                    })
                    .onErrorResume(e -> {
                        logger.error("Error while adding event: {}", e.getMessage());
                        throw new CustomClientException("Failed to add event: " + e.getMessage());
                    });

        } catch (Exception e) {
            logger.error("Exception occurred while adding event: {}", e.getMessage());
            throw new CustomClientException("Exception occurred: " + e.getMessage());
        }
    }

    @PutMapping(value = "/event/update/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> updateEvent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long eventId,
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart(name = "galleryImages", required = false) List<MultipartFile> galleryImages
    ) {
        String token = extractToken(authHeader);
        logger.info("Received request to update event ID: {}", eventId);

        try {
            MultiValueMap<String, Object> body = buildMultipartBody(eventDTO, imageNames, galleryImages);
            return webClientBuilder.build()
                    .put()
                    .uri("lb://" + EVENT_SERVICE_NAME + UPDATE_EVENT_PATH + eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        logger.info("Event updated successfully: {}", response);
                        return ResponseEntity.ok(new ApiResponse<>(200, "Event updated successfully", response));
                    })
                    .onErrorResume(e -> {
                        logger.error("Error while updating event: {}", e.getMessage());
                        throw new CustomClientException("Failed to update event: " + e.getMessage());
                    });

        } catch (Exception e) {
            logger.error("Exception occurred while updating event: {}", e.getMessage());
            throw new CustomClientException("Exception occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/event/delete/{eventId}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteEvent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long eventId
    ) {
        String token = extractToken(authHeader);
        logger.info("Received request to delete event ID: {}", eventId);

        return webClientBuilder.build()
                .delete()
                .uri("lb://" + EVENT_SERVICE_NAME + DELETE_EVENT_PATH + eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    logger.info("Event deleted successfully: {}", response);
                    return ResponseEntity.ok(new ApiResponse<>(200, "Event deleted successfully", response));
                })
                .onErrorResume(e -> {
                    logger.error("Error while deleting event: {}", e.getMessage());
                    throw new CustomClientException("Failed to delete event: " + e.getMessage());
                });
    }

    private MultiValueMap<String, Object> buildMultipartBody(EventsDTO eventDTO, List<String> imageNames, List<MultipartFile> galleryImages) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        if (eventDTO.getEventName() != null) body.add("eventName", eventDTO.getEventName());
        if (eventDTO.getDate() != null) body.add("date", eventDTO.getDate());
        if (eventDTO.getDuration() != null) body.add("duration", eventDTO.getDuration().format(formatter));
        if (eventDTO.getDescription() != null) body.add("description", eventDTO.getDescription());
        if (eventDTO.getCity() != null) body.add("city", eventDTO.getCity());

        if (eventDTO.getSchedule() != null) {
            for (int i = 0; i < eventDTO.getSchedule().size(); i++) {
                var sched = eventDTO.getSchedule().get(i);
                body.add("schedule[" + i + "].date", sched.getDate());
                body.add("schedule[" + i + "].time", sched.getTime().format(formatter));
                body.add("schedule[" + i + "].activityName", sched.getActivityName());
            }
        }

        if (galleryImages != null) {
            for (MultipartFile file : galleryImages) {
                body.add("galleryImages", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }

        if (imageNames != null) {
            for (String name : imageNames) {
                body.add("imageNames", name);
            }
        }

        return body;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        } else {
            throw new CustomClientException("Invalid Authorization header");
        }
    }
}

