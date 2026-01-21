package com.example.smartfarmapp;

import java.util.Date;

public class History {
    private Long id;
    private Long farmId;           // This matches "farm_id" in FarmHistory table
    private Long temperature;      // Changed from "temp" to "temperature"
    private Long groundHumidity;   // Changed from "groundHumid" to "groundHumidity"
    private Long airHumidity;      // Changed from "airHumid" to "airHumidity"
    private String pictureUrl;     // Changed from "picture" to "pictureUrl"
    private String notes;
    private Date recordedAt;       // Changed from "date" to "recordedAt"

    // Required no-argument constructor
    public History() {}

    // Convenience constructor
    public History(Long farmId, Long temperature, Long groundHumidity, Long airHumidity) {
        this.farmId = farmId;
        this.temperature = temperature;
        this.groundHumidity = groundHumidity;
        this.airHumidity = airHumidity;
        this.recordedAt = new Date();
        this.pictureUrl = "";
        this.notes = "";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    // IMPORTANT: Changed from getTemp() to getTemperature()
    public Long getTemperature() { return temperature; }
    public void setTemperature(Long temperature) { this.temperature = temperature; }

    // IMPORTANT: Changed from getGroundHumid() to getGroundHumidity()
    public Long getGroundHumidity() { return groundHumidity; }
    public void setGroundHumidity(Long groundHumidity) { this.groundHumidity = groundHumidity; }

    // IMPORTANT: Changed from getAirHumid() to getAirHumidity()
    public Long getAirHumidity() { return airHumidity; }
    public void setAirHumidity(Long airHumidity) { this.airHumidity = airHumidity; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // IMPORTANT: Changed from getDate() to getRecordedAt()
    public Date getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Date recordedAt) { this.recordedAt = recordedAt; }
}