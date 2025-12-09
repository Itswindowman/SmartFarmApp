package com.example.smartfarmapp;

import com.google.gson.annotations.SerializedName;


public class Vegetation {
    private int id;
    private String name;


    private Float dayTempMin;
    private Float dayTempMax;
    private Float nightTempMin;
    private Float nightTempMax;

    private Float dayGroundHumidMin;
    private Float dayGroundHumidMax;
    private Float nightGroundHumidMin;
    private Float nightGroundHumidMax;

    private Float dayAirHumidMin;
    private Float dayAirHumidMax;
    private Float nightAirHumidMin;
    private Float nightAirHumidMax;

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDayTempMin(Float dayTempMin) {
        this.dayTempMin = dayTempMin;
    }

    public void setDayTempMax(Float dayTempMax) {
        this.dayTempMax = dayTempMax;
    }

    public void setNightTempMin(Float nightTempMin) {
        this.nightTempMin = nightTempMin;
    }

    public void setNightTempMax(Float nightTempMax) {
        this.nightTempMax = nightTempMax;
    }

    public void setDayGroundHumidMin(Float dayGroundHumidMin) {
        this.dayGroundHumidMin = dayGroundHumidMin;
    }

    public void setDayGroundHumidMax(Float dayGroundHumidMax) {
        this.dayGroundHumidMax = dayGroundHumidMax;
    }

    public void setNightGroundHumidMin(Float nightGroundHumidMin) {
        this.nightGroundHumidMin = nightGroundHumidMin;
    }

    public void setNightGroundHumidMax(Float nightGroundHumidMax) {
        this.nightGroundHumidMax = nightGroundHumidMax;
    }

    public void setDayAirHumidMin(Float dayAirHumidMin) {
        this.dayAirHumidMin = dayAirHumidMin;
    }

    public void setDayAirHumidMax(Float dayAirHumidMax) {
        this.dayAirHumidMax = dayAirHumidMax;
    }

    public void setNightAirHumidMin(Float nightAirHumidMin) {
        this.nightAirHumidMin = nightAirHumidMin;
    }

    public void setNightAirHumidMax(Float nightAirHumidMax) {
        this.nightAirHumidMax = nightAirHumidMax;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Float getDayTempMin() {
        return dayTempMin;
    }

    public Float getDayTempMax() {
        return dayTempMax;
    }

    public Float getNightTempMin() {
        return nightTempMin;
    }

    public Float getNightTempMax() {
        return nightTempMax;
    }

    public Float getDayGroundHumidMin() {
        return dayGroundHumidMin;
    }

    public Float getDayGroundHumidMax() {
        return dayGroundHumidMax;
    }

    public Float getNightGroundHumidMin() {
        return nightGroundHumidMin;
    }

    public Float getNightGroundHumidMax() {
        return nightGroundHumidMax;
    }

    public Float getDayAirHumidMin() {
        return dayAirHumidMin;
    }

    public Float getDayAirHumidMax() {
        return dayAirHumidMax;
    }

    public Float getNightAirHumidMin() {
        return nightAirHumidMin;
    }

    public Float getNightAirHumidMax() {
        return nightAirHumidMax;
    }

    public Vegetation(int id, String name, Float dayTempMin, Float dayTempMax, Float nightTempMin, Float nightTempMax, Float dayGroundHumidMin, Float dayGroundHumidMax, Float nightGroundHumidMin, Float nightGroundHumidMax, Float dayAirHumidMin, Float dayAirHumidMax, Float nightAirHumidMin, Float nightAirHumidMax) {
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






}
