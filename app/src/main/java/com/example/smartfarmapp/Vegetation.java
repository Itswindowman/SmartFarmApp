package com.example.smartfarmapp;

import com.google.gson.annotations.SerializedName;


public class Vegetation {
    private int id;
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

    public Vegetation(int id, String name, float dayTempMin, float dayTempMax, float nightTempMin, float nightTempMax, float dayGroundHumidMin, float dayGroundHumidMax, float nightGroundHumidMin, float nightGroundHumidMax, float dayAirHumidMin, float dayAirHumidMax, float nightAirHumidMin, float nightAirHumidMax) {
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

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vegetation()
    {}







}
