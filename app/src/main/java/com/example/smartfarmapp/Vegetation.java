package com.example.smartfarmapp;

public class Vegetation {

    // --- IMPORTANT FIX ---
    // Changed `int` to `Long`. The `Long` wrapper class can be `null`.
    // When creating a new vegetation, this `id` will be null.
    // Gson will then OMIT the `id` field from the JSON it sends to Supabase,
    // which is the correct way to ask Supabase to generate a new, unique ID.
    private Long id;
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
    public Vegetation() {}

    public void setDayTempMin(float dayTempMin) {
        this.dayTempMin = dayTempMin;
    }

    public void setDayTempMax(float dayTempMax) {
        this.dayTempMax = dayTempMax;
    }

    public void setNightTempMin(float nightTempMin) {
        this.nightTempMin = nightTempMin;
    }

    public void setNightTempMax(float nightTempMax) {
        this.nightTempMax = nightTempMax;
    }

    public void setDayGroundHumidMin(float dayGroundHumidMin) {
        this.dayGroundHumidMin = dayGroundHumidMin;
    }

    public void setDayGroundHumidMax(float dayGroundHumidMax) {
        this.dayGroundHumidMax = dayGroundHumidMax;
    }

    public void setNightGroundHumidMin(float nightGroundHumidMin) {
        this.nightGroundHumidMin = nightGroundHumidMin;
    }

    public void setNightGroundHumidMax(float nightGroundHumidMax) {
        this.nightGroundHumidMax = nightGroundHumidMax;
    }

    public void setDayAirHumidMin(float dayAirHumidMin) {
        this.dayAirHumidMin = dayAirHumidMin;
    }

    public void setDayAirHumidMax(float dayAirHumidMax) {
        this.dayAirHumidMax = dayAirHumidMax;
    }

    public void setNightAirHumidMin(float nightAirHumidMin) {
        this.nightAirHumidMin = nightAirHumidMin;
    }

    public void setNightAirHumidMax(float nightAirHumidMax) {
        this.nightAirHumidMax = nightAirHumidMax;
    }

    // Updated getter and setter to use `Long`.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDayTempMin() {
        return dayTempMin;
    }

    public float getDayTempMax() {
        return dayTempMax;
    }

    public float getNightTempMin() {
        return nightTempMin;
    }

    public float getNightTempMax() {
        return nightTempMax;
    }

    public float getDayGroundHumidMin() {
        return dayGroundHumidMin;
    }

    public float getDayGroundHumidMax() {
        return dayGroundHumidMax;
    }

    public float getNightGroundHumidMin() {
        return nightGroundHumidMin;
    }

    public float getNightGroundHumidMax() {
        return nightGroundHumidMax;
    }

    public float getDayAirHumidMin() {
        return dayAirHumidMin;
    }

    public float getDayAirHumidMax() {
        return dayAirHumidMax;
    }

    public float getNightAirHumidMin() {
        return nightAirHumidMin;
    }

    public float getNightAirHumidMax() {
        return nightAirHumidMax;
    }
}
