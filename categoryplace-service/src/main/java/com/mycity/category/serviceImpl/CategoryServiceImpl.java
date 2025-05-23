package com.mycity.category.serviceImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mycity.category.entity.Category;
import com.mycity.category.exception.CategoryAlreadyExistsException;
import com.mycity.category.exception.CategoryNotFoundException;
import com.mycity.category.exception.CategoryOperationException;
import com.mycity.category.repository.CategoryRepository;
import com.mycity.category.service.CategoryService;
import com.mycity.shared.categorydto.CategoryDTO;
import com.mycity.shared.categorydto.CategoryImageDTO;
import com.mycity.shared.categorydto.CategoryWithPlacesDTO;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceRelatedImagesDTO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private WebClientPlaceService placeService;

    @Autowired
    private WebClientMediaService mediaService;

    @Autowired
    private CategoryRepository categoryRepo;

    @Override
    public Mono<List<CategoryImageDTO>> fetchCategoriesWithImages() {
        log.info("Fetching categories with images");
        return placeService.fetchPlaceCategories()
                .collectList()
                .flatMap(places -> {
                    Set<String> seenCategories = new HashSet<>();
                    List<PlaceCategoryDTO> uniquePlaces = new ArrayList<>();

                    for (PlaceCategoryDTO place : places) {
                        String normalized = place.getCategoryName().trim().toLowerCase();
                        if (!seenCategories.contains(normalized)) {
                            seenCategories.add(normalized);
                            uniquePlaces.add(place);
                        }
                    }

                    List<Mono<CategoryImageDTO>> dtoMonos = new ArrayList<>();
                    for (PlaceCategoryDTO place : uniquePlaces) {
                        String categoryName = place.getCategoryName();
                        String placeId = String.valueOf(place.getPlaceId());
                        String placeName = place.getPlaceName();

                        log.debug("Processing category: {}, placeId: {}, placeName: {}", categoryName, placeId, placeName);

                        Mono<String> imageUrlMono = mediaService.fetchCategoryImage(categoryName)
                                .doOnNext(imageUrl -> {
                                    if (imageUrl == null || imageUrl.isEmpty()) {
                                        log.warn("Empty image URL for category: {}", categoryName);
                                    } else {
                                        log.debug("Fetched image URL for category {}: {}", categoryName, imageUrl);
                                    }
                                });

                        Mono<String> descriptionMono = fetchCategoryDescription(categoryName)
                                .doOnNext(description -> {
                                    if (description == null || description.isEmpty()) {
                                        log.warn("Empty description for category: {}", categoryName);
                                    } else {
                                        log.debug("Fetched description for category {}: {}", categoryName, description);
                                    }
                                });

                        Mono<CategoryImageDTO> dtoMono = Mono.zip(imageUrlMono, descriptionMono)
                                .doOnNext(tuple -> log.debug("Inside zip: Image URL: {}, Description: {}", tuple.getT1(), tuple.getT2()))
                                .map(tuple -> new CategoryImageDTO(
                                        categoryName,
                                        tuple.getT1(),
                                        placeId,
                                        placeName,
                                        tuple.getT2()
                                ));

                        dtoMonos.add(dtoMono);
                    }
                    return Flux.concat(dtoMonos).collectList();
                })
                .doOnError(e -> log.error("Failed to fetch categories with images", e));
    }

    @Override
    public boolean categoryExists(String categoryName) {
        log.info("Checking existence of category: {}", categoryName);
        try {
            return categoryRepo.existsByNameIgnoreCase(categoryName);
        } catch (Exception e) {
            log.error("Error checking category existence: {}", categoryName, e);
            throw new CategoryOperationException("Error checking category existence for: " + categoryName, e);
        }
    }

    @Override
    public CategoryDTO getCategoryByName(String categoryName) {
        log.info("Fetching category by name: {}", categoryName);
        try {
            Category category = categoryRepo.findByNameIgnoreCase(categoryName)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found with name: " + categoryName));
            return mapToDTO(category);
        } catch (CategoryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching category by name: {}", categoryName, e);
            throw new CategoryOperationException("Failed to fetch category by name: " + categoryName, e);
        }
    }

    @Override
    public CategoryDTO createCategory(String name, String description) {
        log.info("Creating category: {}", name);
        if (categoryRepo.existsByNameIgnoreCase(name)) {
            log.warn("Category already exists: {}", name);
            throw new CategoryAlreadyExistsException("Category already exists with name: " + name);
        }
        try {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            Category saved = categoryRepo.save(category);
            return mapToDTO(saved);
        } catch (Exception e) {
            log.error("Error creating category: {}", name, e);
            throw new CategoryOperationException("Failed to create category: " + name, e);
        }
    }

    @Override
    public CategoryDTO saveCategory(CategoryDTO categoryDTO) {
        log.info("Saving category: {}", categoryDTO.getName());
        try {
            Category category = new Category();
            category.setName(categoryDTO.getName());
            category.setDescription(categoryDTO.getDescription());
            category.setPlaceId(categoryDTO.getPlaceId());
            category.setPlaceName(categoryDTO.getPlaceName());
            if (categoryDTO.getCategoryId() != null) {
                category.setCategoryId(categoryDTO.getCategoryId());
            }
            Category saved = categoryRepo.save(category);
            return mapToDTO(saved);
        } catch (Exception e) {
            log.error("Error saving category: {}", categoryDTO.getName(), e);
            throw new CategoryOperationException("Failed to save category: " + categoryDTO.getName(), e);
        }
    }

    private CategoryDTO mapToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }

    @Override
    public Mono<String> fetchCategoryDescription(String categoryName) {
        log.info("Fetching category description for: {}", categoryName);
        return Mono.fromCallable(() -> {
            List<String> descriptions = categoryRepo.findDescriptionsByNameIgnoreCase(categoryName);
            if (descriptions.isEmpty()) {
                throw new CategoryNotFoundException("Category not found: " + categoryName);
            }
            return descriptions.get(0);
        }).doOnError(e -> log.error("Error fetching description for category: {}", categoryName, e));
    }

    public String getDescriptionByCategoryName(String categoryName) {
        log.info("Getting description by category name: {}", categoryName);
        List<Category> categories = categoryRepo.findAllByName(categoryName);
        if (!categories.isEmpty()) {
            return categories.get(0).getDescription();
        } else {
            log.warn("Category not found for description request: {}", categoryName);
            throw new CategoryNotFoundException("Category with name '" + categoryName + "' not found.");
        }
    }

    @Override
    public Mono<List<CategoryWithPlacesDTO>> getSingleCategoryWithPlacesAndImages(String categoryName) {
        log.info("Fetching single category with places and images for: {}", categoryName);
        return Mono.fromCallable(() -> categoryRepo.findAllByNameIgnoreCase(categoryName))
                .flatMapMany(Flux::fromIterable)
                .flatMap(category -> {
                    String categoryId = String.valueOf(category.getCategoryId());
                    return placeService.getPlacesByCategoryId(categoryId)
                            .flatMapMany(Flux::fromIterable)
                            .flatMap(place -> mediaService.getImagesByPlaceId(place.getPlaceId())
                                    .map(photoUrls -> {
                                        PlaceRelatedImagesDTO dto = new PlaceRelatedImagesDTO();
                                        dto.setPlaceId(String.valueOf(place.getPlaceId()));
                                        dto.setPlaceName(place.getPlaceName());
                                        dto.setAboutPlace(place.getAboutPlace());
                                        dto.setPhotoUrls(photoUrls);
                                        return dto;
                                    }))
                            .collectList()
                            .map(placeDtos -> {
                                CategoryWithPlacesDTO dto = new CategoryWithPlacesDTO();
                                dto.setCategoryName(category.getName());
                                dto.setPlaces(placeDtos);
                                return dto;
                            });
                })
                .collectList()
                .map(this::mergeCategoriesByName)
                .doOnError(e -> log.error("Failed to fetch category with places and images: {}", categoryName, e));
    }

    private List<CategoryWithPlacesDTO> mergeCategoriesByName(List<CategoryWithPlacesDTO> list) {
        Map<String, CategoryWithPlacesDTO> map = new LinkedHashMap<>();
        for (CategoryWithPlacesDTO dto : list) {
            String name = dto.getCategoryName().toLowerCase();
            if (!map.containsKey(name)) {
                map.put(name, dto);
            } else {
                CategoryWithPlacesDTO existing = map.get(name);
                List<PlaceRelatedImagesDTO> mergedPlaces = new ArrayList<>(existing.getPlaces());
                for (PlaceRelatedImagesDTO place : dto.getPlaces()) {
                    boolean alreadyPresent = mergedPlaces.stream()
                            .anyMatch(p -> p.getPlaceId().equals(place.getPlaceId()));
                    if (!alreadyPresent) mergedPlaces.add(place);
                }
                existing.setPlaces(mergedPlaces);
            }
        }
        return new ArrayList<>(map.values());
    }
}
