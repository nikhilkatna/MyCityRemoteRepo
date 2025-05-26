package com.mycity.place.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.place.controller.PlaceController;
import com.mycity.place.serviceImpl.PlaceServiceImpl;
import com.mycity.shared.placedto.PlaceCategoryDTO;
import com.mycity.shared.placedto.PlaceDTO;
import com.mycity.shared.placedto.PlaceResponseDTO;
import com.mycity.shared.placedto.PlaceWithImagesDTO;
import com.mycity.shared.timezonedto.TimezoneDTO;
import com.mycity.shared.tripplannerdto.CoordinateDTO;

/*
@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

    @InjectMocks
    private PlaceController placeController;

    @Mock
    private PlaceServiceImpl placeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private PlaceDTO createSamplePlaceDTO() {
        PlaceDTO dto = new PlaceDTO();
        dto.setPlaceName("Sample Place");
        dto.setAboutPlace("About");
        dto.setPlaceHistory("History");
        dto.setPlaceDistrict("District");
        dto.setCategoryName("Nature");
        dto.setRating(4.2);
        dto.setPostedOn(LocalDate.now());

        CoordinateDTO coord = new CoordinateDTO();
        coord.setLatitude(10.0);
        coord.setLongitude(20.0);
        dto.setCoordinate(coord);

        TimezoneDTO tz = new TimezoneDTO();
        tz.setOpeningTime(LocalTime.parse("08:00"));
        tz.setClosingTime(LocalTime.parse("18:00"));
        dto.setTimeZone(tz);

        return dto;
    }


    @Test
    void testAddPlaceWithImageMap() {
        PlaceDTO dto = new PlaceDTO(); // replace with sample DTO creation
        Map<String, MultipartFile> images = new HashMap<>();
        Map<String, MultipartFile> localCuisineImages = new HashMap<>();

        when(placeService.addPlace(any(PlaceDTO.class), anyMap(), anyMap()))
                .thenReturn("Place added with images successfully.");

        ResponseEntity<String> response = placeController.addPlaceDetails(dto, images, localCuisineImages);

        System.out.println("Actual Status Code: " + response.getStatusCodeValue());
        System.out.println("Actual Response Body: " + response.getBody());

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Place added with images successfully.", response.getBody());
    }


    @Test
    void testGetPlace() {
        PlaceResponseDTO mockResponse = new PlaceResponseDTO();
        mockResponse.setPlaceName("Sample Place");

        when(placeService.getPlace(1L)).thenReturn(mockResponse);

        ResponseEntity<PlaceResponseDTO> response = placeController.getPlaceDetails(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Sample Place", response.getBody().getPlaceName());
    }

    @Test
    void testUpdatePlace() {
        PlaceDTO dto = createSamplePlaceDTO();
        when(placeService.updatePlace(1L, dto)).thenReturn("Updated successfully.");

        ResponseEntity<String> response = placeController.updatePlaceDetails(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Updated successfully.", response.getBody());
    }

    @Test
    void testDeletePlace() {
        when(placeService.deletePlace(1L)).thenReturn("Place deleted.");

        ResponseEntity<String> response = placeController.deletePlaceDetails(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Place deleted.", response.getBody());
    }

    @Test
    void testGetAllPlaces() {
        PlaceResponseDTO dto = new PlaceResponseDTO();
        dto.setPlaceName("Place1");

        when(placeService.getAllPlaces()).thenReturn(List.of(dto));

        ResponseEntity<List<PlaceResponseDTO>> response = placeController.getAllPlaces();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Place1", response.getBody().get(0).getPlaceName());
    }

    @Test
    void testGetAllDistinctCategories() {
        PlaceCategoryDTO dto = new PlaceCategoryDTO();
        dto.setCategoryName("Nature");

        when(placeService.getAllDistinctCategories()).thenReturn(List.of(dto));

        ResponseEntity<List<PlaceCategoryDTO>> response = placeController.getAllDistinctCategories();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Nature", response.getBody().get(0).getCategoryName());
    }

    @Test
    void testGetPlacesByCategoryWithImages() {
        PlaceWithImagesDTO dto = new PlaceWithImagesDTO();
        dto.setPlaceName("Image Place");

        when(placeService.getPlacesByCategoryWithImages("Nature")).thenReturn(List.of(dto));

        ResponseEntity<List<PlaceWithImagesDTO>> response = placeController.getPlacesByCategory("Nature");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Image Place", response.getBody().get(0).getPlaceName());
    }
}
*/

