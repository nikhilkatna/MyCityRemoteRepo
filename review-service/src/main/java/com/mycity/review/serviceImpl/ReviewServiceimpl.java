package com.mycity.review.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.mycity.review.entity.Review;
import com.mycity.review.exception.InvalidPlaceException;
import com.mycity.review.exception.InvalidUserException;
import com.mycity.review.exception.MediaServiceException;
import com.mycity.review.exception.PlaceServiceUnavailableException;
import com.mycity.review.exception.ReviewNotFoundException;
import com.mycity.review.exception.UserServiceUnavailableException;
import com.mycity.review.repository.ReviewRepository;
import com.mycity.review.service.ReviewServiceInterface;
import com.mycity.shared.reviewdto.ReviewDTO;
import com.mycity.shared.reviewdto.ReviewSummaryDTO;

import reactor.core.publisher.Mono;

@Service
public class ReviewServiceimpl implements ReviewServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceimpl.class);

    @Autowired
    private ReviewRepository reviewRepo;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private WebClientMediaService mediaService;

    private static final String PLACE_SERVICE_NAME = "place-service";
    private static final String PATH_TO_FIND_PLACEID = "/place/getplaceid/{placeName}";
    private static final String USER_SERVICE_NAME = "user-service";
    private static final String PATH_TO_FIND_USERID = "/user/getuserId/{userName}";

    @Override
    public ResponseEntity<String> addPlaceReview(ReviewDTO dto, List<MultipartFile> images) {
        logger.info("Adding review for place: {}", dto.getPlaceName());
        validateReviewDTO(dto);

        Review review = new Review();
        review.setPlaceName(dto.getPlaceName());
        review.setReviewDescription(dto.getReviewDescription());
        review.setUserName(dto.getUserName());
        review.setPostedOn(LocalDate.now());

        try {
            // Validate Place and User
            Long placeId = getPlaceId(dto.getPlaceName());
            Long userId = getUserId(dto.getUserName()); // Here we validate if the username is correct

            // If userId is null or doesn't exist, it means the username is incorrect
            if (userId == null) {
                throw new InvalidUserException("User with username " + dto.getUserName() + " does not exist.");
            }

            review.setPlaceId(placeId);
            review.setUserId(userId);

            Review savedReview = reviewRepo.save(review);
            logger.info("Review saved with ID: {}", savedReview.getReviewId());

            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    // Handle media upload for the review
                    mediaService.uploadImageForReview(
                            image,
                            savedReview.getReviewId(),
                            savedReview.getPlaceId(),
                            savedReview.getPlaceName(),
                            savedReview.getUserId(),
                            savedReview.getPlaceName()
                    );
                }
                logger.info("Images uploaded for reviewId: {}", savedReview.getReviewId());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("Review saved.");

        } catch (InvalidPlaceException | InvalidUserException e) {
            logger.warn("Validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (PlaceServiceUnavailableException | UserServiceUnavailableException | MediaServiceException e) {
            logger.error("External service failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());

        } catch (Exception e) {
            logger.error("Internal error while saving review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong while saving the review.");
        }
    }


    private Long getPlaceId(String placeName) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("lb://" + PLACE_SERVICE_NAME + PATH_TO_FIND_PLACEID, placeName)
                    .exchangeToMono(response -> handleServiceResponse(response, placeName, "place"))
                    .block();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PlaceServiceUnavailableException("Failed to fetch placeId: " + e.getMessage());
        }
    }

    private Long getUserId(String userName) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("lb://" + USER_SERVICE_NAME + PATH_TO_FIND_USERID, userName)
                    .exchangeToMono(response -> handleServiceResponse(response, userName, "user"))
                    .block();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceUnavailableException("Failed to fetch userId: " + e.getMessage());
        }
    }

    private Mono<Long> handleServiceResponse(ClientResponse response, String value, String type) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(Long.class);
        } else if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
            if ("place".equals(type)) {
                throw new InvalidPlaceException("Place '" + value + "' is not present.");
            } else {
                throw new InvalidUserException("User '" + value + "' is not present.");
            }
        } else {
            if ("place".equals(type)) {
                throw new PlaceServiceUnavailableException("Place service returned an error.");
            } else {
                throw new UserServiceUnavailableException("User service returned an error.");
            }
        }
    }

    @Override
    public ResponseEntity<String> updateReview(Long reviewId, ReviewDTO dto) {
        logger.info("Updating review with ID: {}", reviewId);
        Optional<Review> opt = reviewRepo.findById(reviewId);

        if (opt.isPresent()) {
            Review review = opt.get();

            if (dto.getReviewDescription() != null && !dto.getReviewDescription().trim().isEmpty()) {
                review.setReviewDescription(dto.getReviewDescription());
            } else {
                logger.warn("Invalid review description for update on ID: {}", reviewId);
                return ResponseEntity.badRequest().body("Review description must not be empty.");
            }

            reviewRepo.save(review);
            logger.info("Review updated successfully for ID: {}", reviewId);
            return ResponseEntity.ok("Review With Id " + reviewId + " Updated.");
        } else {
            logger.warn("Review not found: {}", reviewId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Review not found with ID " + reviewId);
        }
    }


    @Override
    public ResponseEntity<List<ReviewSummaryDTO>> getUserReview(Long placeId) {
        logger.info("Fetching reviews for placeId: {}", placeId);
        List<Review> reviews = reviewRepo.findByPlaceId(placeId);
        List<ReviewSummaryDTO> dtos = new ArrayList<>();

        if (reviews.isEmpty()) {
            logger.warn("No reviews found for placeId: {}", placeId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(dtos); // Return empty list with 200 OK
        }

        for (Review r : reviews) {
            ReviewSummaryDTO dto = new ReviewSummaryDTO();
            dto.setReviewDescription(r.getReviewDescription());
            dto.setPlaceName(r.getPlaceName());
            dto.setUserName(r.getUserName());
            dto.setPostedOn(r.getPostedOn());
            dto.setRating(null); // placeholder if rating needed
            dto.setUserImageUrl(mediaService.getImageUrlByPlaceId(r.getPlaceId()));
            dtos.add(dto);
            
            System.out.println("Dto ::"+dto);
        }

        logger.info("Total reviews found for placeId {}: {}", placeId, dtos.size());
        return ResponseEntity.ok(dtos);
    }



    @Override
    public ResponseEntity<String> deleteReview(Long reviewId) {
        logger.info("Deleting review with ID: {}", reviewId);
        Optional<Review> opt = reviewRepo.findById(reviewId);

        if (opt.isPresent()) {
            reviewRepo.deleteById(reviewId);
            String result = mediaService.deleteImagesForReviews(reviewId);
            logger.info("Review and associated images deleted successfully");
            // Return success message with 200 OK
            return ResponseEntity.ok("Review & " + result);
        } else {
            logger.warn("Review not found for deletion: {}", reviewId);
            // Return 404 Not Found with message instead of throwing exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Review not found with ID " + reviewId);
        }
    }


    @Override
    public List<ReviewDTO> fetchReviews(Long placeId) {
        logger.info("Fetching full review details for placeId: {}", placeId);
        List<Review> reviews = reviewRepo.findByPlaceId(placeId);
        List<ReviewDTO> dtos = new ArrayList<>();

        for (Review r : reviews) {
            ReviewDTO dto = new ReviewDTO();
            dto.setPlaceName(r.getPlaceName());
            dto.setReviewDescription(r.getReviewDescription());
            dto.setUserName(r.getUserName());
            dto.setPostedOn(r.getPostedOn());
            dto.setImageUrl(mediaService.getImageUrlByPlaceId(r.getPlaceId()));
            dtos.add(dto);
        }

        logger.info("Fetched {} reviews for placeId {}", dtos.size(), placeId);
        return dtos;
    }

    @Override
    public void validateReviewDTO(ReviewDTO dto) {
        logger.debug("Validating ReviewDTO: {}", dto);
        if (dto.getUserName() == null || dto.getUserName().trim().isEmpty()) {
            throw new InvalidUserException("UserName cannot be blank.");
        }
        if (dto.getPlaceName() == null || dto.getPlaceName().trim().isEmpty()) {
            throw new InvalidPlaceException("PlaceName cannot be blank.");
        }
        if (dto.getReviewDescription() == null || dto.getReviewDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Review description cannot be empty.");
        }
    }
}
