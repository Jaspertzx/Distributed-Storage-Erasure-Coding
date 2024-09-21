package com.example.restservice.integration.user;

import com.example.restservice.user.User;
import com.example.restservice.user.UserRepository;
import com.example.restservice.user.UserService;
import com.example.restservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    public void setUp() {
        User testUser = new User();
        testUser.setUsername("testUser");
        testUser.setPassword("password");
        userRepository.save(testUser);
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        // Test the register endpoint with valid data
        mockMvc.perform(post("/auth/register")
                        .param("username", "newUser")
                        .param("password", "newPassword")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User registered successfully!")));
    }

    @Test
    void testRegisterUser_Failure() throws Exception {
        // Test the register endpoint with a user that already exists
        mockMvc.perform(post("/auth/register")
                        .param("username", "testUser")
                        .param("password", "password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User already exists!")));
    }

    @Test
    void testLoginUser_Success() throws Exception {
        // Test the login endpoint with valid credentials
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .param("username", "testUser")
                        .param("password", "password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("Login response: " + response);
    }

    @Test
    void testLoginUser_InvalidCredentials() throws Exception {
        // Test the login endpoint with invalid credentials
        mockMvc.perform(post("/auth/login")
                        .param("username", "testUser")
                        .param("password", "wrongPassword")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid credentials")));
    }
}
