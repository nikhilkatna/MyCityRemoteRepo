package com.mycity.media.entity;

import java.util.List;

import lombok.Data;

@Data
public class GroupedVideoResponse {
    private String monthName;
    private List<VideoItem> videos;

    public GroupedVideoResponse(String monthName, List<VideoItem> videos) {
        this.monthName = monthName;
        this.videos = videos;
    }

}

