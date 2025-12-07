package com.example.smartfarmapp;

public class Vegetation {
    private int id;
    private String name;
    private int dayTempMin;
    private int dayTempMax;
    private int nightTempMin;
    private int nightTempMax;
    private int dayGroundHumidMin;
    private int dayGroundHumidMax;
    private int nightGroundHumidMin;

    public Vegetation(int id, String name, int dayTempMin, int dayTempMax, int nightTempMin, int nightTempMax, int dayGroundHumidMin, int dayGroundHumidMax, int nightGroundHumidMin, int nightGroundHumidMax, int dayAirHumidMin, int dayAirHumidMax, int nightAirHumidMin, int nightAirHumidMax) {
        this.id = id;
        this.name = name;
        this.dayTempMin = dayTempMin;
        this.dayTempMax = dayTempMax;
        this.nightTempMin = nightTempMin;
        this.nightTempMax = nightTempMax;
        this.dayGroundHumidMin = dayGroundHumidMin;
        this.dayGroundHumidMax = dayGroundHumidMax;
        this.nightGroundHumidMin = nightGroundHumidMin;
        this.nightGroundHumidMax = nightGroundHumidMax;
        this.dayAirHumidMin = dayAirHumidMin;
        this.dayAirHumidMax = dayAirHumidMax;
        this.nightAirHumidMin = nightAirHumidMin;
        this.nightAirHumidMax = nightAirHumidMax;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDayTempMin(int dayTempMin) {
        this.dayTempMin = dayTempMin;
    }

    public void setDayTempMax(int dayTempMax) {
        this.dayTempMax = dayTempMax;
    }

    public void setNightTempMin(int nightTempMin) {
        this.nightTempMin = nightTempMin;
    }

    public void setNightTempMax(int nightTempMax) {
        this.nightTempMax = nightTempMax;
    }

    public void setDayGroundHumidMin(int dayGroundHumidMin) {
        this.dayGroundHumidMin = dayGroundHumidMin;
    }

    public void setDayGroundHumidMax(int dayGroundHumidMax) {
        this.dayGroundHumidMax = dayGroundHumidMax;
    }

    public void setNightGroundHumidMin(int nightGroundHumidMin) {
        this.nightGroundHumidMin = nightGroundHumidMin;
    }

    public void setNightGroundHumidMax(int nightGroundHumidMax) {
        this.nightGroundHumidMax = nightGroundHumidMax;
    }

    public void setDayAirHumidMin(int dayAirHumidMin) {
        this.dayAirHumidMin = dayAirHumidMin;
    }

    public void setDayAirHumidMax(int dayAirHumidMax) {
        this.dayAirHumidMax = dayAirHumidMax;
    }

    public void setNightAirHumidMin(int nightAirHumidMin) {
        this.nightAirHumidMin = nightAirHumidMin;
    }

    public void setNightAirHumidMax(int nightAirHumidMax) {
        this.nightAirHumidMax = nightAirHumidMax;
    }

    private int nightGroundHumidMax;
    private int dayAirHumidMin;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDayTempMin() {
        return dayTempMin;
    }

    public int getDayTempMax() {
        return dayTempMax;
    }

    public int getNightTempMin() {
        return nightTempMin;
    }

    public int getNightTempMax() {
        return nightTempMax;
    }

    public int getDayGroundHumidMin() {
        return dayGroundHumidMin;
    }

    public int getDayGroundHumidMax() {
        return dayGroundHumidMax;
    }

    public int getNightGroundHumidMin() {
        return nightGroundHumidMin;
    }

    public int getNightGroundHumidMax() {
        return nightGroundHumidMax;
    }

    public int getDayAirHumidMin() {
        return dayAirHumidMin;
    }

    public int getDayAirHumidMax() {
        return dayAirHumidMax;
    }

    public int getNightAirHumidMin() {
        return nightAirHumidMin;
    }

    public int getNightAirHumidMax() {
        return nightAirHumidMax;
    }

    private int dayAirHumidMax;
    private int nightAirHumidMin;
    private int nightAirHumidMax;

    // Constructor, getters, and setters...


}
