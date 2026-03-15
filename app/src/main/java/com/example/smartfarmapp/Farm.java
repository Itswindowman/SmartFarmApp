package com.example.smartfarmapp;

public class Farm {

    private int    id;
    private int    UserID;
    private int    temp;
    private int    groundHumid;
    private int    airHumid;
    private String dateTime;

    // ── Constructors ──────────────────────────────────────────────────────────
    public Farm() {}   // required by Gson

    public Farm(int id, int UserID, int temp, int groundHumid, int airHumid, String dateTime) {
        this.id          = id;
        this.UserID      = UserID;
        this.temp        = temp;
        this.groundHumid = groundHumid;
        this.airHumid    = airHumid;
        this.dateTime    = dateTime;
    }

    // ── id ────────────────────────────────────────────────────────────────────
    public int  getId()          { return id; }
    public void setId(int id)    { this.id = id; }

    // ── UserID ────────────────────────────────────────────────────────────────
    // BUG FIX: the old getFarmid() mistakenly returned UserID instead of id,
    // and was confusingly named. Replaced with a correctly named getUserID().
    public int  getUserID()           { return UserID; }
    public void setUserID(int userID) { this.UserID = userID; }

    // ── sensors ───────────────────────────────────────────────────────────────
    public int  getTemp()                  { return temp; }
    public void setTemp(int temp)          { this.temp = temp; }

    public int  getGroundHumid()                    { return groundHumid; }
    public void setGroundHumid(int groundHumid)     { this.groundHumid = groundHumid; }

    public int  getAirHumid()                  { return airHumid; }
    public void setAirHumid(int airHumid)      { this.airHumid = airHumid; }

    public String getDateTime()                  { return dateTime; }
    public void   setDateTime(String dateTime)   { this.dateTime = dateTime; }
}