package com.mycity.media.entity;

public class VideoDetailsResponse {
    private String monthName;
    private String placeName;
    private String videoUrl;


    public VideoDetailsResponse(String monthName, String placeName, String videoUrl) {
        this.monthName = monthName;
        this.placeName = placeName;
        this.videoUrl = videoUrl;
    }


    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}

