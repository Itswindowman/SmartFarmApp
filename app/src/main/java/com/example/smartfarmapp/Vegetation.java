package com.example.smartfarmapp;

public class Vegetation {

    // --- IMPORTANT FIX ---
    // Changed `int` to `Long`. The `Long` wrapper class can be `null`.
    // When creating a new vegetation, this `id` will be null.
    // Gson will then OMIT the `id` field from the JSON it sends to Supabase,
    // which is the correct way to ask Supabase to generate a new, unique ID.
    private Long id;

    // NEW: ties this vegetation to the user who created it (private vegetations).
    // Must be set before calling VegetationRepo.addVegetation(), or Supabase
    // will insert a row with UserID = NULL, invisible to everyone.
    private Long UserID;

    private String name;

    private float dayTempMin;
    private float dayTempMax;
    private float nightTempMin;
    private float nightTempMax;

    private float dayGroundHumidMin;
    private float dayGroundHumidMax;
    private float nightGroundHumidMin;
    private float nightGroundHumidMax;

    private float dayAirHumidMin;
    private float dayAirHumidMax;
    private float nightAirHumidMin;
    private float nightAirHumidMax;

    // A no-argument constructor is required for Gson and your MainFragment.
    // Precondition: None
    // Postcondition: A new empty Vegetation object is created
    public Vegetation() {}

    // Precondition: A valid float dayTempMin is provided
    // Postcondition: The vegetation's dayTempMin is updated
    public void setDayTempMin(float dayTempMin) {
        this.dayTempMin = dayTempMin;
    }

    // Precondition: A valid float dayTempMax is provided
    // Postcondition: The vegetation's dayTempMax is updated
    public void setDayTempMax(float dayTempMax) {
        this.dayTempMax = dayTempMax;
    }

    // Precondition: A valid float nightTempMin is provided
    // Postcondition: The vegetation's nightTempMin is updated
    public void setNightTempMin(float nightTempMin) {
        this.nightTempMin = nightTempMin;
    }

    // Precondition: A valid float nightTempMax is provided
    // Postcondition: The vegetation's nightTempMax is updated
    public void setNightTempMax(float nightTempMax) {
        this.nightTempMax = nightTempMax;
    }

    // Precondition: A valid float dayGroundHumidMin is provided
    // Postcondition: The vegetation's dayGroundHumidMin is updated
    public void setDayGroundHumidMin(float dayGroundHumidMin) {
        this.dayGroundHumidMin = dayGroundHumidMin;
    }

    // Precondition: A valid float dayGroundHumidMax is provided
    // Postcondition: The vegetation's dayGroundHumidMax is updated
    public void setDayGroundHumidMax(float dayGroundHumidMax) {
        this.dayGroundHumidMax = dayGroundHumidMax;
    }

    // Precondition: A valid float nightGroundHumidMin is provided
    // Postcondition: The vegetation's nightGroundHumidMin is updated
    public void setNightGroundHumidMin(float nightGroundHumidMin) {
        this.nightGroundHumidMin = nightGroundHumidMin;
    }

    // Precondition: A valid float nightGroundHumidMax is provided
    // Postcondition: The vegetation's nightGroundHumidMax is updated
    public void setNightGroundHumidMax(float nightGroundHumidMax) {
        this.nightGroundHumidMax = nightGroundHumidMax;
    }

    // Precondition: A valid float dayAirHumidMin is provided
    // Postcondition: The vegetation's dayAirHumidMin is updated
    public void setDayAirHumidMin(float dayAirHumidMin) {
        this.dayAirHumidMin = dayAirHumidMin;
    }

    // Precondition: A valid float dayAirHumidMax is provided
    // Postcondition: The vegetation's dayAirHumidMax is updated
    public void setDayAirHumidMax(float dayAirHumidMax) {
        this.dayAirHumidMax = dayAirHumidMax;
    }

    // Precondition: A valid float nightAirHumidMin is provided
    // Postcondition: The vegetation's nightAirHumidMin is updated
    public void setNightAirHumidMin(float nightAirHumidMin) {
        this.nightAirHumidMin = nightAirHumidMin;
    }

    // Precondition: A valid float nightAirHumidMax is provided
    // Postcondition: The vegetation's nightAirHumidMax is updated
    public void setNightAirHumidMax(float nightAirHumidMax) {
        this.nightAirHumidMax = nightAirHumidMax;
    }

    // Updated getter and setter to use `Long`.
    // Precondition: None
    // Postcondition: Returns the current value of id
    public Long getId() {
        return id;
    }

    // Precondition: A valid Long id is provided
    // Postcondition: The vegetation's id is updated
    public void setId(Long id) {
        this.id = id;
    }

    // Precondition: None
    // Postcondition: Returns the current value of UserID
    public Long getUserID() {
        return UserID;
    }

    // Precondition: A valid Long userID is provided
    // Postcondition: The vegetation's owning UserID is updated
    public void setUserID(Long userID) {
        this.UserID = userID;
    }

    // Precondition: None
    // Postcondition: Returns the current value of name
    public String getName() {
        return name;
    }

    // Precondition: A valid String name is provided
    // Postcondition: The vegetation's name is updated
    public void setName(String name) {
        this.name = name;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayTempMin
    public float getDayTempMin() {
        return dayTempMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayTempMax
    public float getDayTempMax() {
        return dayTempMax;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightTempMin
    public float getNightTempMin() {
        return nightTempMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightTempMax
    public float getNightTempMax() {
        return nightTempMax;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayGroundHumidMin
    public float getDayGroundHumidMin() {
        return dayGroundHumidMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayGroundHumidMax
    public float getDayGroundHumidMax() {
        return dayGroundHumidMax;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightGroundHumidMin
    public float getNightGroundHumidMin() {
        return nightGroundHumidMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightGroundHumidMax
    public float getNightGroundHumidMax() {
        return nightGroundHumidMax;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayAirHumidMin
    public float getDayAirHumidMin() {
        return dayAirHumidMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of dayAirHumidMax
    public float getDayAirHumidMax() {
        return dayAirHumidMax;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightAirHumidMin
    public float getNightAirHumidMin() {
        return nightAirHumidMin;
    }

    // Precondition: None
    // Postcondition: Returns the current value of nightAirHumidMax
    public float getNightAirHumidMax() {
        return nightAirHumidMax;
    }
}