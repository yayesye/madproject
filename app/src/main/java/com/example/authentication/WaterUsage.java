package com.example.authentication;

public class WaterUsage {

    public String id;
    public String consumption;
    public String users;
    public String hours;

    public WaterUsage() {
    }

    public WaterUsage(String id, String consumption, String users, String hours) {
        this.id = id;
        this.consumption = consumption;
        this.users = users;
        this.hours = hours;
    }
}