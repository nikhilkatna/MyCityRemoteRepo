package com.mycity.media.serviceImpl;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.media.entity.GroupedVideoResponse;
import com.mycity.media.entity.VideoItem;
import com.mycity.media.entity.VideoMonthResponse;
import com.mycity.media.entity.Videos;
import com.mycity.media.helper.CloudinaryHelper;
import com.mycity.media.repository.VideoUrlRepository;
import com.mycity.media.service.VideoService;


@Service
public class VideoServiceImpl implements VideoService 
{
	@Autowired
	private VideoUrlRepository videoRepo;
	
	@Autowired
	private CloudinaryHelper coludinaryhelper;

	@Override
	public String addVideo(MultipartFile file, Videos video) {

	    if (file == null || file.isEmpty()) {
	        throw new IllegalArgumentException("Uploaded video file is empty or missing.");
	    }

	    if (video == null) {
	        throw new IllegalArgumentException("Video metadata must not be null.");
	    }
	    if (video.getPlaceName() == null || video.getPlaceName().trim().isEmpty()) {
	        throw new IllegalArgumentException("Place name is required.");
	    }

	    if (video.getMonthName() == null || video.getMonthName().trim().isEmpty()) {
	        throw new IllegalArgumentException("Month Name is Required");
	    }

	    try {
	        // Upload video to Cloudinary
	        String url = coludinaryhelper.saveVideo(file);
	        video.setVideoUrl(url);

	        Videos savedVideo = videoRepo.save(video);

	        return "Video saved successfully for the place with name: " + savedVideo.getPlaceName();

	    } catch (RuntimeException e) {
	     
	        throw new RuntimeException("Failed to upload and save video: " + e.getMessage(), e);
	    }
	}


	public List<GroupedVideoResponse> getVideoUrlByMonth(String monthName) {
	    if (monthName == null || monthName.trim().isEmpty()) {
	        throw new IllegalArgumentException("Month name must not be null or empty.");
	    }

	    List<Object[]> results = videoRepo.findRawVideoDetailsByMonth(monthName.trim());

	    if (results.isEmpty()) {
	        throw new RuntimeException("No videos found for month: " + monthName);
	    }

	    List<VideoItem> videoItems = results.stream()
	        .map(obj -> new VideoItem((String) obj[1], (String) obj[2])) 
	        .collect(Collectors.toList());

	    GroupedVideoResponse response = new GroupedVideoResponse(monthName.trim(), videoItems);
	    return Collections.singletonList(response);
	}






	@Override
	public VideoMonthResponse getCurrentMonthVideos() {
	    List<Videos> videos = videoRepo.findAll();

	    if (videos.isEmpty()) {
	        throw new RuntimeException("No videos found in the database.");
	    }

	    String currentMonth = LocalDate.now()
	        .getMonth()
	        .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

	    List<String> urls = videoRepo.findAllVideoUrlsByMonthName(currentMonth);

	    if (urls.isEmpty()) {
	        throw new RuntimeException("No videos found for the current month: " + currentMonth);
	    }

	    List<String> uniqueUrls = urls.stream().distinct().collect(Collectors.toList());

	    return new VideoMonthResponse(currentMonth, uniqueUrls);
	}


}
