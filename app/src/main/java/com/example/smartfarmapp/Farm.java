package com.example.smartfarmapp;

public class Farm {

    private int    id;
    private int    UserID;
    private int    temp;
    private int    groundHumid;
    private int    airHumid;
    private String dateTime;

    // ── Constructors ──────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: A new empty Farm object is created
    public Farm() {}   // required by Gson

    // Precondition: parameters for id, UserID, temp, groundHumid, airHumid, and dateTime are provided
    // Postcondition: A new Farm object is created with the specified values
    public Farm(int id, int UserID, int temp, int groundHumid, int airHumid, String dateTime) {
        this.id          = id;
        this.UserID      = UserID;
        this.temp        = temp;
        this.groundHumid = groundHumid;
        this.airHumid    = airHumid;
        this.dateTime    = dateTime;
    }

    // ── id ────────────────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: Returns the current value of id
    public int  getId()          { return id; }
    // Precondition: A valid integer id is provided
    // Postcondition: The farm's id is updated to the provided value
    public void setId(int id)    { this.id = id; }

    // ── UserID ────────────────────────────────────────────────────────────────
    // BUG FIX: the old getFarmid() mistakenly returned UserID instead of id,
    // and was confusingly named. Replaced with a correctly named getUserID().
    // Precondition: None
    // Postcondition: Returns the current value of UserID
    public int  getUserID()           { return UserID; }
    // Precondition: A valid integer userID is provided
    // Postcondition: The farm's UserID is updated to the provided value
    public void setUserID(int userID) { this.UserID = userID; }

    // ── sensors ───────────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: Returns the current value of temp
    public int  getTemp()                  { return temp; }
    // Precondition: A valid integer temp is provided
    // Postcondition: The farm's temp is updated to the provided value
    public void setTemp(int temp)          { this.temp = temp; }

    // Precondition: None
    // Postcondition: Returns the current value of groundHumid
    public int  getGroundHumid()                    { return groundHumid; }
    // Precondition: A valid integer groundHumid is provided
    // Postcondition: The farm's groundHumid is updated to the provided value
    public void setGroundHumid(int groundHumid)     { this.groundHumid = groundHumid; }

    // Precondition: None
    // Postcondition: Returns the current value of airHumid
    public int  getAirHumid()                  { return airHumid; }
    // Precondition: A valid integer airHumid is provided
    // Postcondition: The farm's airHumid is updated to the provided value
    public void setAirHumid(int airHumid)      { this.airHumid = airHumid; }

    // Precondition: None
    // Postcondition: Returns the current value of dateTime
    public String getDateTime()                  { return dateTime; }
    // Precondition: A valid String dateTime is provided
    // Postcondition: The farm's dateTime is updated to the provided value
    public void   setDateTime(String dateTime)   { this.dateTime = dateTime; }
}