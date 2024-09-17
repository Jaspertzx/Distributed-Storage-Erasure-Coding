package com.example.restservice.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

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

        when(userRepository.findByUsername(username)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = userService.registerUser(username, password);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        verify(userRepository).save(any(User.class));
    }


    @Test
    void testRegisterUser_UserAlreadyExists() {
        String username = "testUser";
        when(userRepository.findByUsername(username)).thenReturn(new User());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(username, "password"));
        assertEquals("User already exists!", exception.getMessage());
    }

    @Test
    void testLoginUser_Success() {
        String username = "testUser";
        String password = "password";
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPassword("encodedPassword");

        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);

        User result = userService.loginUser(username, password);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        String username = "testUser";
        String password = "password";
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPassword("encodedPassword");

        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.loginUser(username, password));
        assertEquals("Invalid credentials!", exception.getMessage());
    }
}
