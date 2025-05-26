package com.mycity.place.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.place.entity.Coordinate;
import com.mycity.place.entity.LocalCuisine;
import com.mycity.place.entity.Place;
import com.mycity.place.entity.TimeZone;
import com.mycity.place.repository.PlaceRepository;
import com.mycity.place.service.PlaceServiceInterface;
import com.mycity.shared.categorydto.CategoryDTO;
import com.mycity.shared.placedto.LocalCuisineDTO;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;
import com.mycity.shared.placedto.PlaceWithImagesDTO;
import com.mycity.shared.timezonedto.TimezoneDTO;
import com.mycity.shared.tripplannerdto.CoordinateDTO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class PlaceServiceImpl implements PlaceServiceInterface {

	@Autowired
	private PlaceRepository placeRepo;

	@Autowired
	private WebClientMediaService mediaService;

	@Autowired
	private WebClientCategoryService categoryService;

	@Autowired
	private WebClientLocationService clientLocationService;

	
	@Override
	@Transactional
	public String addPlace(PlaceDTO dto, List<MultipartFile> images) {
		// Validate incoming DTO
		validatePlaceDTO(dto);

		// Build Coordinate entity
		Coordinate coordinate = new Coordinate();
		coordinate.setLatitude(dto.getCoordinate().getLatitude());
		coordinate.setLongitude(dto.getCoordinate().getLongitude());

		// Build Place entity
		Place place = new Place();
		place.setPlaceName(dto.getPlaceName().trim());
		place.setPlaceHistory(dto.getPlaceHistory());
		place.setAboutPlace(dto.getAboutPlace());
		place.setPlaceDistrict(dto.getPlaceDistrict());
		place.setRating(dto.getRating());
		place.setCoordinate(coordinate);
		place.setCategoryName(dto.getCategoryName());

		// Set PostedOn date
		if (dto.getPostedOn() != null) {
			place.setPostedOn(dto.getPostedOn());
		} else {
			place.setPostedOn(LocalDate.now()); // fallback to current date if not provided
		}

		// Save Place first to get the generated placeId
		Place savedPlace = placeRepo.save(place);
		placeRepo.flush(); // Force insert to get generated ID immediately

		// Build CategoryDTO using saved Place info
		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName(savedPlace.getCategoryName());
		categoryDTO.setPlaceName(savedPlace.getPlaceName());
		categoryDTO.setPlaceId(savedPlace.getPlaceId());
		categoryDTO.setDescription(dto.getPlaceCategoryDescription());

		// Create/save Category
		CategoryDTO createdCategory = categoryService.saveCategory(categoryDTO);

		// Update Place with Category ID and Category Name
		savedPlace.setCategoryId(createdCategory.getCategoryId());
		savedPlace.setCategoryName(createdCategory.getName());

		// Save updated Place (with categoryId)
		placeRepo.save(savedPlace);

		// Handle Timezone if provided
		TimezoneDTO timezoneDTO = dto.getTimeZone();
		if (timezoneDTO != null) {
			TimeZone timezone = new TimeZone();
			timezone.setOpeningTime(timezoneDTO.getOpeningTime());
			timezone.setClosingTime(timezoneDTO.getClosingTime());
			timezone.setPlace(savedPlace); // Bidirectional

			savedPlace.setTimeZone(timezone);

			placeRepo.save(savedPlace);
		}

		// Handle Image Uploads if images are provided
		if (images != null && !images.isEmpty()) {
			for (MultipartFile image : images) {
				mediaService.uploadImageForPlace(image, savedPlace.getPlaceId(), savedPlace.getPlaceName(),
						savedPlace.getCategoryName(), dto.getImageName());
			}
		}

		return "Place with name '" + savedPlace.getPlaceName() + "' saved successfully, and image uploads initiated.";
	}

	@Override
	@Transactional
	public ResponseEntity<PlaceResponseDTO> getPlace(Long placeId) {
	    if (placeId == null || placeId <= 0) {
	        return ResponseEntity.badRequest().build(); // or include a body with error message if needed
	    }

	    Optional<Place> optionalPlace = placeRepo.findById(placeId);

	    if (optionalPlace.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
	    }

	    Place place = optionalPlace.get();
	    PlaceResponseDTO dto=convertToDTO(place);
	    return ResponseEntity.ok(dto); // Assuming convertToDTO returns ResponseEntity<PlaceResponseDTO>
	}

	@Override
	@Transactional
	public ResponseEntity<String> updatePlace(Long placeId, PlaceDTO dto) {
	    try {
	        // Validate incoming DTO
	        validatePlaceDTO(dto);

	        // Fetch existing place or return 404
	        Optional<Place> optionalPlace = placeRepo.findById(placeId);
	        if (optionalPlace.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body("Place with ID " + placeId + " not found.");
	        }

	        Place place = optionalPlace.get();

	        // Update place fields
	        place.setPlaceName(dto.getPlaceName().trim());
	        place.setAboutPlace(dto.getAboutPlace());
	        place.setPlaceHistory(dto.getPlaceHistory());
	        place.setPlaceDistrict(dto.getPlaceDistrict());
	        place.setRating(dto.getRating());

	        // Update category
	        CategoryDTO category = getCategory(dto);
	        place.setCategoryId(category.getCategoryId());
	        place.setCategoryName(category.getName());

	        // Update coordinates
	        if (dto.getCoordinate() != null) {
	            Coordinate coordinate = place.getCoordinate();
	            if (coordinate == null) {
	                coordinate = new Coordinate();
	            }
	            coordinate.setLatitude(dto.getCoordinate().getLatitude());
	            coordinate.setLongitude(dto.getCoordinate().getLongitude());
	            place.setCoordinate(coordinate);
	        }

	        // Update timezone
	        if (dto.getTimeZone() != null) {
	            TimeZone timezone = place.getTimeZone();
	            if (timezone == null) {
	                timezone = new TimeZone();
	                timezone.setPlace(place);
	            }
	            timezone.setOpeningTime(dto.getTimeZone().getOpeningTime());
	            timezone.setClosingTime(dto.getTimeZone().getClosingTime());
	            place.setTimeZone(timezone);
	        }

	        // Save updated place
	        placeRepo.save(place);

	        return ResponseEntity.ok("Place with ID " + placeId + " updated successfully.");
	    } catch (IllegalArgumentException ex) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body("Invalid data: " + ex.getMessage());
	    } catch (Exception ex) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("An unexpected error occurred: " + ex.getMessage());
	    }
	}


	@Override
	@Transactional
	public ResponseEntity<String> deletePlace(Long placeId) {
		try
		{
			//Check Whether the With Given PlaceId Exists or not 
		    Optional<Place> opt=placeRepo.findById(placeId);
		    if(opt.isPresent())
		    {
		    	placeRepo.deleteById(placeId);
		    	String message = "Place with name: " + opt.get().getPlaceName() + " has been deleted.";
	            log.info(message);
	            return ResponseEntity.ok(message);
		    }
		    else
		    {
		       String message="Place With Given Place Id :"+placeId+" Not Found";
		       log.warn(message);
		       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
		    }
		}
		
		catch (DataAccessException dae) 
		{
	        log.error("Database error while deleting place with ID " + placeId, dae);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Error deleting place due to database issues.");
	    }
		catch(Exception e )
		{
			 log.error("Unexpected error while deleting place with ID " + placeId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An Unexpected Error Occured While Deleting the Place");
		}
		
	}

	@Override
	public ResponseEntity<List<PlaceResponseDTO>> getAllPlaces() {
	    try {
	        List<Place> places = placeRepo.findAll();

	        if (places.isEmpty()) {
	            // No content found
	            return ResponseEntity.noContent().build(); // HTTP 204
	        }

	        List<PlaceResponseDTO> dtoList = places.stream()
	                                               .map(this::convertToDTO)
	                                               .collect(Collectors.toList());

	        return ResponseEntity.ok(dtoList); // HTTP 200
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500
	    }
	}



	@Override
	public List<PlaceCategoryDTO> getAllDistinctCategories() {
		return placeRepo.findPlaceIdsAndCategories();
	}

	@Override
	public Long getPlaceIdByName(String placeName) {
		return placeRepo.findPlaceIdByPlaceName(placeName);
	}

	private CategoryDTO getCategory(PlaceDTO dto) {
		CategoryDTO category = categoryService.getCategoryByName(dto.getCategoryName());
		if (category == null) {
			category = categoryService.createCategory(dto.getCategoryName(), dto.getPlaceCategoryDescription());
		}
		return category;
	}

	private PlaceResponseDTO convertToDTO(Place place) {
		
		PlaceResponseDTO dto = new PlaceResponseDTO();
		dto.setPlaceId(place.getPlaceId());
		dto.setPlaceName(place.getPlaceName());
		dto.setAboutPlace(place.getAboutPlace());
		dto.setPlaceHistory(place.getPlaceHistory());
		dto.setCategoryId(place.getCategoryId());
		dto.setCategoryName(place.getCategoryName());
		dto.setPlaceDistrict(place.getPlaceDistrict());
		dto.setRating(place.getRating());
		dto.setClosingTime(place.getTimeZone().getClosingTime());
		dto.setOpeningTime(place.getTimeZone().getOpeningTime());

		if (place.getCoordinate() != null) {
			dto.setLatitude(place.getCoordinate().getLatitude());
			dto.setLongitude(place.getCoordinate().getLongitude());
		}

		if (place.getTimeZone() != null) {
			dto.setOpeningTime(place.getTimeZone().getOpeningTime());
			dto.setClosingTime(place.getTimeZone().getClosingTime());
		}

		return dto;
	}

	private void validatePlaceDTO(PlaceDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Place data must not be null");
		}
		if (isNullOrEmpty(dto.getPlaceName())) {
			throw new IllegalArgumentException("Enter Place Name");
		}
		if (isNullOrEmpty(dto.getAboutPlace())) {
			throw new IllegalArgumentException("Mention About the Place");
		}
		if (isNullOrEmpty(dto.getPlaceHistory())) {
			throw new IllegalArgumentException("Mention About the Place History");
		}
		if (isNullOrEmpty(dto.getPlaceDistrict())) {
			throw new IllegalArgumentException("Mention the District of the Place");
		}
		if (dto.getCoordinate() == null) {
			throw new IllegalArgumentException("Mention the Place Coordinates");
		}
		if (dto.getTimeZone() == null) {
			throw new IllegalArgumentException("Mention the Place TimeZone details");
		}
		if (dto.getRating() == null) {
			throw new IllegalArgumentException("Mention the Rating");
		}
	}

	private boolean isNullOrEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	@Override
	public Place savePlace(Place place) {
		return placeRepo.save(place);
	}

	@Override
	public PlaceDTO getPlaceById(Long id) {
				
		Place place = placeRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid Place Id.."));
	
		PlaceDTO p = new PlaceDTO();
		
		p.setPlaceId(place.getPlaceId());
		p.setPlaceName(place.getPlaceName());
		p.setPlaceHistory(place.getPlaceHistory());
		p.setPlaceDistrict(place.getPlaceDistrict());
		//p.setPlaceCategoryDescription(place.);
		p.setAboutPlace(place.getAboutPlace());
		//p.setLocalCuisines(place.getLocalCuisines());
		p.setCategoryName(place.getCategoryName());
		//p.setCoordinate(place.getCoordinate());
		
	return p;
	
	}

	@Override
	public String addPlace(PlaceDTO dto) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	@Transactional
	public String addPlace(PlaceDTO placeDto, Map<String, MultipartFile> placeImages,
			Map<String, MultipartFile> cuisineImages) {

		if (placeDto == null)
			throw new IllegalArgumentException("PlaceDTO cannot be null");

		if (placeDto.getPlaceName() == null || placeDto.getPlaceName().isBlank())
			throw new IllegalArgumentException("Place name is required");

		if (placeDto.getAboutPlace() == null || placeDto.getAboutPlace().isBlank())
			throw new IllegalArgumentException("About place is required");

		if (placeDto.getPlaceHistory() == null || placeDto.getPlaceHistory().isBlank())
			throw new IllegalArgumentException("Place history is required");

		if (placeDto.getCategoryName() == null || placeDto.getCategoryName().isBlank())
			throw new IllegalArgumentException("Category name is required");

		if (placeDto.getPlaceDistrict() == null || placeDto.getPlaceDistrict().isBlank())
			throw new IllegalArgumentException("District is required");

		// 🌍 Fetch coordinates
		CoordinateDTO coordinateDTO;
		try {
			coordinateDTO = clientLocationService.fetchCoordinatesByPlaceName(placeDto.getPlaceName()).get();
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch coordinates", e);
		}

		if (coordinateDTO == null || coordinateDTO.getLatitude() == 0.0 || coordinateDTO.getLongitude() == 0.0)
			throw new IllegalArgumentException("Invalid coordinates fetched");

		// 🏞️ Create Place
		Place place = new Place();
		place.setPlaceName(placeDto.getPlaceName());
		place.setAboutPlace(placeDto.getAboutPlace());
		place.setPlaceHistory(placeDto.getPlaceHistory());
		place.setPlaceDistrict(placeDto.getPlaceDistrict());
		place.setRating(placeDto.getRating());
		place.setCategoryName(placeDto.getCategoryName());
		place.setPostedOn(placeDto.getPostedOn() != null ? placeDto.getPostedOn() : LocalDate.now());

		// 🌐 Set Coordinate
		Coordinate coordinate = new Coordinate();
		coordinate.setLatitude(coordinateDTO.getLatitude());
		coordinate.setLongitude(coordinateDTO.getLongitude());
		place.setCoordinate(coordinate);

		// 🕒 Set TimeZone (Bidirectional)
		if (placeDto.getTimeZone() != null) {
			TimeZone timezone = new TimeZone();
			timezone.setOpeningTime(placeDto.getTimeZone().getOpeningTime());
			timezone.setClosingTime(placeDto.getTimeZone().getClosingTime());
			timezone.setPlace(place);
			place.setTimeZone(timezone);
		}

		// 🍜 Local Cuisines (Bidirectional)
		List<LocalCuisine> cuisineList = new ArrayList<>();
		if (placeDto.getLocalCuisines() != null) {
			for (LocalCuisineDTO cuisineDTO : placeDto.getLocalCuisines()) {
				LocalCuisine cuisine = new LocalCuisine();
				cuisine.setCuisineName(cuisineDTO.getCuisineName());
				cuisine.setPlace(place);
				cuisineList.add(cuisine);

				
			}
		}
		place.setLocalCuisines(cuisineList);


		// Save Place and flush to get generated ID
		Place savedPlace = placeRepo.save(place);
		placeRepo.flush();
		
		//add cuisine images
		if (placeDto.getLocalCuisines() != null && cuisineImages != null) {

		    // Convert DTOs to a map for lookup by cuisine name
		    Map<String, LocalCuisineDTO> cuisineMap = placeDto.getLocalCuisines().stream()
		            .collect(Collectors.toMap(LocalCuisineDTO::getCuisineName, Function.identity(), (a, b) -> a));

		    // Normalize cuisine image keys (remove extensions like jpg)
		    Map<String, MultipartFile> normalizedCuisineImages = new HashMap<>();
		    for (Map.Entry<String, MultipartFile> entry : cuisineImages.entrySet()) {
		        String originalKey = entry.getKey();
		        String normalizedKey = originalKey.contains(".") ? originalKey.substring(0, originalKey.lastIndexOf('.')) : originalKey;
		        normalizedCuisineImages.put(normalizedKey, entry.getValue());
		    }

		    for (LocalCuisine cuisine : savedPlace.getLocalCuisines()) {
		        String cuisineName = cuisine.getCuisineName();

		        if (cuisineMap.containsKey(cuisineName) && normalizedCuisineImages.containsKey(cuisineName)) {
		            MultipartFile cuisineImage = normalizedCuisineImages.get(cuisineName);

		            if (cuisineImage != null && !cuisineImage.isEmpty()) {
		                try {
		                    mediaService.uploadCuisineImage(
		                            cuisineImage,
		                            cuisineName,
		                            savedPlace.getPlaceId(),
		                            savedPlace.getPlaceName(),
		                            savedPlace.getCategoryName()
		                    );
		                    System.out.println("✅ Uploaded image for cuisine: " + cuisineName);
		                } catch (Exception e) {
		                    System.err.println("❌ Error uploading image for cuisine: " + cuisineName);
		                    e.printStackTrace();
		                }
		            } else {
		                System.out.println("⚠️ Empty or missing image for cuisine: " + cuisineName);
		            }
		        } else {
		            System.out.println("⚠️ No matching image found for cuisine: " + cuisineName);
		        }
		    }
		}




		// Save the updated place
		placeRepo.saveAndFlush(savedPlace);
		System.out.println("✅ Images saved for cuisines and hotels");



		// 🗂️ Save Category
		CategoryDTO categoryDTO = new CategoryDTO();
		categoryDTO.setName(savedPlace.getCategoryName());
		categoryDTO.setPlaceName(savedPlace.getPlaceName());
		categoryDTO.setPlaceId(savedPlace.getPlaceId());
		categoryDTO.setDescription(placeDto.getPlaceCategoryDescription());
		CategoryDTO createdCategory = categoryService.saveCategory(categoryDTO);

		// Update category info
		savedPlace.setCategoryId(createdCategory.getCategoryId());
		savedPlace.setCategoryName(createdCategory.getName());
		placeRepo.save(savedPlace);

		// 📸 Upload Place Images
		if (placeImages != null) {
			for (Map.Entry<String, MultipartFile> entry : placeImages.entrySet()) {
				MultipartFile image = entry.getValue();
				if (image != null && !image.isEmpty()) {
					mediaService.uploadImageForPlace(image, savedPlace.getPlaceId(), savedPlace.getPlaceName(),
							savedPlace.getCategoryName(), entry.getKey());
				}
			}
		}

		return "Place '" + savedPlace.getPlaceName() + "' added successfully with images and  cuisines";
	}

	@Override
	public String addPlace(PlaceDTO placeDto, List<MultipartFile> placeImages,
			Map<String, MultipartFile> cuisineImages) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<PlaceWithImagesDTO> getPlacesByCategoryWithImages(String categoryName) {
	    List<Place> places = placeRepo.findByCategoryName(categoryName);
	    String categoryDescription = categoryService.getCategoryDescription(categoryName);

	    return places.stream().map(place -> {
	        List<String> photoUrls = mediaService.getPhotoUrlsByPlaceId(place.getPlaceId());

	        return new PlaceWithImagesDTO(
	            place.getPlaceId(),
	            place.getPlaceName(),
	            place.getAboutPlace(),
	            place.getPlaceHistory(),
	            place.getTimeZone() != null ? place.getTimeZone().getOpeningTime() : null,
	            place.getTimeZone() != null ? place.getTimeZone().getClosingTime() : null,
	            place.getRating(),
	            place.getPlaceDistrict(),
	            place.getCoordinate() != null ? place.getCoordinate().getLatitude() : null,
	            place.getCoordinate() != null ? place.getCoordinate().getLongitude() : null,
	            photoUrls, //from media service
	            categoryDescription,//from category Description
	            place.getPostedOn(),
	            place.getCategoryName()
	        );
	    }).collect(Collectors.toList());
	}
	
	@Override
	public Flux<PlaceResponseDTO> getPlacesByCategoryId(String categoryId) {
	    Long categoryIdLong = Long.parseLong(categoryId); // ensure correct type
	    return Flux.fromIterable(placeRepo.findAllByCategoryId(categoryIdLong))
	               .map(this::convertToDTO); // convert to DTO
	}



}