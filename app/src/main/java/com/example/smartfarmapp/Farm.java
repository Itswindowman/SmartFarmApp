package com.example.smartfarmapp;

import java.sql.Time;
import java.time.LocalDateTime;

public class Farm {

    private int temp;
    private int groundHumid;
    private int airHumid;
    private String dateTime;

    public Farm(int temp, int groundHumid, int airHumid)
    {
        this.temp = temp;
        this.groundHumid = groundHumid;
        this.airHumid = airHumid;
    }

    // גלאי עשן יש גם

    public int getTemp() { return temp; }
    public int getGroundHumid() { return groundHumid; }
    public int getAirHumid() { return airHumid; }
    public String getDateTime() { return dateTime; }

    public void setTemp(int temp) {
        this.temp = temp;
    }

    public void setGroundHumid(int groundHumid) {
        this.groundHumid = groundHumid;
    }

    public void setAirHumid(int airHumid) {
        this.airHumid = airHumid;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
