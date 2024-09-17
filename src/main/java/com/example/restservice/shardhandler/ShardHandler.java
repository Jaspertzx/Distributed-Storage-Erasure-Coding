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
    File[] encodeFile(File inputFile) throws IOException;
    File decodeFile(List<byte[]> shardFiles, String filename, long originalFileLength) throws IOException;
}
