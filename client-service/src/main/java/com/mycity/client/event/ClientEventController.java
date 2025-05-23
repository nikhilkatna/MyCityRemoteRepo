package com.mycity.client.event;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.client.config.MultipartInputStreamFileResource;
import com.mycity.client.config.CookieTokenExtractor;
import com.mycity.shared.eventsdto.EventsDTO;
import com.mycity.shared.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
@Slf4j
public class ClientEventController {

    private final CookieTokenExtractor cookieTokenExtractor;
    private final WebClient.Builder webClientBuilder;

    private static final String API_GATEWAY_SERVICE_NAME = "API-GATEWAY";
    private static final String ADMIN_EVENT_PATH = "/admin/event/add";
    private static final String UPDATE_PATH = "/admin/event/update/";
    private static final String DELETE_PATH = "/admin/event/delete/";
    private static final String EVENTS_FETCH_DETAILS = "/event/internal/fetch/";
    private static final String EVENTS_FETCH_CARTS = "/event/internal/fetch";

    @PostMapping(value = "/event/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> addEvent(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart("galleryImages") List<MultipartFile> galleryImages) {

        log.info("Received request to add event: {}", eventDTO.getEventName());
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.debug("Extracted token from cookie: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("Authorization token is missing");
            return Mono.just(ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Authorization token is missing", null)));
        }

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

        try {
            for (MultipartFile file : galleryImages) {
                body.add("galleryImages", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        } catch (Exception e) {
            log.error("Error reading gallery images: {}", e.getMessage(), e);
            return Mono.just(ResponseEntity.status(500)
                    .body(new ApiResponse<>(500, "Failed to process images: " + e.getMessage(), null)));
        }

        if (imageNames != null) {
            for (String name : imageNames) {
                body.add("imageNames", name);
            }
        }

        return webClientBuilder.build()
                .post()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + ADMIN_EVENT_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .map(res -> {
                    log.info("Event added successfully: {}", eventDTO.getEventName());
                    return ResponseEntity.ok(new ApiResponse<>(200, "Event added successfully", res));
                })
                .doOnError(e -> log.error("Failed to add event: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(new ApiResponse<>(500, "Failed to add event: " + e.getMessage(), null))));
    }

    @PutMapping(value = "/event/update/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> updateEvent(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @PathVariable Long eventId,
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart(name = "galleryImages", required = false) List<MultipartFile> galleryImages) {

        log.info("Received request to update event with id: {}", eventId);
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.debug("Extracted token from cookie: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("Unauthorized: Missing token");
            return Mono.just(ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Unauthorized: Missing token", null)));
        }

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

        try {
            if (galleryImages != null) {
                for (MultipartFile file : galleryImages) {
                    body.add("galleryImages", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
                }
            }
        } catch (Exception e) {
            log.error("Error reading gallery images: {}", e.getMessage(), e);
            return Mono.just(ResponseEntity.status(500)
                    .body(new ApiResponse<>(500, "Failed to process images: " + e.getMessage(), null)));
        }

        if (imageNames != null) {
            for (String name : imageNames) {
                body.add("imageNames", name);
            }
        }

        return webClientBuilder.build()
                .put()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + UPDATE_PATH + eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .map(res -> {
                    log.info("Event updated successfully: id={}", eventId);
                    return ResponseEntity.ok(new ApiResponse<>(200, "Event updated successfully", res));
                })
                .doOnError(e -> log.error("Update failed for event id {}: {}", eventId, e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(new ApiResponse<>(500, "Update failed: " + e.getMessage(), null))));
    }

    @DeleteMapping("/event/delete/{eventId}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteEvent(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @PathVariable Long eventId) {

        log.info("Received request to delete event with id: {}", eventId);
        String token = cookieTokenExtractor.extractTokenFromCookie(cookie);
        log.debug("Extracted token from cookie: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("Unauthorized: Missing token");
            return Mono.just(ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Unauthorized: Missing token", null)));
        }

        return webClientBuilder.build()
                .delete()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + DELETE_PATH + eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .map(res -> {
                    log.info("Event deleted successfully: id={}", eventId);
                    return ResponseEntity.ok(new ApiResponse<>(200, "Event deleted successfully", res));
                })
                .doOnError(e -> log.error("Delete failed for event id {}: {}", eventId, e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(new ApiResponse<>(500, "Delete failed: " + e.getMessage(), null))));
    }

    @GetMapping("/event/fetch/{eventId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> fetchEventDetails(
            @PathVariable Long eventId) {

        log.info("Fetching event details for id: {}", eventId);

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + EVENTS_FETCH_DETAILS + eventId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(res -> {
                    log.info("Fetched event details for id: {}", eventId);
                    return ResponseEntity.ok(new ApiResponse<>(200, "Fetched event details", res));
                })
                .doOnError(e -> log.error("Failed to fetch event details for id {}: {}", eventId, e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(new ApiResponse<>(500, "Failed to fetch event details: " + e.getMessage(), null))));
    }

    @GetMapping("/event/fetch")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> fetchEventCarts() {

        log.info("Fetching event carts");

        return webClientBuilder.build()
                .get()
                .uri("lb://" + API_GATEWAY_SERVICE_NAME + EVENTS_FETCH_CARTS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(res -> {
                    log.info("Fetched event carts");
                    return ResponseEntity.ok(new ApiResponse<>(200, "Fetched event carts", res));
                })
                .doOnError(e -> log.error("Failed to fetch event carts: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(new ApiResponse<>(500, "Failed to fetch event carts: " + e.getMessage(), null))));
    }
}
