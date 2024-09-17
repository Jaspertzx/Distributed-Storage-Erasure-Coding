/**
 * This class represents a User entity that is mapped to the "user" table in the database.
 * Each User object contains information about a user, including a unique username and a password.
 * Author: Jasper Tan
 */
package com.example.restservice.user;

import jakarta.persistence.*;

@Entity
@Table(name = "[user]")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;


    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(long l) {
    }
}
