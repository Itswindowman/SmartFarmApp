package com.example.smartfarmapp;

public class Farm {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

     //need implementation

    public int getFarmid() {
        return UserID;
    }

    public void setUserID(int UserID) {
        this.UserID = UserID;
    }
    private int id;
    private int UserID; //need implementation

    public Farm(int temp, int groundHumid, int airHumid, String dateTime) {
        this.temp = temp;
        this.groundHumid = groundHumid;
        this.airHumid = airHumid;
        this.dateTime = dateTime;
    }

    private int temp;
    private int groundHumid;
    private int airHumid;
    private String dateTime;

    public Farm(int id, int UserID, int temp, int groundHumid, int airHumid, String dateTime) {
        this.id = id;
        this.UserID = UserID;
        this.temp = temp;
        this.groundHumid = groundHumid;
        this.airHumid = airHumid;
        this.dateTime = dateTime;
    }

    // --- REQUIRED: No-Argument Constructor ---
    // Gson needs this to create the object from JSON.
    public Farm() {
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
