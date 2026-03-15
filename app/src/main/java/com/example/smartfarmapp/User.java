package com.example.smartfarmapp;

public class User {

    // The database primary key. Using Long (nullable) so Gson omits it on INSERT.
    private Long id;

    private String email;
    private String password;
    private float latitude;
    private float longitude;

    public User() {}

    public User(String email, String password, float latitude, float longitude) {
        this.email     = email;
        this.password  = password;
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    // ── id ────────────────────────────────────────────────────────────────────
    public Long getId()          { return id; }
    public void setId(Long id)   { this.id = id; }

    // ── email ─────────────────────────────────────────────────────────────────
    public String getEmail()             { return email; }
    public void   setEmail(String email) { this.email = email; }

    // ── password ──────────────────────────────────────────────────────────────
    public String getPassword()                { return password; }
    public void   setPassword(String password) { this.password = password; }

    // ── location ──────────────────────────────────────────────────────────────
    public float getLatitude()               { return latitude; }
    public void  setLatitude(float latitude) { this.latitude = latitude; }

    public float getLongitude()                { return longitude; }
    public void  setLongitude(float longitude) { this.longitude = longitude; }
}