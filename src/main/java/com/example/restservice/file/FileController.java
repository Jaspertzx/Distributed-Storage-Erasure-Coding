/**
 * This class handles file upload and retrieval functionalities for users.
 * It allows users to upload files, which are then sharded and stored via a shard uploader.
 * It also facilitates the retrieval of files by reconstructing the original file from its shards.
 * Dependencies:
 * - FileService: Service layer for managing file-related operations.
 * - UserRepository: Repository to fetch user information from the database.
 * - ShardUploader: Handles file upload to the storage solution.
 * - ShardRetriever: Retrieves file shards from the storage solution.
 * - ShardHandler: Handles encoding (sharding) and decoding of files.
 * - JwtTokenProvider: Used to validate and extract information from JWT tokens.
 * API Endpoints:
 * - POST /file/upload: Uploads a file after sharding and stores the information.
 * - GET /file/retrieve: Retrieves a file by combining its shards.
 * Author: Jasper Tan
 */
package com.example.restservice.file;

import com.example.restservice.shardretriever.ShardRetriever;
import com.example.restservice.shardhandler.ShardHandler;
import com.example.restservice.sharduploader.ShardUploader;
import com.example.restservice.security.JwtTokenProvider;
import com.example.restservice.user.User;
import com.example.restservice.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShardUploader shardUploader;

    @Autowired
    private ShardRetriever shardRetriever;

    @Autowired
    private ShardHandler shardHandler;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private FileRepository fileRepository;

    /**
     * Handles the uploading of files by a user request, using dependency injected by the framework.
     *
     * @param file The file to be uploaded, received as a MultipartFile.
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity indicating success or failure of the file upload.
     * @throws Exception if there is an error during file upload or sharding.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestHeader("Authorization") String authHeader) throws Exception {
        FileInfo fileInfo = new FileInfo();
        String originalFilename = file.getOriginalFilename();
        User user = getUserFromAuthorization(authHeader);

        if (!fileRepository.findByUserIdAndOriginalFilename(user.getId(), originalFilename).isEmpty()) {
            return ResponseEntity.badRequest().body("File already exists");
        }

        String accessToken = shardUploader.getAccessToken();

        File convertedFile = convertMultipartFileToFile(file);
        long originalFileSize = convertedFile.length();

        File[] shardedFiles = shardHandler.encodeFile(convertedFile);

        for (int index = 0; index < shardedFiles.length; index++) {
            try {
                fileInfo = new FileInfo(shardedFiles[index], user.getId(), index, originalFilename, originalFileSize);
                fileService.saveFileInfo(fileInfo);
            } catch (RuntimeException e) {
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
            shardUploader.uploadFileToDrive(accessToken, shardedFiles[index], index);
        }

        return ResponseEntity.ok("File successfully encoded and stored");
    }

    /**
     * Handles the retrieval of a shards created previously.
     *
     * @param filename The name of the file to be retrieved.
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity containing the file or an error message if the file is not found.
     * @throws Exception if there is an error during file retrieval or decoding.
     */
    @GetMapping("/retrieve")
    public ResponseEntity<?> retrieveFile(@RequestParam String filename,
                                          @RequestHeader("Authorization") String authHeader) throws Exception {
        User user = getUserFromAuthorization(authHeader);
        List<FileInfo> fileShards = fileService.getFileShards(user.getId(), filename);

        if (fileShards.isEmpty()) {
            return ResponseEntity.status(404).body("File not found or shards missing");
        }

        List<byte[]> files = shardRetriever.downloadFilesFromDrive(fileShards);
        File file = null;
        try {
            file = shardHandler.decodeFile(files, filename, fileShards.get(0).getOrginalFileSize());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        byte[] fileContent = Files.readAllBytes(file.toPath());

        ByteArrayResource resource = new ByteArrayResource(fileContent);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileContent.length)
                .body(resource);
    }

    /**
     * Extracts the user information from the provided Authorization header.
     *
     * @param authHeader The Authorization header containing the JWT token.
     * @return The User object associated with the token.
     */
    private User getUserFromAuthorization(String authHeader) {
        String authToken = authHeader.split("Bearer ")[1];
        String username = jwtTokenProvider.getUsernameFromJWT(authToken);
        return userRepository.findByUsername(username);
    }

    /**
     * Converts a MultipartFile into a regular File object.
     *
     * @param multipartFile The MultipartFile to be converted.
     * @return The converted File object.
     * @throws IOException if an error occurs during the conversion.
     */
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File convertedFile = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(multipartFile.getBytes());
        }
        return convertedFile;
    }
}
