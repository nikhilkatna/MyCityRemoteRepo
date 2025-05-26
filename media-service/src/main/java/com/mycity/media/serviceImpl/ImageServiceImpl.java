package com.mycity.media.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.media.entity.CuisineImages;
import com.mycity.media.entity.Images;
import com.mycity.media.helper.CloudinaryHelper;
import com.mycity.media.repository.ImageServiceRepository;
import com.mycity.media.service.ImageService;
import com.mycity.shared.mediadto.AboutPlaceImageDTO;
import com.mycity.shared.mediadto.ImageDTO;

import reactor.core.publisher.Mono;

@Service
public class ImageServiceImpl implements ImageService {

    @Autowired
    private CloudinaryHelper cloudinaryHelper;

    @Autowired
    private ImageServiceRepository imageServiceRepository;

    @Override
	public void uploadImageForPlaces(MultipartFile file, Long placeId, String placeName, String category,
			String imageName) {
 
		String imageUrl = cloudinaryHelper.saveImage(file); // your Cloudinary logic
 
		Images img = new Images();
		img.setImageUrl(imageUrl);
		img.setPlaceId(placeId);
		img.setPlaceName(placeName);
		img.setCategory(category);
		img.setImageName(imageName);
 
		imageServiceRepository.save(img);
	}
   
    @Override
	public void uploadImageForCuisines(MultipartFile file, Long placeId, Long cuisineId, String placeName,
			String category, String cuisineName) {
		// Upload to Cloudinary (your existing logic)
		String imageUrl = cloudinaryHelper.saveImage(file);

		// Create and populate CuisineImages entity
		CuisineImages cuisineImage = new CuisineImages();
		cuisineImage.setCuisineId(cuisineId); // ✅ NEW: Set cuisine ID
		cuisineImage.setImageUrl(imageUrl);
		cuisineImage.setPlaceId(placeId);
		cuisineImage.setPlaceName(placeName);
		cuisineImage.setCategory(category);
		cuisineImage.setCuisineName(cuisineName);
		try {
//			cuisineImageRepository.save(cuisineImage);
			System.out.println("✅ Uploaded and saved image for cuisineId: " + cuisineId);
		} catch (Exception e) {
			System.err.println("❌ Failed to save cuisine image: " + e.getMessage());
		}

	}
    

    @Override
	public ImageDTO fetchImage(Long imageId) {
		Images image = imageServiceRepository.findById(imageId)
				.orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));

		return new ImageDTO(image.getImageId(), image.getImageUrl(), image.getCategory(), image.getPlaceName(),
				image.getPlaceId());
	}
    public Mono<String> getFirstImageUrlByCategory(String category) {
        return Mono.defer(() -> {
            Optional<Images> imageOptional = imageServiceRepository
                    .findFirstByCategoryIgnoreCaseOrderByImageIdAsc(category);
            
            // Log the result for debugging
            if (imageOptional.isEmpty()) {
                System.out.println("No image found for category: " + category);
            } else {
                System.out.println("Found image URL: " + imageOptional.get().getImageUrl());
            }

            return Mono.justOrEmpty(imageOptional.map(Images::getImageUrl).orElse(null));
        });
    }



    @Override
	public List<AboutPlaceImageDTO> getAboutPlaceImages(Long placeId) {
	    // Fetch all images associated with the placeId
	    List<Images> images = imageServiceRepository.findImagesByPlaceId(placeId);

	    // Map to DTOs
	    List<AboutPlaceImageDTO> imageDTOs = new ArrayList<>();
	    for (Images image : images) {
	        AboutPlaceImageDTO dto = new AboutPlaceImageDTO();
	        dto.setImageUrl(image.getImageUrl());   // Assuming Images has getImageUrl()
	        dto.setImageName(image.getImageName()); // Assuming Images has getImageName()
	        imageDTOs.add(dto);
	    }

	    return imageDTOs;
	}

    @Override
    public String deleteImage(Long placeId)
    {
        // Get all image IDs associated with the given placeId
        List<Long> imageIds = imageServiceRepository.findImageIdsByPlaceId(placeId);
        
        // Check if there are any images associated with the placeId
        if (imageIds.isEmpty()) {
            throw new IllegalArgumentException("No images found for PlaceId: " + placeId);
        }

        // using flag to check if any image was deleted
        boolean isDeleted = false;
 
        //deleting each image
        for (Long imageId : imageIds) {
            Optional<Images> opt = imageServiceRepository.findById(imageId);

            // Check if the image exists or not  before deleting
            if (opt.isPresent()) {
                imageServiceRepository.deleteById(imageId);
                isDeleted = true;
            } else {

               throw new IllegalArgumentException("Images With Id "+imageId+" not found");
            }
        }

        // Return a success message if any image was deleted
        if (isDeleted) {
            return "Images with PlaceId " + placeId + " have been deleted.";
        } else {
            throw new IllegalArgumentException("No images found to delete for PlaceId: " + placeId);
        }
    }

	@Override
	public List<String> getAllImageUrlsByCategory(String category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AboutPlaceImageDTO> getAboutPlaceImages(String placeName) {
		  // Fetch all images associated with the placeId
	    List<Images> images = imageServiceRepository.findImagesByPlaceName(placeName);
	    System.out.println("Fetching images for placeName: " + placeName);

	    // Map to DTOs
	    List<AboutPlaceImageDTO> imageDTOs = new ArrayList<>();
	    for (Images image : images) {
	    	if (image.getImageName().toLowerCase().contains("main")) {
	        	System.out.println("Fetching images for placeName: " + image.getImageName());

	            AboutPlaceImageDTO dto = new AboutPlaceImageDTO();
	            dto.setImageUrl(image.getImageUrl());   // Assuming Images has getImageUrl()
	            dto.setImageName(image.getImageName()); // Assuming Images has getImageName()
	            imageDTOs.add(dto);
	        }
	    }

	    return imageDTOs;
	}

	@Override
	public Mono<List<String>> getImagesByPlaceId(Long placeId) {
	    return Mono.fromCallable(() -> {
	        List<Images> images = imageServiceRepository.findByPlaceId(placeId);
	        return images.stream()
	                     .map(Images::getImageUrl)
	                     .collect(Collectors.toList());
	    });
	}
	


}
