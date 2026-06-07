package com.example.authentication;

public class NotificationModel {
    public String id;
    public String title;
    public String message;
    public Long timestamp;

    public NotificationModel() {
    }

    public NotificationModel(String id, String title, String message, Long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }
}