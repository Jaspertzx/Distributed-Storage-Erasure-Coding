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
    /**
     * Retrieves an access token to authenticate the upload to the remote storage service.
     *
     * @return A string representing the access token.
     * @throws Exception If an error occurs while retrieving the access token.
     */
    String getAccessToken() throws Exception;

    /**
     * Uploads a single file shard to a remote storage service (e.g., Google Drive).
     *
     * @param string The access token required to authenticate the upload request.
     * @param file The file shard to be uploaded.
     * @param index The index of the file shard, used for tracking or naming purposes.
     * @throws Exception If an error occurs during the file upload process.
     */
    void uploadFileToDrive(String string, File file, int index) throws Exception;

}
