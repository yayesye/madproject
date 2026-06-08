package com.example.authentication;

public class StationModel {
    private String stationId;
    private String name;
    private double turbidity;
    private double ph;
    private double temperature;
    private double maxTurbidityLimit;
    private double minPhLimit;

    // Required empty constructor for Firestore
    public StationModel() {}

    public StationModel(String stationId, String name, double turbidity, double ph, double temperature, double maxTurbidityLimit, double minPhLimit) {
        this.stationId = stationId;
        this.name = name;
        this.turbidity = turbidity;
        this.ph = ph;
        this.temperature = temperature;
        this.maxTurbidityLimit = maxTurbidityLimit;
        this.minPhLimit = minPhLimit;
    }

    // Getters
    public String getStationId() { return stationId; }
    public String getName() { return name; }
    public double getTurbidity() { return turbidity; }
    public double getPh() { return ph; }
    public double getTemperature() { return temperature; }
    public double getMaxTurbidityLimit() { return maxTurbidityLimit; }
    public double getMinPhLimit() { return minPhLimit; }

    // Setters (optional but good to have)
    public void setStationId(String stationId) { this.stationId = stationId; }
    public void setName(String name) { this.name = name; }
    public void setTurbidity(double turbidity) { this.turbidity = turbidity; }
    public void setPh(double ph) { this.ph = ph; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setMaxTurbidityLimit(double maxTurbidityLimit) { this.maxTurbidityLimit = maxTurbidityLimit; }
    public void setMinPhLimit(double minPhLimit) { this.minPhLimit = minPhLimit; }
}