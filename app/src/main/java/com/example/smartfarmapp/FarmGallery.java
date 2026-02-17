package com.example.smartfarmapp;

public class FarmGallery {
    private Long   id;
    private Long   UserID;
    private String URI;
    private String date;

    public FarmGallery() {}

    public FarmGallery(Long userID, String uri) {
        this.UserID = userID;
        this.URI    = uri;
    }

    public Long   getId()                  { return id; }
    public void   setId(Long id)           { this.id = id; }
    public Long   getUserID()              { return UserID; }
    public void   setUserID(Long userID)   { this.UserID = userID; }
    public String getURI()                 { return URI; }
    public void   setURI(String URI)       { this.URI = URI; }
    public String getDate()                { return date; }
    public void   setDate(String date)     { this.date = date; }

    public boolean isVideo() {
        if (URI == null) return false;
        String lower = URI.toLowerCase();
        return lower.contains(".mp4") || lower.contains(".mov")
                || lower.contains(".avi") || lower.contains(".webm");
    }
}