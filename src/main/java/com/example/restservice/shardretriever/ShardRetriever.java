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
    List<byte[]> downloadFilesFromDrive(List<FileInfo> fileInfos) throws Exception;
}
