package com.mycity.media.entity;

import lombok.Data;

@Data
public class VideoItem {
    private String placeName;
    private String videoUrl;

    public VideoItem(String placeName, String videoUrl) {
        this.placeName = placeName;
        this.videoUrl = videoUrl;
    }
}

