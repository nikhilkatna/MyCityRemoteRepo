package com.mycity.review.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

public interface ReviewServiceInterface {
	
	
	ResponseEntity<String> addPlaceReview(ReviewDTO dto,List<MultipartFile> images);

	ResponseEntity<String> updateReview(Long reviewId, ReviewDTO dto);

	ResponseEntity<List<ReviewSummaryDTO>> getUserReview(Long placeId);

	ResponseEntity<String> deleteReview(Long reviewId);

	List<ReviewDTO> fetchReviews(Long placeId);

	void validateReviewDTO(ReviewDTO dto);
}
