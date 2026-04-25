package com.example.smartfarmapp;

public class User {

    // The database primary key. Using Long (nullable) so Gson omits it on INSERT.
    private Long id;

    private String email;
    private String password;

    // Precondition: None
    // Postcondition: A new empty User object is created
    public User() {}

    // Precondition: email and password are valid Strings
    // Postcondition: A new User object is created with the specified email and password
    public User(String email, String password) {
        this.email    = email;
        this.password = password;
    }

    // ── id ────────────────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: Returns the current value of id
    public Long getId()          { return id; }
    // Precondition: A valid Long id is provided
    // Postcondition: The user's id is updated to the provided value
    public void setId(Long id)   { this.id = id; }

    // ── email ─────────────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: Returns the current value of email
    public String getEmail()             { return email; }
    // Precondition: A valid String email is provided
    // Postcondition: The user's email is updated to the provided value
    public void   setEmail(String email) { this.email = email; }

    // ── password ──────────────────────────────────────────────────────────────
    // Precondition: None
    // Postcondition: Returns the current value of password
    public String getPassword()                { return password; }
    // Precondition: A valid String password is provided
    // Postcondition: The user's password is updated to the provided value
    public void   setPassword(String password) { this.password = password; }

}