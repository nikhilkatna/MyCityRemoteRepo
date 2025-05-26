package com.mycity.media.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.media.service.ReviewImageService;

import jakarta.ws.rs.core.MediaType;

@RestController
@RequestMapping("/media")
public class ReviewImageController 
{
  @Autowired
  private ReviewImageService service;
  
  @PostMapping(value="/upload",consumes =MediaType.MULTIPART_FORM_DATA)
  public ResponseEntity<String> addImageToReview(
		           @RequestPart ("image") MultipartFile file,
		           @RequestParam Long reviewId,
		           @RequestParam Long placeId,
		           @RequestParam String placeName,
		           @RequestParam Long userId,
		           @RequestParam String userName)
  { 
	  service.uploadReviewImage(file,reviewId, placeId, placeName, userId, userName);
	  return ResponseEntity.ok("Images Uploaded Successfully.");
  }
  
  @DeleteMapping("/delete/{reviewId}")
  public ResponseEntity<String> deleteImage(@PathVariable Long reviewId)
  {
	  return ResponseEntity.ok(service.deleteReviewImage(reviewId));
  }
  @GetMapping("/review/image/place/{placeId}")
  public String getImagesPlaceReview(@PathVariable long placeId) {
       return service.getReviewForPlace(placeId);
     
  }
  
  
}
