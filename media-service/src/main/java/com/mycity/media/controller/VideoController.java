package com.mycity.media.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycity.media.entity.GroupedVideoResponse;
import com.mycity.media.entity.VideoMonthResponse;
import com.mycity.media.entity.Videos;
import com.mycity.media.service.VideoService;


@RestController
@RequestMapping("/media/video")
public class VideoController 
{
	@Autowired
    private VideoService service;
	
	@PostMapping(value="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadVideos(@RequestPart MultipartFile videos,
			                                    @ModelAttribute Videos video)
	{
		//use service
		String result=service.addVideo(videos, video);
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/geturl/{monthName}")
	public ResponseEntity<List<GroupedVideoResponse>> getVideoUrlsByMonth(@PathVariable String monthName)
	{
	    //use service
		List<GroupedVideoResponse> response=service.getVideoUrlByMonth(monthName);
	    return ResponseEntity.ok(response);
	}
	
	
	@GetMapping("/currentmonth")
	public ResponseEntity<VideoMonthResponse> getAllVideoUrls()
	{
		VideoMonthResponse response=service.getCurrentMonthVideos();
		return ResponseEntity.ok(response);
	}
	
}
