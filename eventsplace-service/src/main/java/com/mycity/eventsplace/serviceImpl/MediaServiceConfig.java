package com.mycity.eventsplace.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.eventsplace.exception.MediaServiceException;
import com.mycity.shared.mediadto.EventSubImagesDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MediaServiceConfig {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String MEDIA_BASE_URL = "MEDIA-SERVICE";
    private static final String MEDIA_ADD_IMAGES = "/media/upload/images";
    private static final String MEDIA_UPDATE_IMAGES = "/media/update/images/";
    private static final String MEDIA_DELETE_IMAGES = "/media/delete/images/";
    private static final String MEDIA_FETCH_IMAGES = "/media/fetch/images/";

    public void uploadGalleryImages(List<MultipartFile> galleryImages, List<String> imageNames, EventSubImagesDTO dto) {
        try {
            WebClient webClient = webClientBuilder.build();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            for (int i = 0; i < galleryImages.size(); i++) {
                MultipartFile file = galleryImages.get(i);
                String customName = imageNames.size() > i ? imageNames.get(i) : file.getOriginalFilename();

                body.add("files", file.getResource());
                body.add("names", customName);
            }
            body.add("eventId", dto.getEventId());
            body.add("eventName", dto.getEventName());

            String response = webClient
                    .post()
                    .uri("lb://" + MEDIA_BASE_URL + MEDIA_ADD_IMAGES)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully uploaded images for event '{}'. Response: {}", dto.getEventName(), response);
        } catch (Exception e) {
            log.error("Error uploading gallery images for event '{}': {}", dto.getEventName(), e.getMessage(), e);
            throw new MediaServiceException("Failed to upload gallery images for event: " + dto.getEventName(), e);
        }
    }

    public void updateEventImagesInMediaService(Long eventId, String eventName, List<MultipartFile> galleryImages, List<String> imageNames) {
        try {
            WebClient webClient = webClientBuilder.build();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            for (int i = 0; i < galleryImages.size(); i++) {
                MultipartFile file = galleryImages.get(i);
                String customName = imageNames.size() > i ? imageNames.get(i) : file.getOriginalFilename();

                body.add("files", file.getResource());
                body.add("names", customName);
            }
            body.add("eventId", eventId);
            body.add("eventName", eventName);

            String response = webClient
                    .put()
                    .uri("lb://" + MEDIA_BASE_URL + MEDIA_UPDATE_IMAGES + eventId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully updated images for event '{}'. Response: {}", eventName, response);
        } catch (Exception e) {
            log.error("Error updating images for event '{}': {}", eventName, e.getMessage(), e);
            throw new MediaServiceException("Failed to update images for event: " + eventName, e);
        }
    }

    public void deleteEventImages(Long eventId, String eventName) {
        try {
            WebClient webClient = webClientBuilder.build();

            String mediaResponse = webClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                        .scheme("lb")
                        .host(MEDIA_BASE_URL)
                        .path(MEDIA_DELETE_IMAGES + eventId)
                        .queryParam("eventName", eventName)
                        .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully deleted images for event '{}'. Response: {}", eventName, mediaResponse);
        } catch (Exception e) {
            log.error("Error deleting images for event '{}': {}", eventName, e.getMessage(), e);
            throw new MediaServiceException("Failed to delete images for event: " + eventName, e);
        }
    }


    public List<String> getImageUrlsForEvent(Long eventId) {
        try {
            WebClient webClient = webClientBuilder.build();

            List<String> images = webClient
                    .get()
                    .uri("lb://" + MEDIA_BASE_URL + MEDIA_FETCH_IMAGES + eventId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block();

            log.info("Fetched {} images for event ID {}", images != null ? images.size() : 0, eventId);
            return images != null ? images : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching images for event ID {}: {}", eventId, e.getMessage(), e);
            throw new MediaServiceException("Failed to fetch images for event with ID: " + eventId, e);
        }
    }
}
