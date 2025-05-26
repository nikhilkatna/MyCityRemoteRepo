package com.mycity.place.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.place.entity.Place;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;
import com.mycity.shared.placedto.PlaceWithImagesDTO;

import reactor.core.publisher.Flux;

public interface PlaceServiceInterface {
	String addPlace(PlaceDTO dto);

	ResponseEntity<PlaceResponseDTO> getPlace(Long placeId);

	ResponseEntity<String> updatePlace(Long placeId, PlaceDTO dto);

	ResponseEntity<String> deletePlace(Long placeId);

	Place savePlace(Place place);

	PlaceDTO getPlaceById(Long id);

	ResponseEntity<List<PlaceResponseDTO>> getAllPlaces();

	List<PlaceCategoryDTO> getAllDistinctCategories();

	String addPlace(PlaceDTO placeDto, List<MultipartFile> images);


	Long getPlaceIdByName(String placeName);


	String addPlace(PlaceDTO placeDto, List<MultipartFile> placeImages, Map<String, MultipartFile> cuisineImages);

	String addPlace(PlaceDTO placeDto, Map<String, MultipartFile> placeImages, Map<String, MultipartFile> cuisineImages
			);

	List<PlaceWithImagesDTO> getPlacesByCategoryWithImages(String categoryName);

	Flux<PlaceResponseDTO> getPlacesByCategoryId(String categoryId);


}