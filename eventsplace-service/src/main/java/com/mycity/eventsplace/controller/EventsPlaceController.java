package com.mycity.eventsplace.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.eventsplace.service.EventsPlaceServiceInterface;
import com.mycity.shared.eventsdto.EventsDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventsPlaceController {

    private static final Logger log = LoggerFactory.getLogger(EventsPlaceController.class);

    private final EventsPlaceServiceInterface eventsPlaceService;

    @PostMapping(value = "/internal/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addEvent(
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart("galleryImages") List<MultipartFile> galleryImages
    ) {
        log.info("Received request to add event: {}", eventDTO.getEventName());
        String response = eventsPlaceService.addEvent(eventDTO, galleryImages, imageNames);
        log.info("Event added successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/internal/update/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateEvent(
            @PathVariable Long eventId,
            @ModelAttribute EventsDTO eventDTO,
            @RequestParam(name = "imageNames", required = false) List<String> imageNames,
            @RequestPart(name = "galleryImages", required = false) List<MultipartFile> galleryImages
    ) {
        log.info("Received request to update event with ID: {}", eventId);
        String response = eventsPlaceService.updateEvent(eventId, eventDTO, galleryImages, imageNames);
        log.info("Event updated successfully with ID: {}", eventId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/internal/delete/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long eventId) {
        log.info("Received request to delete event with ID: {}", eventId);
        String response = eventsPlaceService.deleteEvent(eventId);
        log.info("Event deleted successfully with ID: {}", eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/fetch/{eventId}")
    public ResponseEntity<Map<String, Object>> fetchEventDetails(@PathVariable Long eventId) {
        log.info("Fetching details for event ID: {}", eventId);
        Map<String, Object> response = eventsPlaceService.createEventDetailsSection(eventId);

        if (response == null || response.isEmpty()) {
            log.warn("Event not found for ID: {}", eventId);
            return ResponseEntity.notFound().build();
        }

        log.info("Event details fetched successfully for ID: {}", eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/fetch")
    public ResponseEntity<Map<String, Object>> fetchEventsCarts() {
        log.info("Fetching all event cards");
        Map<String, Object> response = eventsPlaceService.createEventCartSection();

        if (response == null || response.isEmpty()) {
            log.warn("No events found to display");
            return ResponseEntity.notFound().build();
        }

        log.info("Event cards fetched successfully");
        return ResponseEntity.ok(response);
    }
}
