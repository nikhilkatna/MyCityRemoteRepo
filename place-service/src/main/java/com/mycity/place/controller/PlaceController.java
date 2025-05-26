package com.mycity.place.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.place.exception.GlobalExceptionHandler;
import com.mycity.place.service.PlaceServiceInterface;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;
import com.mycity.shared.placedto.PlaceWithImagesDTO;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/place")
public class PlaceController {

    private final GlobalExceptionHandler globalExceptionHandler;

	@Autowired
	private PlaceServiceInterface placeService;

    PlaceController(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

 // Endpoint to add Place details
    @PostMapping(value = "/add-place", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addPlaceDetails(
            @RequestPart PlaceDTO placeDto,  // Bind the form data to the PlaceDTO
            @RequestParam Map<String, MultipartFile>  placeImages,  // Images for the Place itself
            @RequestParam Map<String, MultipartFile>  cuisineImages  // Images for Cuisines
    ) {
        System.out.println("PlaceController.addPlaceDetails()");
        System.out.println(placeDto.getCategoryName());

        try {
            // Call service method to add the Place and save images for cuisines and hotels
            String msg = placeService.addPlace(placeDto, placeImages, cuisineImages);
            return new ResponseEntity<>(msg, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error adding place with images", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    



	

	// Create a place using PlaceDTO
	@PostMapping("/newplace/add")
	public ResponseEntity<String> addPlaceDetails(@RequestBody PlaceDTO dto) {
		String msg = placeService.addPlace(dto);
		return new ResponseEntity<>(msg, HttpStatus.CREATED);
	}

	// Get place by ID (Place entity)
	@GetMapping("/get/{placeId}")
	public ResponseEntity<PlaceResponseDTO> getPlaceDetails(@PathVariable Long placeId) {
		ResponseEntity<PlaceResponseDTO> response = placeService.getPlace(placeId);
		return response;
	}

	// Update place using PlaceDTO
	@PutMapping("/update/{placeId}")
	public ResponseEntity<String> updatePlaceDetails(@PathVariable Long placeId, @RequestBody PlaceDTO dto) {
		ResponseEntity<String> response = placeService.updatePlace(placeId, dto);
		return response;
	}

	// Delete place
	@DeleteMapping("/delete/{placeId}")
	public ResponseEntity<String> deletePlaceDetails(@PathVariable Long placeId) {
		ResponseEntity<String> response = placeService.deletePlace(placeId);
		return response;
	}



	@GetMapping("/allplaces")
	public ResponseEntity<List<PlaceResponseDTO>> getAllPlaces() {
		ResponseEntity<List<PlaceResponseDTO>> response=placeService.getAllPlaces();
		return response;
 	}

	@GetMapping("/placeby/{id}")
	public ResponseEntity<PlaceDTO> getPlaceById(@PathVariable Long id) {
		PlaceDTO place = placeService.getPlaceById(id);
		return place != null ? ResponseEntity.ok(place) : ResponseEntity.notFound().build();
	}

	@GetMapping("/placeby/categories")
	public ResponseEntity<List<PlaceCategoryDTO>> getAllDistinctCategories() {
		List<PlaceCategoryDTO> categories = placeService.getAllDistinctCategories();
		return ResponseEntity.ok(categories);
	}
	
	@GetMapping("/getplaceid/{placeName}")
	public ResponseEntity<Long> getPlaceIdByName(@PathVariable String placeName) {
	    Long placeId = placeService.getPlaceIdByName(placeName);
	    if (placeId != null) {
	        return new ResponseEntity<>(placeId, HttpStatus.OK);
	    } else {
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	    }
	}
	
	@GetMapping("/places-by-category/{categoryName}")
	public ResponseEntity<List<PlaceWithImagesDTO>> getPlacesByCategory(@PathVariable String categoryName) {
	    List<PlaceWithImagesDTO> places = placeService.getPlacesByCategoryWithImages(categoryName);
	    return ResponseEntity.ok(places);
	}
	
	@GetMapping("/bycategory/{categoryId}")
	public Flux<PlaceResponseDTO> getPlacesByCategoryId(@PathVariable String categoryId) {
	    return placeService.getPlacesByCategoryId(categoryId);
	}

	


	
	
}

