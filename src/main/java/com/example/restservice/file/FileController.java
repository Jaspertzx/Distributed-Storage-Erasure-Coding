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
 * - POST /file: Uploads a file after sharding and stores the information.
 * - GET /file: Retrieves a file by combining its shards.
 * - GET /file/list: Retrieves a list of files from a user.
 * - DELETE /file: Deletes a file via its filename.
 * Author: Jasper Tan
 */
package com.example.restservice.file;

import com.example.restservice.shardremover.ShardRemover;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@CrossOrigin(origins = "*")
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
    private ShardRemover shardRemover;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private FileRepository fileRepository;


    /**
     * Handles the uploading of files by a user request, using dependency injected by the framework.
     * The flow is handled this way ensure that the dependencies are injected outside the code, making it run with
     * different backend implementations without knowing the actual implementations.
     *
     * Threading was done here due to high I/O limitations, so parallelizing the upload process allows multiple file
     * shards to be uploaded simultaneously, improving overall performance and reducing the time spent waiting for
     * individual I/O operations to complete.
     *
     * @param file The file to be uploaded, received as a MultipartFile.
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity indicating success or failure of the file upload.
     */
    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestHeader("Authorization") String authHeader) {
        User user = getUserFromAuthorization(authHeader);
        String originalFilename = file.getOriginalFilename();
        if (fileExistsForUser(user.getId(), originalFilename)) {
            return ResponseEntity.badRequest().body("File already exists");
        }

        try {
            File convertedFile = convertMultipartFileToFile(file);
            long originalFileSize = convertedFile.length();
            File[] shardedFiles = shardHandler.encodeFile(convertedFile);
            ExecutorService executorService = Executors.newFixedThreadPool(6);
            String accessToken = shardUploader.getAccessToken();
            List<Future<Void>> futures = uploadShards(shardedFiles, accessToken, user, originalFilename,
                    originalFileSize, executorService);

            waitForCompletion(futures);
            executorService.shutdown();
            return ResponseEntity.ok("File successfully encoded and stored");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Handles the retrieval of file whose shards were created previously.
     * The flow is handled this way ensure that the dependencies are injected outside the code, making it run with
     * different backend implementations without knowing the actual implementations.
     *
     * Threading is done on the implementation end, and should be up to the implementor of shardRetriever to decide.
     *
     * @param filename The name of the file to be retrieved.
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity containing the file or an error message if the file is not found.
     * @throws Exception if there is an error during file retrieval or decoding.
     */
    @GetMapping
    public ResponseEntity<?> retrieveFile(@RequestParam String filename,
                                          @RequestHeader("Authorization") String authHeader) throws Exception {
        User user = getUserFromAuthorization(authHeader);
        List<FileInfo> fileShards = fileService.getFileShards(user.getId(), filename);
        if (fileShards.isEmpty()) {
            return ResponseEntity.status(404).body("File not found or shards missing");
        }

        List<byte[]> files = shardRetriever.downloadFiles(fileShards);
        List<Integer> missingFileShardsIndex = new ArrayList<>();
        for (int index = 0; index < fileShards.size(); index++) {
            if (index < files.size() && files.get(index) == null) {
                missingFileShardsIndex.add(index);
            }
        }

        File file = null;
        try {
            file = shardHandler.decodeFile(files, filename, fileShards.get(0).getOrginalFileSize());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // Rebuild Missing Shards
        File[] shards = shardHandler.encodeFile(file);
        for (Integer index : missingFileShardsIndex) {
            FileInfo corruptedShard = fileShards.get(0);
            for (FileInfo fileShard : fileShards) {
                if (Objects.equals(fileShard.getShardIndex(), index)) {
                    corruptedShard = fileShard;
                    break;
                }
            }
            fileRepository.deleteByUserIdAndFileName(user.getId(), corruptedShard.getFileName());
            uploadShard(shards[index], index, shardUploader.getAccessToken(), user, filename,
                    fileShards.get(0).getOrginalFileSize());
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
     * Returns all files created by user.
     * The validation stage is necessary as the number of shards present to Users will be provided.
     *
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity containing the file or an error message if the file is not found.
     * @throws Exception if there is an error during file retrieval or decoding.
     */
    @GetMapping("/list")
    public ResponseEntity<?> listFile(@RequestHeader("Authorization") String authHeader) throws Exception {
        User user = getUserFromAuthorization(authHeader);
        List<FileInfo> fileShards = fileRepository.findByUserId(user.getId());
        Map<String, FileInfo> uniqueFiles = new HashMap<>();

        for (FileInfo fileShard : fileShards) {
            if (!shardRetriever.fileExists(fileShard.getFileName(), fileShard.getShardIndex())) {
                continue;
            }
            if (!uniqueFiles.containsKey(fileShard.getOriginalFilename())) {
                fileShard.setShardsOriginalCount(6);
                fileShard.setShardsRetrievable(1);
                uniqueFiles.put(fileShard.getOriginalFilename(), fileShard);
            }
            else {
                FileInfo file = uniqueFiles.get(fileShard.getOriginalFilename());
                file.setShardsRetrievable(file.getShardsRetrievable() + 1);
            }
        }

        List<FileInfo> oneShardPerFile = new ArrayList<>(uniqueFiles.values());
        return ResponseEntity.ok(oneShardPerFile);
    }

    /**
     * Delete file specified by User.
     * File is deleted on the database table first, before deleting using the `shardRemover` implementation to prevent
     * a scenario where a user initializes the delete for a file and calls the `GET` mapping for file, causing a
     * "ghost" file to appear on their webpage. (Ghost file is a file that is in the midst of deleting).
     *
     * @param authHeader The Authorization header containing the JWT token.
     * @return A ResponseEntity containing the file or an error message if the file is not found.
     * @throws Exception if there is an error during file retrieval or decoding.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteFile(@RequestParam String filename,
                                        @RequestHeader("Authorization") String authHeader) {
        User user = getUserFromAuthorization(authHeader);
        List<FileInfo> fileShards = fileRepository.findByUserIdAndOriginalFilename(user.getId(), filename);

        fileRepository.deleteByUserIdAndOriginalFilename(user.getId(), filename);
        shardRemover.deleteShards(fileShards);

        return ResponseEntity.ok("File deleted successfully");
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

    /**
     * Checks if a file with the given filename exists for a specific user.
     *
     * @param userId the ID of the user
     * @param originalFilename the name of the original file
     * @return true if the file exists, false otherwise
     */
    private boolean fileExistsForUser(Long userId, String originalFilename) {
        return !fileRepository.findByUserIdAndOriginalFilename(userId, originalFilename).isEmpty();
    }

    /**
     * Submits tasks to upload file shards in parallel using the provided ExecutorService.
     *
     * @param shardedFiles an array of file shards to be uploaded
     * @param accessToken the access token for authorization
     * @param user the user uploading the file
     * @param originalFilename the name of the original file
     * @param originalFileSize the size of the original file
     * @param executorService the executor service for managing concurrent tasks
     * @return a list of Future objects representing the status of the shard upload tasks
     */
    private List<Future<Void>> uploadShards(File[] shardedFiles, String accessToken, User user,
                                            String originalFilename, long originalFileSize,
                                            ExecutorService executorService) {
        List<Future<Void>> futures = new ArrayList<>();
        for (int index = 0; index < shardedFiles.length; index++) {
            final int shardIndex = index;
            futures.add(executorService.submit(() -> uploadShard(shardedFiles[shardIndex], shardIndex, accessToken,
                    user, originalFilename, originalFileSize)));
        }
        return futures;
    }

    /**
     * Uploads a single shard of a file to the server.
     *
     * @param shardFile the file shard to be uploaded
     * @param shardIndex the index of the shard
     * @param accessToken the access token for authorization
     * @param user the user uploading the file
     * @param originalFilename the name of the original file
     * @param originalFileSize the size of the original file
     * @return null after successfully uploading the shard
     * @throws Exception if an error occurs during the upload process
     */
    private Void uploadShard(File shardFile, int shardIndex, String accessToken, User user,
                             String originalFilename, long originalFileSize) throws Exception {
        try {
            FileInfo shardFileInfo = new FileInfo(shardFile, user.getId(), shardIndex, originalFilename,
                    originalFileSize);
            fileService.saveFileInfo(shardFileInfo);
            shardUploader.uploadFileToDrive(accessToken, shardFile, shardIndex);
        } catch (RuntimeException e) {
            throw new Exception("Error uploading shard " + shardIndex + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Waits for all shard upload tasks to complete.
     *
     * @param futures a list of Future objects representing the shard upload tasks
     * @throws Exception if any task fails or throws an exception
     */
    private void waitForCompletion(List<Future<Void>> futures) throws Exception {
        for (Future<Void> future : futures) {
            future.get();
        }
    }
}
