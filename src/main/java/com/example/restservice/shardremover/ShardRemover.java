/**
 * Interface for removing file shards.
 * Provides a method to delete shards related to a file based on the provided file information.
 * Methods:
 * - deleteShards: Deletes a specific shard
 * Author: Jasper Tan
 */
package com.example.restservice.shardremover;

import com.example.restservice.file.FileInfo;

import java.util.List;

public interface ShardRemover {
    /**
     * Deletes the file shards associated with the provided list of file information.
     * This method is responsible for removing the physical or stored shards of the files.
     *
     * @param fileInfos A list of FileInfo objects containing information about the file shards to be deleted.
     */
    void deleteShards(List<FileInfo> fileInfos);
}
