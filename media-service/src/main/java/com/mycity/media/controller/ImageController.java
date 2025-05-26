package com.mycity.media.controller;

import java.util.List;

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

import com.mycity.media.service.ImageService;
import com.mycity.shared.mediadto.AboutPlaceImageDTO;
import com.mycity.shared.mediadto.ImageDTO;

import jakarta.ws.rs.core.MediaType;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/media")
public class ImageController {

	@Autowired
	private ImageService imageService;

	@PostMapping(value = "/upload/places", consumes = MediaType.MULTIPART_FORM_DATA)
	public ResponseEntity<String> uploadImageForPlaces(

			@RequestPart("image") MultipartFile file, @RequestParam Long placeId, @RequestParam String placeName,
			@RequestParam String category, @RequestParam String imageName) {

		System.out.println("ImageController.uploadImageForPlaces()");
		imageService.uploadImageForPlaces(file, placeId, placeName, category, imageName);

		return ResponseEntity.ok("Image uploaded successfully");
	}

	@GetMapping("/fetch/{id}")
	public ResponseEntity<ImageDTO> getImage(@PathVariable Long id) {
		ImageDTO imageDTO = imageService.fetchImage(id);
		return ResponseEntity.ok(imageDTO);
	}

	@GetMapping("/bycategory/image")
	public Mono<ResponseEntity<String>> getCoverImageForCategory(@RequestParam String category) {
		return imageService.getFirstImageUrlByCategory(category).doOnNext(imageUrl -> {
			// Log the image URL before returning it to make sure it's correct
			System.out.println("Image URL in controller: " + imageUrl);
		}).map(imageUrl -> imageUrl != null ? ResponseEntity.ok(imageUrl) : ResponseEntity.notFound().build()); // Return
																												// 404
																												// if
																												// image
																												// URL
																												// is
																												// null
	}

	@GetMapping("/images/{placeId}")
	public ResponseEntity<List<AboutPlaceImageDTO>> getAboutPlaceImages(@PathVariable Long placeId) {
		List<AboutPlaceImageDTO> images = imageService.getAboutPlaceImages(placeId);
		return ResponseEntity.ok(images);
	}

	@DeleteMapping("/images/delete/{placeId}")
	public ResponseEntity<String> deleteImage(@PathVariable Long placeId) {
		String result = imageService.deleteImage(placeId);
		return ResponseEntity.ok(result);

	}

	@GetMapping("/findby/place")
	public Mono<ResponseEntity<List<String>>> getImagesByPlaceId(@RequestParam Long placeId) {
		return imageService.getImagesByPlaceId(placeId).map(imageUrls -> {
			if (imageUrls.isEmpty()) {
				return ResponseEntity.noContent().<List<String>>build();
			} else {
				return ResponseEntity.ok(imageUrls);
			}
		}).defaultIfEmpty(ResponseEntity.noContent().<List<String>>build());
	}

	@GetMapping("/image-byplacename/{placeName}")
	public ResponseEntity<List<AboutPlaceImageDTO>> getAboutPlaceImagesForTop10Des(@PathVariable String placeName) {
		System.out.println("Fetching images for placeName: " + placeName);

		List<AboutPlaceImageDTO> images = imageService.getAboutPlaceImages(placeName);
		return ResponseEntity.ok(images);
	}
}
