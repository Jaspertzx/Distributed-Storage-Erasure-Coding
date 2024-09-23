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

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileInfo, Long> {

    /**
     * Retrieves a list of FileInfo entities associated with a specific user and original filename.
     * SQL equivalent:
     *
     * @param userId The ID of the user.
     * @param originalFilename The original name of the file.
     * @return A list of FileInfo entities matching the userId and originalFilename.
     */
    List<FileInfo> findByUserIdAndOriginalFilename(Long userId, String originalFilename);

    /**
     * Retrieves a list of FileInfo entities associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of FileInfo entities for the given userId.
     */
    List<FileInfo> findByUserId(Long userId);

    /**
     * Deletes a FileInfo entity by the userId and original filename.
     *
     * This method is marked as transactional to ensure that the delete operation is atomic and
     * performed within a transaction context.
     *
     * @param userId The ID of the user.
     * @param originalFilename The original name of the file to be deleted.
     */
    @Transactional
    void deleteByUserIdAndOriginalFilename(Long userId, String originalFilename);

    /**
     * Deletes shard that no longer have the same hash values.
     *
     * @param userId The ID of the user.
     * @param corruptedShard The original name of the file to be deleted.
     */
    @Transactional
    void deleteByUserIdAndFileName(Long userId, String corruptedShard);
}
