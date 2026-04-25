package com.example.smartfarmapp;

public class FarmGallery {
    private Long   id;
    private Long   UserID;
    private String URI;
    private String date;

    // Precondition: None
    // Postcondition: A new empty FarmGallery object is created
    public FarmGallery() {}

    // Precondition: userID and uri are valid
    // Postcondition: A new FarmGallery object is created with specified UserID and URI
    public FarmGallery(Long userID, String uri) {
        this.UserID = userID;
        this.URI    = uri;
    }

    // Precondition: None
    // Postcondition: Returns the current value of id
    public Long   getId()                  { return id; }
    // Precondition: A valid Long id is provided
    // Postcondition: The id is updated
    public void   setId(Long id)           { this.id = id; }
    
    // Precondition: None
    // Postcondition: Returns the current value of UserID
    public Long   getUserID()              { return UserID; }
    // Precondition: A valid Long userID is provided
    // Postcondition: The UserID is updated
    public void   setUserID(Long userID)   { this.UserID = userID; }
    
    // Precondition: None
    // Postcondition: Returns the current value of URI
    public String getURI()                 { return URI; }
    // Precondition: A valid String URI is provided
    // Postcondition: The URI is updated
    public void   setURI(String URI)       { this.URI = URI; }
    
    // Precondition: None
    // Postcondition: Returns the current value of date
    public String getDate()                { return date; }
    // Precondition: A valid String date is provided
    // Postcondition: The date is updated
    public void   setDate(String date)     { this.date = date; }

    // Precondition: None
    // Postcondition: Returns true if the URI represents a video file based on its extension, false otherwise
    public boolean isVideo() {
        if (URI == null) return false;
        String lower = URI.toLowerCase();
        return lower.contains(".mp4") || lower.contains(".mov")
                || lower.contains(".avi") || lower.contains(".webm");
    }
}