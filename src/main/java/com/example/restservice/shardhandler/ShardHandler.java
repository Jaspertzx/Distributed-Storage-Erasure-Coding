/**
 * This interface defines methods for handling the sharding and reconstruction of files.
 * Implementations of this interface are responsible for:
 * - Encoding a file into data and parity shards using error correction techniques.
 * - Decoding and reconstructing a file from its shards, utilizing parity for any missing shards.
 * Methods:
 * - encodeFile: Encodes a given file into data and parity shards.
 * - decodeFile: Reconstructs the original file from a list of shard files.
 * Author: Jasper Tan
 */
package com.example.restservice.shardhandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ShardHandler {
    /**
     * Encodes the input file into multiple file shards.
     * The file is split into smaller parts (shards) for efficient storage or transmission.
     *
     * @param inputFile The file to be encoded (sharded).
     * @return An array of File objects representing the shards of the original file.
     */
    File[] encodeFile(File inputFile) throws IOException;

    /**
     * Decodes a list of byte arrays (file shards) back into the original file.
     * The file shards are reassembled into a single file matching the original.
     *
     * @param shardFiles A list of byte arrays representing the file shards.
     * @param filename The name of the original file.
     * @param originalFileLength The length of the original file before it was sharded.
     * @return The reassembled File object.
     */
    File decodeFile(List<byte[]> shardFiles, String filename, long originalFileLength) throws IOException;
}
