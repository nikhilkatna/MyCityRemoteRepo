//package com.mycity.place.test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.mycity.place.entity.Coordinate;
//import com.mycity.place.entity.Place;
//import com.mycity.place.entity.TimeZone;
//import com.mycity.place.repository.PlaceRepository;
//import com.mycity.place.serviceImpl.PlaceServiceImpl;
//import com.mycity.place.serviceImpl.WebClientCategoryService;
//import com.mycity.place.serviceImpl.WebClientLocationService;
//import com.mycity.place.serviceImpl.WebClientMediaService;
//import com.mycity.shared.categorydto.CategoryDTO;
//import com.mycity.shared.placedto.LocalCuisineDTO;
//import com.mycity.shared.placedto.PlaceCategoryDTO;
//import com.mycity.shared.placedto.PlaceDTO;
//import com.mycity.shared.placedto.PlaceResponseDTO;
//import com.mycity.shared.placedto.PlaceWithImagesDTO;
//import com.mycity.shared.timezonedto.TimezoneDTO;
//import com.mycity.shared.tripplannerdto.CoordinateDTO;
//
//
//@ExtendWith(MockitoExtension.class)
//class PlaceServiceImplTest {
//
//    @InjectMocks
//    private PlaceServiceImpl placeService;
//
//    @Mock
//    private PlaceRepository placeRepo;
//
//    @Mock
//    private WebClientMediaService mediaService;
//
//    @Mock
//    private WebClientCategoryService categoryService;
//
//    @Mock
//    private WebClientLocationService clientLocationService;
//
//    private Place samplePlace;
//    private PlaceDTO sampleDto;
//
//    @BeforeEach
//    void setUp() {
//    	MockitoAnnotations.openMocks(this); // Initialize the mocks
//    	
//        samplePlace = new Place();
//        samplePlace.setPlaceId(1L);
//        samplePlace.setPlaceName("Sample Place");
//        samplePlace.setAboutPlace("Nice place");
//        samplePlace.setPlaceHistory("Historical background");
//        samplePlace.setCategoryName("Nature");
//        samplePlace.setPlaceDistrict("District");
//        samplePlace.setRating(4.5);
//        samplePlace.setCoordinate(new Coordinate(12.34, 56.78));
//        samplePlace.setPostedOn(LocalDate.now());
//        samplePlace.setTimeZone(new TimeZone(LocalTime.of(9, 0), LocalTime.of(18, 0)));
//
//        sampleDto = new PlaceDTO();
//        sampleDto.setPlaceName("Sample Place");
//        sampleDto.setAboutPlace("Nice place");
//        sampleDto.setPlaceHistory("Historical background");
//        sampleDto.setCategoryName("Nature");
//        sampleDto.setPlaceDistrict("District");
//        sampleDto.setRating(4.5);
//        sampleDto.setCoordinate(new CoordinateDTO(12.34, 56.78));
//        TimezoneDTO timezoneDTO = new TimezoneDTO(0L,LocalTime.of(9, 0), LocalTime.of(18, 0));
//        sampleDto.setTimeZone(timezoneDTO);
//        sampleDto.setPostedOn(LocalDate.now());
//    }
//
//    @Test
//    void testAddPlace_basic() {
//        // Mock the repository methods
//        when(placeRepo.save(any())).thenReturn(samplePlace);
//        doNothing().when(placeRepo).flush();  // Mock flush() as a void method
//
//        // Mock the category service to return a CategoryDTO with the expected values
//        CategoryDTO categoryDTO = new CategoryDTO(1L, 2L, "Nature", "Sample Place", "Desc");
//        when(categoryService.saveCategory(any())).thenReturn(categoryDTO);
//
//        // Call the method to test
//        String result = placeService.addPlace(sampleDto, new ArrayList<>());
//
//        // Assert that the result contains the expected success message
//        assertTrue(result.contains("saved successfully"));
//
//        // Verify that the uploadImageForPlace method was never called
//        verify(mediaService, never()).uploadImageForPlace(any(), anyLong(), anyString(), anyString(), anyString());
//    }
//
//
//
//    @Test
//    void testGetPlace_success() {
//        when(placeRepo.findById(1L)).thenReturn(Optional.of(samplePlace));
//
//        PlaceResponseDTO dto = placeService.getPlace(1L);
//
//        assertEquals("Sample Place", dto.getPlaceName());
//        assertEquals(4.5, dto.getRating());
//    }
//
//    @Test
//    void testUpdatePlace_success() {
//        when(placeRepo.findById(1L)).thenReturn(Optional.of(samplePlace));
//        when(categoryService.getCategoryByName(any()))
//            .thenReturn(new CategoryDTO(1L, null, null, "Nature", "Nature description"));
//
//        String result = placeService.updatePlace(1L, sampleDto);
//
//        assertTrue(result.contains("updated successfully"));
//        verify(placeRepo, times(1)).save(any());
//    }
//
//
//    @Test
//    void testDeletePlace_success() {
//        doNothing().when(placeRepo).deleteById(1L);
//
//        String result = placeService.deletePlace(1L);
//        assertEquals("Place With Id ::1 Deleted Successfully.", result);
//    }
//
//    @Test
//    void testGetAllPlaces() {
//        when(placeRepo.findAll()).thenReturn(List.of(samplePlace));
//
//        List<PlaceResponseDTO> results = placeService.getAllPlaces();
//
//        assertEquals(1, results.size());
//        assertEquals("Sample Place", results.get(0).getPlaceName());
//    }
//
//    @Test
//    void testGetAllDistinctCategories() {
//        List<PlaceCategoryDTO> mockList = List.of(
//            new PlaceCategoryDTO("Nature", 1L, "Sample Place")
//        );
//        when(placeRepo.findPlaceIdsAndCategories()).thenReturn(mockList);
//
//        List<PlaceCategoryDTO> result = placeService.getAllDistinctCategories();
//
//        assertEquals(1, result.size());
//        assertEquals("Nature", result.get(0).getCategoryName());
//        assertEquals(1L, result.get(0).getPlaceId());
//        assertEquals("Sample Place", result.get(0).getPlaceName());
//    }
//
//
//    @Test
//    void testGetPlaceIdByName() {
//        when(placeRepo.findPlaceIdByPlaceName("Sample Place")).thenReturn(1L);
//
//        Long placeId = placeService.getPlaceIdByName("Sample Place");
//        assertEquals(1L, placeId);
//    }
//
//    @Test
//    void testSavePlace() {
//        when(placeRepo.save(any())).thenReturn(samplePlace);
//
//        Place saved = placeService.savePlace(samplePlace);
//        assertEquals("Sample Place", saved.getPlaceName());
//    }
//
//    @Test
//    void testGetPlaceById_success() {
//        when(placeRepo.findById(1L)).thenReturn(Optional.of(samplePlace));
//
//        PlaceDTO place = placeService.getPlaceById(1L);
//        assertEquals("Sample Place", place.getPlaceName());
//    }
//
//    @Test
//    void testAddPlace_Successful() throws Exception {
//        // Given
//        PlaceDTO placeDto = new PlaceDTO();
//        placeDto.setPlaceName("Goa Beach");
//        placeDto.setAboutPlace("A beautiful beach in Goa");
//        placeDto.setPlaceHistory("Historically significant location");
//        placeDto.setCategoryName("Beach");
//        placeDto.setPlaceDistrict("North Goa");
//        placeDto.setRating(4.5);
//        placeDto.setPostedOn(LocalDate.of(2025, 5, 12));
//
//        // Add TimeZone
//        TimezoneDTO timeZoneDTO = new TimezoneDTO();
//        timeZoneDTO.setOpeningTime(LocalTime.of(9, 0));
//        timeZoneDTO.setClosingTime(LocalTime.of(18, 0));
//        placeDto.setTimeZone(timeZoneDTO);
//
//        // Add Local Cuisines
//        LocalCuisineDTO cuisineDTO = new LocalCuisineDTO();
//        cuisineDTO.setCuisineName("Fish Curry");
//        placeDto.setLocalCuisines(List.of(cuisineDTO));
//
//        // Mock Coordinates
//        CoordinateDTO coordinateDTO = new CoordinateDTO(15.2993, 74.1240);
//        when(clientLocationService.fetchCoordinatesByPlaceName("Goa Beach"))
//                .thenReturn(CompletableFuture.completedFuture(coordinateDTO));
//
//        // Mock Place save
//        Place savedPlace = new Place();
//        savedPlace.setPlaceId(1L);
//        savedPlace.setPlaceName("Goa Beach");
//        savedPlace.setCategoryName("Beach");
//        savedPlace.setLocalCuisines(new ArrayList<>());
//        when(placeRepo.save(any(Place.class))).thenReturn(savedPlace);
//
//        // Mock Category creation
//        CategoryDTO categoryDTO = new CategoryDTO();
//        categoryDTO.setCategoryId(2L);
//        categoryDTO.setName("Beach");
//        when(categoryService.saveCategory(any(CategoryDTO.class))).thenReturn(categoryDTO);
//
//        // Mock images
//        MockMultipartFile placeImage = new MockMultipartFile("beach.jpg", "beach.jpg", "image/jpeg", "image-content".getBytes());
//        MockMultipartFile cuisineImage = new MockMultipartFile("Fish Curry.jpg", "Fish Curry.jpg", "image/jpeg", "image-content".getBytes());
//
//        Map<String, MultipartFile> placeImages = Map.of("beach.jpg", placeImage);
//        Map<String, MultipartFile> cuisineImages = Map.of("Fish Curry.jpg", cuisineImage);
//
//        // When
//        String result = placeService.addPlace(placeDto, placeImages, cuisineImages);
//
//        // Then
//        assertTrue(result.contains("Goa Beach"));
//        verify(clientLocationService).fetchCoordinatesByPlaceName("Goa Beach");
//        verify(placeRepo, times(2)).save(any(Place.class)); // Save called twice
//        verify(placeRepo).flush(); // Only one flush
//        verify(categoryService).saveCategory(any(CategoryDTO.class));
//
//        // ArgumentCaptor for better debugging
//        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
//        ArgumentCaptor<String> cuisineCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<Long> placeIdCaptor = ArgumentCaptor.forClass(Long.class);
//        ArgumentCaptor<String> placeNameCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
//
//        // Verify the uploadCuisineImage method is called with the correct arguments
//        verify(mediaService).uploadCuisineImage(
//            fileCaptor.capture(),
//            cuisineCaptor.capture(),
//            placeIdCaptor.capture(),
//            placeNameCaptor.capture(),
//            categoryCaptor.capture()
//        );
//
//        // Print captured arguments for debugging
//        System.out.println("Captured file: " + fileCaptor.getValue().getOriginalFilename());
//        System.out.println("Captured cuisine name: " + cuisineCaptor.getValue());
//        System.out.println("Captured placeId: " + placeIdCaptor.getValue());
//        System.out.println("Captured placeName: " + placeNameCaptor.getValue());
//        System.out.println("Captured category: " + categoryCaptor.getValue());
//
//        // Assert the arguments passed to the method
//        assertEquals("Fish Curry", cuisineCaptor.getValue()); // Check if cuisine name is captured correctly
//        assertEquals(1L, placeIdCaptor.getValue()); // Check if placeId is captured correctly
//        assertEquals("Goa Beach", placeNameCaptor.getValue()); // Check if placeName is captured correctly
//        assertEquals("Beach", categoryCaptor.getValue()); // Check if category is captured correctly
//
//        System.out.println("Upload cuisine image check passed");
//    }
//
//
//
//
//
//    @Test
//    void testGetPlacesByCategoryWithImages() {
//        when(placeRepo.findByCategoryName("Nature")).thenReturn(List.of(samplePlace));
//        when(categoryService.getCategoryDescription("Nature")).thenReturn("Beautiful nature");
//        when(mediaService.getPhotoUrlsByPlaceId(anyLong())).thenReturn(List.of("img1.jpg"));
//
//        List<PlaceWithImagesDTO> results = placeService.getPlacesByCategoryWithImages("Nature");
//
//        assertEquals(1, results.size());
//        assertEquals("img1.jpg", results.get(0).getPhotoUrls().get(0));
//        assertEquals("Beautiful nature", results.get(0).getPlaceCategoryDescription()); // ✅ Should pass now
//    }
//
//
//    /*
//    @Test
//    void testAddPlace_withNullDTO_shouldThrow() {
//        // Ensure that this method matches the one you're testing.
//        // When the PlaceDTO is null, it should throw an IllegalArgumentException.
//        Exception ex = assertThrows(IllegalArgumentException.class, () -> 
//            placeService.addPlace(null, null, null)  // This should match the method signature
//        );
//        
//        // Check that the correct exception message is returned
//        assertEquals("PlaceDTO cannot be null", ex.getMessage());
//    }
//
//        
//        assertEquals("PlaceDTO cannot be null", ex.getMessage());
//    }
//    */
//
//
//    @Test
//    void testGetPlace_invalidId_throwsException() {
//        when(placeRepo.findById(999L)).thenReturn(Optional.empty());
//
//        assertThrows(IllegalArgumentException.class, () -> placeService.getPlace(999L));
//    }
//}
