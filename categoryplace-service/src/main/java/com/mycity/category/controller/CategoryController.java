package com.mycity.category.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycity.category.exception.CategoryOperationException;
import com.mycity.category.service.CategoryService;
import com.mycity.shared.categorydto.CategoryDTO;
import com.mycity.shared.categorydto.CategoryImageDTO;
import com.mycity.shared.categorydto.CategoryWithPlacesDTO;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/category")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/unique/images")
    public Mono<ResponseEntity<List<CategoryImageDTO>>> getCategoriesWithImages() {
        log.info("Fetching categories with images");
        return categoryService.fetchCategoriesWithImages()
            .map(ResponseEntity::ok)
            .doOnError(e -> log.error("Error fetching categories with images", e));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> categoryExists(@RequestParam String name) {
        log.info("Checking if category exists: {}", name);
        try {
            boolean exists = categoryService.categoryExists(name);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Error checking category existence: {}", name, e);
            throw new CategoryOperationException("Failed to check category existence for: " + name, e);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        log.info("Creating category: {}", categoryDTO.getName());
        try {
            CategoryDTO created = categoryService.createCategory(categoryDTO.getName(), categoryDTO.getDescription());
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating category: {}", categoryDTO.getName(), e);
            throw new CategoryOperationException("Failed to create category: " + categoryDTO.getName(), e);
        }
    }

    @GetMapping("/categorybyname/{categoryName}")
    public ResponseEntity<CategoryDTO> getCategoryByName(@PathVariable String categoryName) {
        log.info("Fetching category by name: {}", categoryName);
        try {
            CategoryDTO category = categoryService.getCategoryByName(categoryName);
            if (category == null) {
                log.warn("Category not found: {}", categoryName);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            log.error("Error fetching category by name: {}", categoryName, e);
            throw new CategoryOperationException("Failed to fetch category: " + categoryName, e);
        }
    }

    @GetMapping("/category-by-name/{categoryName}")
    public Mono<ResponseEntity<List<CategoryWithPlacesDTO>>> getCategoriesWithPlacesAndImages(@PathVariable String categoryName) {
        log.info("Fetching category with places and images for: {}", categoryName);
        return categoryService.getSingleCategoryWithPlacesAndImages(categoryName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(e -> log.error("Error fetching category with places and images for: {}", categoryName, e));
    }

    @PostMapping("/save")
    public ResponseEntity<CategoryDTO> saveCategory(@RequestBody CategoryDTO categoryDTO) {
        log.info("Saving category: {}", categoryDTO.getName());
        try {
            CategoryDTO saved = categoryService.saveCategory(categoryDTO);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error saving category: {}", categoryDTO.getName(), e);
            throw new CategoryOperationException("Failed to save category: " + categoryDTO.getName(), e);
        }
    }

    @GetMapping("/desc/{categoryName}")
    public ResponseEntity<String> getCategoryDescriptionByName(@PathVariable String categoryName) {
        log.info("Fetching description for category: {}", categoryName);
        try {
            String description = categoryService.getDescriptionByCategoryName(categoryName);
            if (description == null) {
                log.warn("Description not found for category: {}", categoryName);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            log.error("Error fetching description for category: {}", categoryName, e);
            throw new CategoryOperationException("Failed to fetch description for category: " + categoryName, e);
        }
    }
}
