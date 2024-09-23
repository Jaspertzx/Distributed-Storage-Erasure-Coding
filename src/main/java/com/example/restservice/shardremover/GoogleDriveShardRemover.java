/**
 * This class handles the deletion of file shards stored on Google Drive using the Google Drive API.
 * It deletes files based on the metadata provided in the list of FileInfo objects.
 * Key Components:
 * - CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN: OAuth2 credentials used to obtain an access token for Google Drive API access.
 * - STORAGE_LOCATIONS_AVAILABLE: A list of available storage locations (folders) where file shards are stored.
 * Author: Jasper Tan
 */
package com.example.restservice.shardremover;

import com.example.restservice.file.FileInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class GoogleDriveShardRemover implements ShardRemover {

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
    private String getAccessToken() throws Exception {
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
    private Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setHttpRequestInitializer(request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("Your App Name")
                .build();
    }

    /**
     * Deletes file shards from Google Drive based on the list of FileInfo objects.
     *
     * @param fileInfos The list of FileInfo objects representing file shards to be deleted.
     * @throws Exception If there is an error during the deletion process.
     */
    @Override
    public void deleteShards(List<FileInfo> fileInfos) {
        try {
            String accessToken = getAccessToken();
            Drive driveService = getDriveService(accessToken);

            ExecutorService executor = Executors.newFixedThreadPool(6); // Pool of 6 threads

            for (FileInfo fileInfo : fileInfos) {
                executor.submit(() -> {
                    try {
                        deleteFileShard(fileInfo, accessToken, driveService);
                    } catch (Exception e) {
                        System.err.println("Error deleting shard: " + e.getMessage());
                    }
                });
            }

            executor.shutdown();
        } catch (Exception e) {
            System.err.println("Error during shard deletion: " + e.getMessage());
        }
    }

    /**
     * Deletes a single file shard from Google Drive.
     *
     * @param fileInfo The FileInfo object representing the file shard to delete.
     * @param accessToken The OAuth2 access token.
     * @param driveService The Drive service object.
     * @throws Exception If there is an error during the deletion process.
     */
    private void deleteFileShard(FileInfo fileInfo, String accessToken, Drive driveService) throws Exception {
        String fileId = getFileIdByName(accessToken, fileInfo.getFileName(), fileInfo.getShardIndex());

        if (fileId != null) {
            driveService.files().delete(fileId).execute();
            System.out.println("Deleted file shard: " + fileInfo.getFileName());
        } else {
            System.err.println("File shard not found: " + fileInfo.getFileName());
        }
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
    private String getFileIdByName(String accessToken, String fileName, int databaseIndex) throws GeneralSecurityException, IOException {
        Drive driveService = getDriveService(accessToken);

        String folderId = STORAGE_LOCATIONS_AVAILABLE[databaseIndex];

        Drive.Files.List request = driveService.files().list()
                .setQ("'" + folderId + "' in parents and name = '" + fileName + "' and trashed = false")
                .setFields("files(id, name)")
                .setPageSize(1);

        List<com.google.api.services.drive.model.File> files = request.execute().getFiles();

        if (files.isEmpty()) {
            System.out.println("File with name '" + fileName + "' not found in folder: " + folderId);
            return null;
        }

        return files.get(0).getId();
    }
}
