package com.example.smartfarmapp;

public class User {


    int id;

    String email;
    String password;
    int FarmID;
    float latitude;
    float longitude;

    public User()
    {}
    public User(int id, String email, String password, int FarmID, float latitude, float longitude) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.FarmID = FarmID;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getFarmID() {
        return FarmID;
    }

    public void setFarmID(int farmID) {
        FarmID = farmID;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }



}
