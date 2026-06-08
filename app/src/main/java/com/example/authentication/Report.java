package com.example.authentication;

import com.google.firebase.firestore.PropertyName;

public class Report {
    private String id;
    private String issueType;
    private String description;
    private String userId;
    private String timestamp;
    private String status;

    // Empty constructor (REQUIRED for Firebase)
    public Report() {
    }

    // Full constructor
    public Report(String id, String issueType, String description, String userId, String timestamp, String status) {
        this.id = id;
        this.issueType = issueType;
        this.description = description;
        this.userId = userId;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters
    public String getId() { return id; }

    @PropertyName("issueType")
    public String getIssueType() { return issueType; }

    @PropertyName("description")
    public String getDescription() { return description; }

    @PropertyName("userId")
    public String getUserId() { return userId; }

    @PropertyName("timestamp")
    public String getTimestamp() { return timestamp; }

    @PropertyName("status")
    public String getStatus() { return status; }

    // Setters
    public void setId(String id) { this.id = id; }

    @PropertyName("issueType")
    public void setIssueType(String issueType) { this.issueType = issueType; }

    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("timestamp")
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }
}