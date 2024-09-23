/**
 * This interface defines the contract for retrieving file shards from a remote storage service.
 * Implementations of this interface will be responsible for:
 * - Downloading file shards from a remote storage system (e.g., Google Drive, Azure Blob Server) based on file
 * metadata.
 * Methods:
 * - downloadFilesFromDrive: Downloads the file shards from the storage system using the provided FileInfo objects.
 * Author: Jasper Tan
 */
package com.example.restservice.shardretriever;

import com.example.restservice.file.FileInfo;

import java.util.List;

public interface ShardRetriever {
    /**
     * Downloads multiple file shards based on the provided list of file information.
     * Each file shard is represented as a byte array.
     *
     * @param fileInfos A list of FileInfo objects containing information about the file shards to be downloaded.
     * @return A list of byte arrays representing the downloaded file shards.
     * @throws Exception If an error occurs during the download process.
     */
    List<byte[]> downloadFiles(List<FileInfo> fileInfos) throws Exception;

    /**
     * Checks if a file with the specified name and database index exists in the storage system.
     *
     * @param fileName The name of the file to check.
     * @param databaseIndex The index of the database or storage shard where the file might be stored.
     * @return true if the file exists, false otherwise.
     * @throws Exception If an error occurs during the existence check process.
     */
    boolean fileExists(String fileName, int databaseIndex) throws Exception;
}
