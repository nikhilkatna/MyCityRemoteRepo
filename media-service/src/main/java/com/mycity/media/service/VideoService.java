package com.mycity.media.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.mycity.media.entity.GroupedVideoResponse;
import com.mycity.media.entity.VideoDetailsResponse;
import com.mycity.media.entity.VideoMonthResponse;
import com.mycity.media.entity.Videos;

public interface VideoService {

	String addVideo(MultipartFile file,Videos video);
	
	List<GroupedVideoResponse> getVideoUrlByMonth(String monthName);
	
	VideoMonthResponse getCurrentMonthVideos();
	
}
