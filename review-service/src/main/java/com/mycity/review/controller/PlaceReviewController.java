package com.mycity.review.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.review.service.ReviewServiceInterface;
import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

@RestController
@RequestMapping("/review")
public class PlaceReviewController {
	@Autowired
	private ReviewServiceInterface service;

	@PostMapping(value = "/addreview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> addReview(@RequestPart ReviewDTO dto,
			@RequestPart("images") List<MultipartFile> images) {
		ResponseEntity<String> response = service.addPlaceReview(dto, images);
		return response; // Use the status code returned by the service
	}

	@PutMapping("/updatereview/{reviewId}")
	public ResponseEntity<String> updateReview(@PathVariable Long reviewId, @RequestBody ReviewDTO dto) {
		// use service
		ResponseEntity<String> response=service.updateReview(reviewId, dto);
		return response;
	}

	@GetMapping(value="/getreviews/{placeId}")
	public ResponseEntity<List<ReviewSummaryDTO>> getAllReviews(@PathVariable Long placeId) // gives list of review's
																							// for a Particular place
	{
		//use service
		ResponseEntity<List<ReviewSummaryDTO>> response=service.getUserReview(placeId);
		return response;
	}

	@DeleteMapping("/deletereview/{reviewId}")
	public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
		ResponseEntity<String> response=service.deleteReview(reviewId);
		return response;
		
	}

	@GetMapping("/place-reviews/{placeId}")
	public ResponseEntity<List<ReviewDTO>> getPlaceReviews(@PathVariable Long placeId) {
		// Fetch the reviews directly (assuming fetchReviews is synchronous or already
		// handles async properly)
		List<ReviewDTO> reviews = service.fetchReviews(placeId);

		// Check if reviews are found
		if (reviews != null && !reviews.isEmpty()) {
			// Return 200 OK with the reviews if found
			return ResponseEntity.ok(reviews);
		} else {
			// Return 404 Not Found if no reviews are found
			return ResponseEntity.status(404).body(null);
		}
	}

}
