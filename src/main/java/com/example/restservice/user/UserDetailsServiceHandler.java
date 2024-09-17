/**
 * This service class implements the UserDetailsService interface and is responsible for loading user-specific data
 * during authentication.
 * It retrieves user information from the database using the UserRepository and integrates with Spring Security for
 * authentication.
 * Key Components:
 * - UserRepository: Used to query the database for user information.
 * - UserDetails: Represents a user in the Spring Security context, containing the username, password, and authorities.
 * Author: Jasper Tan
 */
package com.example.restservice.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class UserDetailsServiceHandler implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                authorities);
    }
}
