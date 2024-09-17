/**
 * This interface defines methods for uploading file shards to a remote storage service.
 * Implementations of this interface will be responsible for:
 * - Retrieving an OAuth2 access token for authentication.
 * - Uploading file shards to the storage system based on file metadata and storage location.
 * Methods:
 * - getAccessToken: Retrieves an OAuth2 access token for interacting with the storage service.
 * - uploadFileToDrive: Uploads a file shard to the storage service using the provided access token.
 * Author: Jasper Tan
 */
package com.example.restservice.sharduploader;

import java.io.File;

public interface ShardUploader {

    String getAccessToken() throws Exception;

    void uploadFileToDrive(String string, File file, int index) throws Exception;
}
