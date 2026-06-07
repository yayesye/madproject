package com.example.authentication;

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
    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getUserId() { return userId; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public void setDescription(String description) { this.description = description; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setStatus(String status) { this.status = status; }
}