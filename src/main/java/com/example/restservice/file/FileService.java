/**
 * This service class provides methods for handling file-related operations, such as saving file metadata
 * and retrieving file shards from the database.
 *
 * It acts as a bridge between the controller layer and the repository layer, encapsulating the business logic
 * for interacting with FileInfo entities.
 *
 * Dependencies:
 * - FileRepository: Repository for performing CRUD operations on FileInfo entities.
 *
 * Author: Jasper Tan
 */
package com.example.restservice.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    private final FileRepository fileRepository;

    @Autowired
    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Saves a FileInfo object to the database.
     * This method is used to store metadata for each file shard after it has been sharded and uploaded.
     *
     * @param fileInfo The FileInfo object containing metadata about the file shard.
     * @return The saved FileInfo object.
     */
    public FileInfo saveFileInfo(FileInfo fileInfo) {
        return fileRepository.save(fileInfo);
    }

    /**
     * Retrieves all file shards associated with a specific user and original filename.
     * This method is used to get the list of file shards for reconstructing a file during retrieval.
     *
     * @param userId The ID of the user who uploaded the file.
     * @param originalFilename The name of the original file before it was sharded.
     * @return A list of FileInfo objects representing the file shards.
     */
    public List<FileInfo> getFileShards(Long userId, String originalFilename) {
        return fileRepository.findByUserIdAndOriginalFilename(userId, originalFilename);
    }
}
