package com.example.taskappparalelos.model;

public class UnsplashImage {
    private String id;
    private String description;
    private String url;

    public UnsplashImage(String id, String description, String url) {
        this.id = id;
        this.description = description;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }
}
