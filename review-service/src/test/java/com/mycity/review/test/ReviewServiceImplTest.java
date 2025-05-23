package com.mycity.review.test;

import com.mycity.review.entity.Review;
import com.mycity.review.repository.ReviewRepository;
import com.mycity.review.serviceImpl.ReviewServiceimpl;
import com.mycity.review.serviceImpl.WebClientMediaService;
import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
public class ReviewServiceImplTest {

    @InjectMocks
    private ReviewServiceimpl reviewService;

    @Mock
    private ReviewRepository reviewRepo;

    @Mock
    private WebClientMediaService mediaService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> uriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    public void testAddPlaceReview_success() {
        ReviewDTO dto = new ReviewDTO();
        dto.setUserName("testuser");
        dto.setPlaceName("TestPlace");
        dto.setReviewDescription("Great experience!");

        List<MultipartFile> images = new ArrayList<>();

        Review savedReview = new Review();
        savedReview.setReviewId(1L);
        savedReview.setUserId(201L);
        savedReview.setPlaceId(101L);
        savedReview.setPlaceName("TestPlace");

        // Return the same WebClient mock for both calls to .build()
        when(webClientBuilder.build()).thenReturn(webClient, webClient);

        // --- Suppressed Raw Type Mocks ---
        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersUriSpec placeUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersSpec placeHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        @SuppressWarnings("unchecked")
        WebClient.ResponseSpec placeResponseSpec = mock(WebClient.ResponseSpec.class);

        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersSpec userHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        @SuppressWarnings("unchecked")
        WebClient.ResponseSpec userResponseSpec = mock(WebClient.ResponseSpec.class);

        // --- Mock chain for WebClient.get() ---
        when(webClient.get()).thenReturn(placeUriSpec, userUriSpec);

        // --- Place ID lookup chain ---
        when(placeUriSpec.uri(anyString(), eq("TestPlace"))).thenReturn(placeHeadersSpec);
        when(placeHeadersSpec.retrieve()).thenReturn(placeResponseSpec);
        when(placeResponseSpec.onStatus(any(), any())).thenReturn(placeResponseSpec);
        when(placeResponseSpec.bodyToMono(Long.class)).thenReturn(Mono.just(101L));

        // --- User ID lookup chain ---
        when(userUriSpec.uri(anyString(), eq("testuser"))).thenReturn(userHeadersSpec);
        when(userHeadersSpec.retrieve()).thenReturn(userResponseSpec);
        when(userResponseSpec.onStatus(any(), any())).thenReturn(userResponseSpec);
        when(userResponseSpec.bodyToMono(Long.class)).thenReturn(Mono.just(201L));

        // --- Mock DB save ---
        when(reviewRepo.save(any())).thenReturn(savedReview);

        // Call the service method
        ResponseEntity<String> response = reviewService.addPlaceReview(dto, images);

        // Assertions
        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Review saved.", response.getBody());
    }


    @Test
    public void testUpdateReview_success() {
        Review review = new Review();
        review.setReviewId(1L);
        review.setReviewDescription("Old review");

        ReviewDTO dto = new ReviewDTO();
        dto.setReviewDescription("Updated review");

        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepo.save(any())).thenReturn(review);

        String result = reviewService.updateReview(1L, dto);

        assertEquals("Review With Id 1 Updated.", result);
    }

    @Test
    public void testDeleteReview_success() {
        Review review = new Review();
        review.setReviewId(1L);

        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review));
        when(mediaService.deleteImagesForReviews(1L)).thenReturn("Images deleted");

        String result = reviewService.deleteReview(1L);

        assertEquals("Review & Images deleted", result);
    }

    @Test
    public void testGetUserReview_success() {
        Review review = new Review();
        review.setPlaceId(1L);
        review.setUserName("testuser");
        review.setPlaceName("TestPlace");
        review.setReviewDescription("Nice place");
        review.setPostedOn(LocalDate.now());

        when(reviewRepo.findByPlaceId(1L)).thenReturn(List.of(review));
        when(mediaService.getImageUrlByPlaceId(1L)).thenReturn("img.jpg");

        List<ReviewSummaryDTO> result = reviewService.getUserReview(1L);

        assertEquals(1, result.size());
        assertEquals("Nice place", result.get(0).getReviewDescription());
        assertEquals("img.jpg", result.get(0).getUserImageUrl());
    }

    @Test
    public void testFetchReviews_success() {
        Review review = new Review();
        review.setPlaceId(1L);
        review.setUserName("testuser");
        review.setPlaceName("TestPlace");
        review.setReviewDescription("Amazing experience");
        review.setPostedOn(LocalDate.now());

        when(reviewRepo.findByPlaceId(1L)).thenReturn(List.of(review));
        when(mediaService.getImageUrlByPlaceId(1L)).thenReturn("url.jpg");

        List<ReviewDTO> result = reviewService.fetchReviews(1L);

        assertEquals(1, result.size());
        assertEquals("Amazing experience", result.get(0).getReviewDescription());
        assertEquals("url.jpg", result.get(0).getImageUrl());
    }

    @Test
    public void testValidateReviewDTO_throwsExceptionForInvalidData() {
        ReviewDTO dto = new ReviewDTO(); // all fields null

        Exception ex = assertThrows(IllegalArgumentException.class, () -> reviewService.validateReviewDTO(dto));
        assertEquals("UserName Cannot be Blank", ex.getMessage());
    }
}
*/