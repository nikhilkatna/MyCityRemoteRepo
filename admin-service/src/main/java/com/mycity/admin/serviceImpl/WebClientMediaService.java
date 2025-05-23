package com.mycity.admin.serviceImpl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.mycity.shared.mediadto.AboutPlaceImageDTO;

import lombok.NonNull;

@Service
public class WebClientMediaService {

    private static final Logger log = LoggerFactory.getLogger(WebClientMediaService.class);

    private final WebClient.Builder webClientBuilder;

    private static final String IMAGE_SERVICE = "MEDIA-SERVICE";
    private static final String IMAGE_FETCH_PATH = "/media/images/{placeName}";
    private static final String IMAGE_DELETING_PATH = "/media/images/delete/{placeId}";
    private static final String PATH_TO_GET_IMAGES = "/media/gallery/getimages/{districtName}";

    public WebClientMediaService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public CompletableFuture<List<AboutPlaceImageDTO>> getImagesForPlace(@NonNull String placeName) {
        log.info("Fetching images for place: {}", placeName);
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("lb://" + IMAGE_SERVICE + IMAGE_FETCH_PATH, placeName)
                    .retrieve()
                    .bodyToFlux(AboutPlaceImageDTO.class)
                    .collectList()
                    .toFuture();
        } catch (Exception ex) {
            log.error("Failed to fetch images for place '{}': {}", placeName, ex.getMessage());
            throw new RuntimeException("Failed to fetch images for place: " + placeName, ex);
        }
    }

    public String deleteImages(Long placeId) {
        log.info("Deleting images for placeId: {}", placeId);
        try {
            return webClientBuilder.build()
                    .delete()
                    .uri("lb://" + IMAGE_SERVICE + IMAGE_DELETING_PATH, placeId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            log.error("Failed to delete images for placeId {}: {}", placeId, ex.getMessage());
            throw new RuntimeException("Failed to delete images for placeId: " + placeId, ex);
        }
    }

    public CompletableFuture<List<String>> getImageUrlsForPlace(Long placeId) {
        log.info("Fetching image URLs for placeId: {}", placeId);
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("lb://" + IMAGE_SERVICE + IMAGE_FETCH_PATH, placeId)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .collectList()
                    .toFuture();
        } catch (Exception ex) {
            log.error("Failed to fetch image URLs for placeId {}: {}", placeId, ex.getMessage());
            throw new RuntimeException("Failed to fetch image URLs for placeId: " + placeId, ex);
        }
    }

    public List<String> getPhotoUrlsByPlaceId(long placeId) {
        log.info("Getting photo URLs synchronously for placeId: {}", placeId);
        try {
            return getImageUrlsForPlace(placeId).get();
        } catch (Exception ex) {
            log.error("Error getting photo URLs for placeId {}: {}", placeId, ex.getMessage());
            throw new RuntimeException("Failed to get photo URLs for placeId: " + placeId, ex);
        }
    }
}
