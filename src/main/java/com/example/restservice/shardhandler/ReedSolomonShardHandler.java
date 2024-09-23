/**
 * This class handles the sharding (encoding) and reconstruction (decoding) of files using Reed-Solomon erasure code
 * algorithm.
 * Reed-Solomon encoding is used to generate parity shards, and decoding reconstructs the original file from the
 * available shards, with parity being used to fill in any missing shards.
 * Author: Jasper Tan
 */
package com.example.restservice.shardhandler;

import com.backblaze.erasure.ReedSolomon;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ReedSolomonShardHandler implements ShardHandler{

    private final int dataShards = 4;

    private final int parityShards = 2;

    private final int totalShards = 6;

    private final ReedSolomon reedSolomon = ReedSolomon.create(dataShards, parityShards);

    private int shardSize;


    /**
     * Encodes the input file into data and parity shards.
     *
     * Formats the data into a 4 x n matrix for matrix multiplication with a 6 x 4 encoding matrix.
     *
     * @param inputFile The file to be encoded.
     * @return ArrayList of encoded shard files.
     * @throws IOException if there's an error reading or writing files.
     */
    public File[] encodeFile(File inputFile) throws IOException {
        calculateAndSetShardSize(inputFile.length());
        byte[] allBytes = fileToBytes(inputFile);
        int fileSize = allBytes.length;

        byte[][] shards = new byte[totalShards][shardSize];

        int byteOffset = 0;
        for (int i = 0; i < dataShards; i++) {
            int bytesToCopy = Math.min(shardSize, fileSize - byteOffset);
            System.arraycopy(allBytes, byteOffset, shards[i], 0, bytesToCopy);
            if (bytesToCopy < shardSize) {
                Arrays.fill(shards[i], bytesToCopy, shardSize, (byte) 0);
            }
            byteOffset += bytesToCopy;
        }

        reedSolomon.encodeParity(shards, 0, shardSize);
        File[] shardFiles = new File[totalShards];

        for (int i = 0; i < totalShards; i++) {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("shard" + i, ".tmp");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(shards[i]);
                    shardFiles[i] = tempFile;
                }
            } catch (IOException e) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
                throw e;
            }
        }
        return shardFiles;
    }

    /**
     * Decodes the provided shard files into the original file.
     *
     * @param shardFiles     A list of byte arrays representing shard files used to reconstruct the original file.
     *                       If any shards are missing, empty shards are initialized for reconstruction using parity.
     * @return The decoded output file stored in memory.
     * @throws IOException             if there's an error handling the file.
     * @throws IllegalStateException   if there are not enough data shards to reconstruct the original file.
     */
    public File decodeFile(List<byte[]> shardFiles, String filename, long originalFileLength)
            throws IOException, IllegalStateException {
        calculateAndSetShardSize(originalFileLength);
        byte[][] shards = new byte[totalShards][];
        boolean[] shardPresent = new boolean[totalShards];
        List<Integer> missingShards = new ArrayList<>();
        int shardCount = 0;

        for (int i = 0; i < totalShards; i++) {
            if (i < shardFiles.size() && shardFiles.get(i) != null) {
                shards[i] = shardFiles.get(i);
                shardPresent[i] = true;
                shardCount++;
            } else {
                shards[i] = new byte[shardSize];
                shardPresent[i] = false;
                missingShards.add(i);
            }
        }

        if (shardCount < dataShards) {
            throw new IllegalStateException("Not enough shards to reconstruct the file");
        }

        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);
        Path tempFilePath = Files.createTempFile("decoded_", "_" + filename);
        File tempFile = tempFilePath.toFile();
        tempFile.deleteOnExit();

        for (Integer missingShardIndex : missingShards) {
            shardFiles.set(missingShardIndex, shards[missingShardIndex]);
        }

        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            for (int i = 0; i < dataShards; i++) {
                fos.write(shards[i], 0, Math.min(shardSize, (int) originalFileLength - i * shardSize));
            }
        } catch (IOException e) {
            tempFile.delete();
            throw e;
        }

        return tempFile;
    }

    /**
     * Converts a given file to a byte array.
     * This method reads the entire file into a byte array, ensuring that the file is fully read.
     *
     * @param file The file to be converted to a byte array.
     * @return A byte array containing the contents of the file.
     * @throws IOException If an I/O error occurs or the file cannot be fully read.
     */
    public static byte[] fileToBytes(File file) throws IOException {
        byte[] originalBytes = new byte[(int) file.length()];
        try (FileInputStream fl = new FileInputStream(file)) {
            int bytesRead = fl.read(originalBytes);
            if (bytesRead != file.length()) {
                throw new IOException("Failed to read the entire file");
            }
        }
        return originalBytes;
    }

    /**
     * Calculates and sets the size of each shard based on the file size and the number of data shards.
     * The shard size is determined by dividing the file size by the number of data shards.
     *
     * @param fileSize The size of the file to be sharded.
     */
    private void calculateAndSetShardSize(long fileSize) {
        shardSize = (int) Math.ceil((double) fileSize / dataShards);
    }
}