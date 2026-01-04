package com.example.smartfarmapp;

public class Farm {

    private int temp;
    private int groundHumid;
    private int airHumid;
    private String dateTime;

    // --- REQUIRED: No-Argument Constructor ---
    // Gson needs this to create the object from JSON.
    public Farm() {
    }

    public Farm(int temp, int groundHumid, int airHumid) {
        this.temp = temp;
        this.groundHumid = groundHumid;
        this.airHumid = airHumid;
    }

    public int getTemp() { return temp; }
    public void setTemp(int temp) { this.temp = temp; }

    public int getGroundHumid() { return groundHumid; }
    public void setGroundHumid(int groundHumid) { this.groundHumid = groundHumid; }

    public int getAirHumid() { return airHumid; }
    public void setAirHumid(int airHumid) { this.airHumid = airHumid; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
}
