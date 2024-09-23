/**
 * This class handles the retrieval of file shards stored on Google Drive using the Google Drive API.
 * Files are retrieved from specific storage locations (folders) and verified using a SHA-256 hash.
 * Key Components:
 * - CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN: OAuth2 credentials used to obtain an access token for Google Drive API
 * access.
 * - STORAGE_LOCATIONS_AVAILABLE: A list of available storage locations (folders) where file shards are stored.
 * Author: Jasper Tan
 */
package com.example.restservice.shardretriever;

import com.example.restservice.file.FileInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class GoogleDriveShardRetriever implements ShardRetriever {

    @Value("${client.id}")
    private String CLIENT_ID;

    @Value("${client.secret}")
    private String CLIENT_SECRET;

    @Value("${refresh.token}")
    private String REFRESH_TOKEN;

    @Value("${storage.locations.available}")
    private String[] STORAGE_LOCATIONS_AVAILABLE;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Retrieves an OAuth2 access token using the stored refresh token.
     *
     * @return The access token as a String.
     * @throws Exception If there is an error during the token request.
     */
    public String getAccessToken() throws Exception {
        GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                REFRESH_TOKEN,
                CLIENT_ID,
                CLIENT_SECRET
        ).execute();

        return tokenResponse.getAccessToken();
    }

    /**
     * Creates a Google Drive service object to interact with the Drive API.
     *
     * @param accessToken The OAuth2 access token.
     * @return A configured Drive service object.
     * @throws GeneralSecurityException If there is a security-related error.
     * @throws IOException If there is an I/O error.
     */
    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setHttpRequestInitializer(request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("Your App Name")
                .build();
    }

    /**
     * Retrieves the metadata of a file stored on Google Drive by its file ID.
     *
     * @param driveService The Google Drive service object.
     * @param fileId The ID of the file to retrieve metadata for.
     * @return The file metadata as a File object.
     * @throws IOException If there is an error during the request.
     */
    public File retrieveFileMetadata(Drive driveService, String fileId) throws IOException {
        return driveService.files().get(fileId).execute();
    }

    /**
     * Downloads file shards from Google Drive based on the list of FileInfo objects.
     *
     * The file shards are downloaded into memory and verified using a SHA-256 hash.
     * If the hash matches, the file shard is added to the list; otherwise, a null value is added.
     *
     * @param fileInfos The list of FileInfo objects representing file shards to be downloaded.
     * @return A list of byte arrays representing the downloaded file shards.
     * @throws Exception If there is an error during the download process.
     */
    @Override
    public List<byte[]> downloadFiles(List<FileInfo> fileInfos) throws Exception {
        String accessToken = getAccessToken();
        Drive driveService = getDriveService(accessToken);

        List<byte[]> downloadedFiles = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(6); // Pool of 6 threads
        List<Future<byte[]>> futures = new ArrayList<>();

        for (FileInfo fileInfo : fileInfos) {
            futures.add(executor.submit(() -> downloadFile(fileInfo, accessToken, driveService)));
        }

        for (Future<byte[]> future : futures) {
            try {
                downloadedFiles.add(future.get());
            } catch (Exception e) {
                downloadedFiles.add(null);
            }
        }

        executor.shutdown();
        return downloadedFiles;
    }

    /**
     * Checks if a file exists in Google Drive by file name and storage location (folder).
     *
     * The method searches a specific folder (based on the storage location) for a file with the specified name.
     *
     * @param fileName The name of the file to search for.
     * @param databaseIndex The index used to determine the folder (storage location).
     * @return true if the file exists, false otherwise.
     * @throws Exception If there is a security-related error or an I/O error during the search.
     */
    @Override
    public boolean fileExists(String fileName, int databaseIndex) throws Exception {
        String accessToken = getAccessToken();
        Drive driveService = getDriveService(accessToken);

        String folderId = STORAGE_LOCATIONS_AVAILABLE[databaseIndex];

        Drive.Files.List request = driveService.files().list()
                .setQ("'" + folderId + "' in parents and name = '" + fileName + "' and trashed = false")
                .setFields("files(id, name)")
                .setPageSize(1);  // Limit to 1 result since we just want to check if it exists

        List<File> files = request.execute().getFiles();

        return !files.isEmpty();  // Return true if any file is found
    }

    /**
     * Downloads a file shard from a remote service (e.g., Google Drive) based on the provided file information.
     * It verifies the integrity of the downloaded file by checking its hash against the stored hash.
     *
     * @param fileInfo The information about the file shard to be downloaded, including its name and shard index.
     * @param accessToken The access token used for authenticating the request to the remote service.
     * @param driveService The Drive service used to interact with the remote storage (e.g., Google Drive).
     * @return A byte array containing the downloaded file shard, or null if the file doesn't exist or invalid hash.
     * @throws Exception If an error occurs during file download or hash validation.
     */
    private byte[] downloadFile(FileInfo fileInfo, String accessToken, Drive driveService) throws Exception {
        String fileId = getFileIdByName(accessToken, fileInfo.getFileName(), fileInfo.getShardIndex());

        if (fileId == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HttpResponse fileResponse = driveService.files().get(fileId).executeMedia();
            fileResponse.download(outputStream);

            byte[] downloadedBytes = outputStream.toByteArray();
            if (!isFileHashValid(downloadedBytes, fileInfo.getFileSha256())) {
                return null;
            }

            return downloadedBytes;
        }
    }

    /**
     * Validates the SHA-256 hash of the downloaded file data against the expected hash value.
     *
     * @param fileData The byte array containing the downloaded file data.
     * @param expectedHash The expected SHA-256 hash of the file.
     * @return true if the file's hash matches the expected hash, false otherwise.
     * @throws NoSuchAlgorithmException If the SHA-256 hashing algorithm is not available.
     */
    private boolean isFileHashValid(byte[] fileData, String expectedHash) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileHashBytes = digest.digest(fileData);
        String fileHash = HexFormat.of().formatHex(fileHashBytes);
        return fileHash.equals(expectedHash);
    }

    /**
     * Retrieves the Google Drive file ID by file name and storage location (folder).
     *
     * The method searches a specific folder (based on the storage location) for a file with the specified name.
     *
     * @param accessToken The OAuth2 access token.
     * @param fileName The name of the file to search for.
     * @param databaseIndex The index used to determine the folder (storage location).
     * @return The file ID as a String, or null if the file is not found.
     * @throws GeneralSecurityException If there is a security-related error.
     * @throws IOException If there is an I/O error during the search.
     */
    public String getFileIdByName(String accessToken, String fileName, int databaseIndex) throws GeneralSecurityException, IOException {
        Drive driveService = getDriveService(accessToken);

        String folderId = STORAGE_LOCATIONS_AVAILABLE[databaseIndex];

        Drive.Files.List request = driveService.files().list()
                .setQ("'" + folderId + "' in parents and name = '" + fileName + "' and trashed = false")
                .setFields("files(id, name)")
                .setPageSize(1);

        List<File> files = request.execute().getFiles();

        if (files.isEmpty()) {
            System.out.println("File with name '" + fileName + "' not found in folder: " + folderId);
            return null;
        }

        return files.get(0).getId();
    }
}
