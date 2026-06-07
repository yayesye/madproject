package com.example.authentication;

public class MaintenanceTask {
    private String id;
    private String name;
    private String date;
    private String status;
    private String assignedTo;

    public MaintenanceTask() {
    }

    public MaintenanceTask(String id, String name, String date, String status, String assignedTo) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
}