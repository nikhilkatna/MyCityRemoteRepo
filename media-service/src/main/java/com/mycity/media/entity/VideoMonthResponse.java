package com.mycity.media.entity;

import java.util.List;

public class VideoMonthResponse {
    private String month;
    private List<String> urls;


    // Constructors
    public VideoMonthResponse(String month, List<String> urls) {
        this.month = month;
        this.urls = urls;
    }

    // Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

}

