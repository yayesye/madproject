package com.example.authentication;

public class TaskModel {
    private String taskId;
    private String targetStation;
    private String specialistPhone;
    private String date;
    private String status;

    public TaskModel() {}

    public TaskModel(String taskId, String targetStation, String specialistPhone, String date, String status) {
        this.taskId = taskId;
        this.targetStation = targetStation;
        this.specialistPhone = specialistPhone;
        this.date = date;
        this.status = status;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTargetStation() { return targetStation; }
    public void setTargetStation(String targetStation) { this.targetStation = targetStation; }

    public String getSpecialistPhone() { return specialistPhone; }
    public void setSpecialistPhone(String specialistPhone) { this.specialistPhone = specialistPhone; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status != null ? status : "Pending"; }
    public void setStatus(String status) { this.status = status; }
}