package com.example.smartfarmapp;

public class User {

    // The database primary key. Using Long (nullable) so Gson omits it on INSERT.
    private Long id;

    private String email;
    private String password;

    public User() {}

    public User(String email, String password) {
        this.email    = email;
        this.password = password;
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

}