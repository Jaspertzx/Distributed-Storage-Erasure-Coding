package com.example.restservice.endtoend.user;

import com.example.restservice.user.User;
import com.example.restservice.user.UserRepository;
import com.example.restservice.user.UserService;
import com.example.restservice.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerEndToEndTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/auth";
        restTemplate = new RestTemplate();
        userRepository.deleteByUsername("testUser");
    }
    @AfterEach
    void tearDown() {
        userRepository.deleteByUsername("testUser");
    }

    @Test
    void testRegisterAndLoginUser_Success() {
        String registerUrl = baseUrl + "/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> registerRequest = new HttpEntity<>("username=testUser&password=testPassword", headers);
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(registerUrl, registerRequest, String.class);

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertEquals("User registered successfully!", registerResponse.getBody());

        String loginUrl = baseUrl + "/login";
        HttpEntity<String> loginRequest = new HttpEntity<>("username=testUser&password=testPassword", headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(loginUrl, loginRequest, Map.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().get("token"), "JWT token should be present in the response");

        String token = (String) loginResponse.getBody().get("token");
        User newlyCreatedUser = userRepository.findByUsername(jwtTokenProvider.getUsernameFromJWT(token));

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setBearerAuth(token);
        HttpEntity<Void> tokenRequest = new HttpEntity<>(tokenHeaders);

        ResponseEntity<String> tokenResponse = restTemplate.exchange(baseUrl + "/testToken", HttpMethod.GET, tokenRequest, String.class);

        assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
        assertEquals("Token Approved", tokenResponse.getBody());
    }

    @Test
    void testLoginUser_Failure_InvalidCredentials() {
        // Step 1: Register a user first
        userService.registerUser("testUser", "testPassword");
        User newlyCreatedUser = userRepository.findByUsername("testUser");

        // Step 2: Try to login with incorrect credentials
        String loginUrl = baseUrl + "/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> loginRequest = new HttpEntity<>("username=testUser&password=wrongPassword", headers);
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.postForEntity(loginUrl, loginRequest, String.class);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void testTokenValidation_Failure_WithoutToken() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.getForEntity(baseUrl + "/testToken", String.class);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
