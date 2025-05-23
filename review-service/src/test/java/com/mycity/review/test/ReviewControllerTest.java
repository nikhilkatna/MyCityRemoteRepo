package com.mycity.review.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycity.review.controller.PlaceReviewController;
import com.mycity.review.service.ReviewServiceInterface;
import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
@WebMvcTest(PlaceReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewServiceInterface reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testGetAllReviews() throws Exception {
        List<ReviewSummaryDTO> mockReviews = Arrays.asList(new ReviewSummaryDTO(), new ReviewSummaryDTO());

        Mockito.when(reviewService.getUserReview(1L)).thenReturn(mockReviews);

        mockMvc.perform(get("/review/getreviews/1"))
               .andExpect(status().isFound())
               .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void testAddReview() throws Exception {
        ReviewDTO reviewDTO = new ReviewDTO();
        MockMultipartFile jsonPart = new MockMultipartFile("dto", "", "application/json", objectMapper.writeValueAsBytes(reviewDTO));
        MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg", "image data".getBytes());

        Mockito.when(reviewService.addPlaceReview(any(), any())).thenReturn(ResponseEntity.ok("Success"));

        mockMvc.perform(multipart("/review/addreview")
                        .file(jsonPart)
                        .file(image)
                        .with(csrf()) // CSRF required for multipart POST
                        .contentType(MediaType.MULTIPART_FORM_DATA))
               .andExpect(status().isOk())
               .andExpect(content().string("Success"));
    }

    @Test
    @WithMockUser
    void testUpdateReview() throws Exception {
        ReviewDTO reviewDTO = new ReviewDTO();

        Mockito.when(reviewService.updateReview(eq(1L), any())).thenReturn("Updated");

        mockMvc.perform(put("/review/updatereview/1")
                        .with(csrf()) // CSRF required for PUT
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewDTO)))
               .andExpect(status().isOk())
               .andExpect(content().string("Updated"));
    }

    @Test
    @WithMockUser
    void testDeleteReview() throws Exception {
        Mockito.when(reviewService.deleteReview(1L)).thenReturn("Deleted");

        mockMvc.perform(delete("/review/deletereview/1")
                        .with(csrf())) // CSRF required for DELETE
               .andExpect(status().isOk())
               .andExpect(content().string("Deleted"));
    }

    @Test
    @WithMockUser
    void testGetPlaceReviews() throws Exception {
        List<ReviewDTO> reviews = Arrays.asList(new ReviewDTO(), new ReviewDTO());

        Mockito.when(reviewService.fetchReviews(1L)).thenReturn(reviews);

        mockMvc.perform(get("/review/place-reviews/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void testGetPlaceReviews_notFound() throws Exception {
        Mockito.when(reviewService.fetchReviews(1L)).thenReturn(List.of());

        mockMvc.perform(get("/review/place-reviews/1"))
               .andExpect(status().isNotFound());
    }
}
*/