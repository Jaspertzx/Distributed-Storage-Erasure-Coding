/**
 * The class supports uploading file shards by accepting a file and its metadata, such as the storage location index.
 * Key Components:
 * - CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN: OAuth2 credentials used to obtain an access token for Google Drive API
 * access.
 * - STORAGE_LOCATIONS_AVAILABLE: A list of available storage locations (folders) where file shards are uploaded.
 * Methods:
 * - getAccessToken: Fetches an access token using OAuth2 and the stored refresh token.
 * - uploadFileToDrive: Uploads a file to a specific folder in Google Drive based on the provided storage location index.
 * Author: Jasper Tan
 */
package com.example.restservice.sharduploader;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleDriveShardUploader implements ShardUploader {

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
     * Uploads a file shard to Google Drive in the specified storage location (folder).
     * The file is uploaded to a folder based on the provided storage location index, which maps to a folder in Google Drive.
     *
     * @param accessToken The OAuth2 access token.
     * @param uploadFile The file to be uploaded.
     * @param databaseIndex The index used to determine the folder (storage location).
     * @throws GeneralSecurityException If there is a security-related error.
     * @throws IOException If there is an I/O error during the file upload.
     */
    @Override
    public void uploadFileToDrive(String accessToken, java.io.File uploadFile, int databaseIndex) throws GeneralSecurityException, IOException {
        Drive driveService = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY, null)
                .setHttpRequestInitializer(request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("Your App Name")
                .build();

        File fileMetadata = new File();

        fileMetadata.setName(uploadFile.getName());
        fileMetadata.setParents(Collections.singletonList(STORAGE_LOCATIONS_AVAILABLE[databaseIndex]));
        FileContent mediaContent = new FileContent("application/octet-stream", uploadFile);

        Drive.Files.Create createFile = driveService.files().create(fileMetadata, mediaContent);

        File uploadedFile = createFile.execute();
    }
}

