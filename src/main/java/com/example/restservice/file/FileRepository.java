/**
 * This repository interface provides methods for interacting with the FileInfo entity in the database.
 * It extends JpaRepository, allowing for standard CRUD operations and additional custom queries for retrieving
 * file metadata based on specific criteria.
 * Custom query methods:
 * - findByFileName: Retrieves a list of FileInfo objects by the file name (specific to file shards).
 * - findByUserIdAndOriginalFilename: Retrieves a list of FileInfo objects based on the user ID and original filename.
 * Author: Jasper Tan
 */
package com.example.restservice.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileInfo, Long> {

    List<FileInfo> findByFileName(String fileName);

    List<FileInfo> findByUserIdAndOriginalFilename(Long userId, String originalFilename);
}
