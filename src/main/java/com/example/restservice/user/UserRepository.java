/**
 * This repository interface provides methods for interacting with the User entity in the database.
 * It extends JpaRepository, allowing for standard CRUD operations and includes a custom query method for finding a
 * user by username.
 * Custom Query Method:
 * - findByUsername: Retrieves a User object by its unique username.
 * Author: Jasper Tan
 */
package com.example.restservice.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
