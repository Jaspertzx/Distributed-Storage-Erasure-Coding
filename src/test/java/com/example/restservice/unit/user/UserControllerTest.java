package com.example.restservice.unit.user;
import com.example.restservice.user.User;
import com.example.restservice.user.UserController;
import com.example.restservice.user.UserService;
import com.example.restservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testRegisterUser_Success() {
        String username = "testUser";
        String password = "password";

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPassword("encodedPassword");

        when(userService.registerUser(username, password)).thenReturn(mockUser);

        String response = userController.register(username, password);

        assertEquals("User registered successfully!", response);
    }


    @Test
    void testRegisterUser_Failure() {
        String username = "testUser";
        String password = "password";

        doThrow(new RuntimeException("User already exists!")).when(userService).registerUser(username, password);

        String response = userController.register(username, password);

        assertEquals("User already exists!", response);
    }

    @Test
    void testLoginUser_Success() {
        String username = "testUser";
        String password = "password";
        String token = "mockToken";

        when(userService.loginUser(username, password)).thenReturn(new com.example.restservice.user.User());
        when(jwtTokenProvider.generateToken(username)).thenReturn(token);

        ResponseEntity<?> response = userController.login(username, password);

        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        String username = "testUser";
        String password = "password";

        doThrow(new RuntimeException("Invalid credentials")).when(userService).loginUser(username, password);

        ResponseEntity<?> response = userController.login(username, password);

        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
    }
}
