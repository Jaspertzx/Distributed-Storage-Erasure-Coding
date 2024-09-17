/**
 * This service class handles user registration and login operations.
 * It interacts with the UserRepository for database operations and uses BCryptPasswordEncoder to hash and validate
 * passwords.
 * Methods:
 * - registerUser: Registers a new user with a unique username and an encrypted password.
 * - loginUser: Authenticates a user by validating their username and password.
 * Dependencies:
 * - UserRepository: Handles the persistence of User objects in the database.
 * - BCryptPasswordEncoder: Used to securely hash passwords during registration and verify passwords during login.
 * Author: Jasper Tan
 */
package com.example.restservice.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Registers a new user with the provided username and password.
     * The password is securely hashed before saving the user to the database.
     * If a user with the given username already exists, an exception is thrown.
     *
     * @param username The username for the new user.
     * @param password The password for the new user, which will be hashed.
     * @return The saved User object.
     * @throws RuntimeException If a user with the given username already exists.
     */
    public User registerUser(String username, String password) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("User already exists!");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash the password before saving
        return userRepository.save(user);
    }

    /**
     * Authenticates a user with the provided username and password.
     * The method checks if the user exists and if the provided password matches the hashed password in the database.
     * If authentication fails, an exception is thrown.
     *
     * @param username The username of the user attempting to log in.
     * @param password The plain-text password to validate.
     * @return The authenticated User object.
     * @throws RuntimeException If the username does not exist or the password is incorrect.
     */
    public User loginUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials!");
        }
        return user;
    }
}
