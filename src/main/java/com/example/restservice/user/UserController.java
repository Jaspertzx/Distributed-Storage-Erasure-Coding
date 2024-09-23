/**
 * This controller handles user authentication-related operations such as registration and login.
 * It provides endpoints for user registration, login, and token validation.
 * The controller interacts with the UserService for business logic and uses the JwtTokenProvider for JWT token
 * generation.
 * Endpoints:
 * - GET /auth/test: A test endpoint to check if the service is responsive.
 * - GET /auth/testToken: A test endpoint to verify if a JWT token is valid.
 * - POST /auth/register: Registers a new user by providing a username and password.
 * - POST /auth/login: Authenticates a user and returns a JWT token upon successful login.
 * Dependencies:
 * - UserService: Handles the registration and login logic for users.
 * - JwtTokenProvider: Generates JWT tokens for authenticated users.
 * Author: Jasper Tan
 */
package com.example.restservice.user;

import com.example.restservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class UserController {
    /**
     * A simple inner class to represent the authentication response containing a JWT token.
     */
    class AuthResponse {
        private String token;

        public AuthResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * A test endpoint to check if the service is responsive.
     *
     * @return A simple string message "Responsive".
     */
    @GetMapping("/test")
    public String test() {
        return "Responsive";
    }

    /**
     * A test endpoint to verify if a JWT token is valid.
     *
     * This endpoint can be secured later to check token-based authentication.
     *
     * @return A simple string message "Token Approved".
     */
    @GetMapping("/testToken")
    public String testToken() {
        return "Token Approved";
    }

    /**
     * Registers a new user with the provided username and password.
     *
     * @param username The username for the new user.
     * @param password The password for the new user.
     * @return A message indicating whether the user was registered successfully or an error occurred.
     */
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        System.out.println("Attempting to register user: " + username);
        try {
            userService.registerUser(username, password);
            return "User registered successfully!";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    /**
     * Authenticates a user with the provided username and password.
     *
     * If authentication is successful, a JWT token is generated and returned.
     *
     * @param username The username of the user attempting to log in.
     * @param password The password of the user attempting to log in.
     * @return A ResponseEntity containing the JWT token or an error message if authentication fails.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        try {
            User user = userService.loginUser(username, password);
            String token = jwtTokenProvider.generateToken(username);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
