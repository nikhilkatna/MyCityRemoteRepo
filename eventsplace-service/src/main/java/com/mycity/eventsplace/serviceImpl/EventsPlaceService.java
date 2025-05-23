package com.mycity.eventsplace.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.eventsplace.entity.Event;
import com.mycity.eventsplace.entity.EventHighlights;
import com.mycity.eventsplace.repository.EventsRepository;
import com.mycity.eventsplace.service.EventsPlaceServiceInterface;
import com.mycity.shared.eventsdto.EventsDTO;
import com.mycity.shared.mediadto.EventSubImagesDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventsPlaceService implements EventsPlaceServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(EventsPlaceService.class);

    private final EventsRepository eventRepo;
    private final MediaServiceConfig mediaService;

    @Override
    public String addEvent(EventsDTO dto, List<MultipartFile> galleryImages, List<String> imageNames) {
        logger.info("Adding new event: {}", dto.getEventName());
        validateEventDTO(dto);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
            LocalDate eventDate = LocalDate.parse(dto.getDate(), formatter);

            Event event = new Event();
            event.setEventName(dto.getEventName());
            event.setCity(dto.getCity());
            event.setDescription(dto.getDescription());
            event.setDate(eventDate);
            event.setDuration(dto.getDuration());

            List<EventHighlights> highlights = dto.getSchedule().stream().map(highlightDto -> {
                EventHighlights highlight = new EventHighlights();
                highlight.setDate(eventDate);
                highlight.setTime(highlightDto.getTime());
                highlight.setActivityName(highlightDto.getActivityName());
                return highlight;
            }).collect(Collectors.toList());

            event.setSchedule(highlights);
            Event saved = eventRepo.save(event);

            EventSubImagesDTO galleryDto = new EventSubImagesDTO(null, null, saved.getEventName(), saved.getEventId(), null);
            mediaService.uploadGalleryImages(galleryImages, imageNames, galleryDto);

            logger.info("Event '{}' saved with ID {}", saved.getEventName(), saved.getEventId());
            return "Event '" + saved.getEventName() + "' saved with images!";
        } catch (Exception e) {
            logger.error("Error adding event '{}': {}", dto.getEventName(), e.getMessage(), e);
            throw new RuntimeException("Failed to add event. Please try again.");
        }
    }

    @Override
    public String updateEvent(Long eventId, EventsDTO dto, List<MultipartFile> galleryImages, List<String> imageNames) {
        logger.info("Updating event with ID: {}", eventId);
        validateEventDTO(dto);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate eventDate = LocalDate.parse(dto.getDate(), formatter);

            Event existingEvent = eventRepo.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

            existingEvent.setEventName(dto.getEventName());
            existingEvent.setCity(dto.getCity());
            existingEvent.setDescription(dto.getDescription());
            existingEvent.setDate(eventDate);
            existingEvent.setDuration(dto.getDuration());

            List<EventHighlights> highlights = dto.getSchedule().stream().map(highlightDto -> {
                EventHighlights highlight = new EventHighlights();
                highlight.setDate(eventDate);
                highlight.setTime(highlightDto.getTime());
                highlight.setActivityName(highlightDto.getActivityName());
                return highlight;
            }).collect(Collectors.toList());

            existingEvent.setSchedule(highlights);
            Event updatedEvent = eventRepo.save(existingEvent);

            mediaService.updateEventImagesInMediaService(updatedEvent.getEventId(), updatedEvent.getEventName(), galleryImages, imageNames);

            logger.info("Event '{}' updated successfully", updatedEvent.getEventName());
            return "Event '" + updatedEvent.getEventName() + "' updated with new images!";
        } catch (Exception e) {
            logger.error("Error updating event ID {}: {}", eventId, e.getMessage(), e);
            throw new RuntimeException("Failed to update event. Please try again.");
        }
    }

    @Override
    public String deleteEvent(Long eventId) {
        logger.info("Deleting event with ID: {}", eventId);
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

        mediaService.deleteEventImages(eventId, event.getEventName());
        eventRepo.delete(event);
        logger.info("Event '{}' deleted successfully", event.getEventName());
        return "Event '" + event.getEventName() + "' deleted successfully with its images.";
    }

    @Override
    public Map<String, Object> createEventDetailsSection(Long eventId) {
        logger.info("Creating event details section for event ID: {}", eventId);
        Event selectedEvent = eventRepo.findById(eventId).orElse(null);
        if (selectedEvent == null) {
            logger.warn("Event not found with ID: {}", eventId);
            return null;
        }

        Map<String, Object> detailsSection = new HashMap<>();
        List<String> images = new ArrayList<>();
        try {
            images = mediaService.getImageUrlsForEvent(eventId);
        } catch (Exception e) {
            logger.error("Error fetching images for event {}: {}", eventId, e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eventName", selectedEvent.getEventName());
        data.put("description", selectedEvent.getDescription());
        data.put("date", selectedEvent.getDate());
        data.put("time", selectedEvent.getDuration());
        data.put("schedule", selectedEvent.getSchedule());
        data.put("eventImages", images);

        List<Map<String, Object>> matchingCarts = new ArrayList<>();
        for (Event ev : eventRepo.findAll()) {
            if (ev.getEventName().equalsIgnoreCase(selectedEvent.getEventName()) && !ev.getEventId().equals(eventId)) {
                Map<String, Object> cartData = new HashMap<>();
                cartData.put("eventName", ev.getEventName());
                cartData.put("date", ev.getDate());
                cartData.put("city", ev.getCity());
                try {
                    List<String> cartImages = mediaService.getImageUrlsForEvent(ev.getEventId());
                    cartData.put("cartImages", cartImages);
                } catch (Exception ex) {
                    logger.warn("Failed to fetch images for similar event {}: {}", ev.getEventId(), ex.getMessage());
                }
                matchingCarts.add(cartData);
            }
        }

        data.put("similarEvents", matchingCarts);
        detailsSection.put("sectionId", "eventDetails");
        detailsSection.put("data", data);
        return detailsSection;
    }

    @Override
    public Map<String, Object> createEventCartSection() {
        logger.info("Creating event cart section");
        List<Event> events = eventRepo.findAll();
        Map<String, Object> cartSection = new HashMap<>();
        Set<String> seenEventNames = new HashSet<>();
        List<Map<String, Object>> eventList = new ArrayList<>();

        for (Event e : events) {
            String eventName = e.getEventName();
            if (seenEventNames.contains(eventName)) continue;
            seenEventNames.add(eventName);

            Map<String, Object> data = new HashMap<>();
            data.put("eventName", eventName);
            data.put("date", e.getDate());
            data.put("city", e.getCity());

            try {
                List<String> images = mediaService.getImageUrlsForEvent(e.getEventId());
                data.put("images", images);
            } catch (Exception ex) {
                logger.warn("Failed to fetch images for event {}: {}", e.getEventId(), ex.getMessage());
            }

            eventList.add(data);
        }

        cartSection.put("sectionId", "eventCart");
        cartSection.put("data", eventList);
        return cartSection;
    }

    private void validateEventDTO(EventsDTO dto) {
        if (dto.getEventName() == null || dto.getEventName().trim().isEmpty())
            throw new IllegalArgumentException("Enter Event Name");
        if (dto.getCity() == null || dto.getCity().trim().isEmpty())
            throw new IllegalArgumentException("Mention the City");
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty())
            throw new IllegalArgumentException("Mention the Event Description");
        if (dto.getDate() == null)
            throw new IllegalArgumentException("Mention the Event Date");
        if (dto.getDuration() == null)
            throw new IllegalArgumentException("Mention the Event Duration");
        if (dto.getSchedule() == null || dto.getSchedule().isEmpty())
            throw new IllegalArgumentException("Add at least one Event Highlight");
    }
}
