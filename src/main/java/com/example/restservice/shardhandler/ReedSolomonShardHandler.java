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
    private static final int totalShards = 6;
    private int shardSize;

    /**
     * Encodes the input file into data and parity shards.
     *
     * @param inputFile The file to be encoded.
     * @return ArrayList of encoded shard files.
     * @throws IOException if there's an error reading or writing files.
     */
    public File[] encodeFile(File inputFile) throws IOException {
        calculateAndSetShardSize(inputFile.length());
        byte[] allBytes = fileToBytes(inputFile);
        int fileSize = allBytes.length;

        int totalShardCapacity = shardSize * dataShards;
        if (fileSize > totalShardCapacity) {
            throw new IllegalArgumentException("File size exceeds shard capacity");
        }

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

        ReedSolomon reedSolomon = ReedSolomon.create(dataShards, parityShards);

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
            System.out.println("Shard " + i + " written to " + tempFile.getPath() + ", size: " + tempFile.length());
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

        ReedSolomon reedSolomon = ReedSolomon.create(dataShards, parityShards);

        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        Path tempFilePath = Files.createTempFile("decoded_", "_" + filename);
        File tempFile = tempFilePath.toFile();
        tempFile.deleteOnExit();

        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            for (int i = 0; i < dataShards; i++) {
                fos.write(shards[i], 0, Math.min(shardSize, (int) originalFileLength - i * shardSize));
            }
        } catch (IOException e) {
            tempFile.delete(); // Clean up if there's an issue during write
            throw e;
        }

        return tempFile;
    }

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

    private void calculateAndSetShardSize(long fileSize) {
        shardSize = (int) Math.ceil((double) fileSize / dataShards);
    }
}