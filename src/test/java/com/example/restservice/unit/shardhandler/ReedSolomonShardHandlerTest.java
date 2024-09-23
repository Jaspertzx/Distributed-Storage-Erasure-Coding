package com.example.restservice.unit.shardhandler;

import com.example.restservice.shardhandler.ReedSolomonShardHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ReedSolomonShardHandlerTest {

    private final ReedSolomonShardHandler shardHandler = new ReedSolomonShardHandler();
    private File inputFile;

    @BeforeEach
    public void setUp() throws IOException {
        inputFile = File.createTempFile("testfile", ".txt");
        Files.write(inputFile.toPath(), "abcdefabcdefabcdefabcdefabcdefabcdef".getBytes());
    }

    @Test
    public void testEncodeFileWithZeroByteFile_Success() throws IOException {
        File emptyFile = File.createTempFile("emptyfile", ".txt");
        Files.write(emptyFile.toPath(), new byte[0]);

        File[] shardFiles = shardHandler.encodeFile(emptyFile);

        assertEquals(6, shardFiles.length, "The number of shards should match totalShards");
        for (File shardFile : shardFiles) {
            byte[] shardContent = Files.readAllBytes(shardFile.toPath());
            assertEquals(0, shardContent.length,
                    "Each shard should have the shard size (empty file padded to shard size)");
        }

        emptyFile.delete();
    }

    @Test
    public void testDecodeWithMissingParityShards_Success() throws IOException {
        File[] shardFiles = shardHandler.encodeFile(inputFile);

        shardFiles[4].delete();
        shardFiles[5].delete();

        List<byte[]> shardData = new ArrayList<>();
        for (int i = 0; i < shardFiles.length; i++) {
            if (shardFiles[i].exists()) {
                shardData.add(Files.readAllBytes(shardFiles[i].toPath()));
            } else {
                shardData.add(null);
            }
        }

        File decodedFile = shardHandler.decodeFile(shardData,
                "decoded_testfile_missing_shards.txt", inputFile.length());

        byte[] originalBytes = Files.readAllBytes(inputFile.toPath());
        byte[] decodedBytes = Files.readAllBytes(decodedFile.toPath());
        assertArrayEquals(originalBytes, decodedBytes,
                "The original and decoded file contents should match, even with missing shards");

        decodedFile.delete();
    }

    @Test
    public void testDecodeWithMissingDataShards_Success() throws IOException {
        File[] shardFiles = shardHandler.encodeFile(inputFile);

        shardFiles[1].delete();
        shardFiles[3].delete();

        List<byte[]> shardData = new ArrayList<>();
        for (int i = 0; i < shardFiles.length; i++) {
            if (shardFiles[i].exists()) {
                shardData.add(Files.readAllBytes(shardFiles[i].toPath()));
            } else {
                shardData.add(null);
            }
        }

        File decodedFile = shardHandler.decodeFile(shardData,
                "decoded_testfile_missing_shards.txt", inputFile.length());

        byte[] originalBytes = Files.readAllBytes(inputFile.toPath());
        byte[] decodedBytes = Files.readAllBytes(decodedFile.toPath());
        assertArrayEquals(originalBytes, decodedBytes,
                "The original and decoded file contents should match, even with missing shards");

        decodedFile.delete();
    }

    @Test
    public void testEncodeFileWithNonDivisibleFileSize_Success() throws IOException {
        File oddSizedFile = File.createTempFile("oddsizefile", ".txt");
        Files.write(oddSizedFile.toPath(), "oddsize".getBytes());

        File[] shardFiles = shardHandler.encodeFile(oddSizedFile);

        assertEquals(6, shardFiles.length,
                "The number of shards should match totalShards");

        List<byte[]> shardData = new ArrayList<>();
        for (File shardFile : shardFiles) {
            shardData.add(Files.readAllBytes(shardFile.toPath()));
        }

        File decodedFile = shardHandler.decodeFile(shardData, "decoded_oddsizefile.txt", oddSizedFile.length());

        byte[] originalBytes = Files.readAllBytes(oddSizedFile.toPath());
        byte[] decodedBytes = Files.readAllBytes(decodedFile.toPath());
        assertArrayEquals(originalBytes, decodedBytes,
                "The original and decoded file contents should be the same for non-divisible file size");

        oddSizedFile.delete();
        decodedFile.delete();
    }

    @Test
    public void testEncodeDecodeWithLargeFile_Success() throws IOException {
        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        File largeFile = File.createTempFile("largefile", ".txt");
        Files.write(largeFile.toPath(), largeData);

        File[] shardFiles = shardHandler.encodeFile(largeFile);

        assertEquals(6, shardFiles.length, "The number of shards should match totalShards");

        List<byte[]> shardData = new ArrayList<>();
        for (File shardFile : shardFiles) {
            shardData.add(Files.readAllBytes(shardFile.toPath()));
        }

        File decodedFile = shardHandler.decodeFile(shardData, "decoded_largefile.txt", largeFile.length());

        byte[] originalBytes = Files.readAllBytes(largeFile.toPath());
        byte[] decodedBytes = Files.readAllBytes(decodedFile.toPath());
        assertArrayEquals(originalBytes, decodedBytes,
                "The original and decoded file contents should match for large files");

        largeFile.delete();
        decodedFile.delete();
    }

    @Test
    public void testEncodeDecodeWithLargerFile_Success() throws IOException {
        byte[] largeData = new byte[8192 * 8192];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        File largeFile = File.createTempFile("largefile", ".txt");
        Files.write(largeFile.toPath(), largeData);

        File[] shardFiles = shardHandler.encodeFile(largeFile);

        assertEquals(6, shardFiles.length, "The number of shards should match totalShards");

        List<byte[]> shardData = new ArrayList<>();
        for (File shardFile : shardFiles) {
            shardData.add(Files.readAllBytes(shardFile.toPath()));
        }

        File decodedFile = shardHandler.decodeFile(shardData, "decoded_largefile.txt", largeFile.length());

        byte[] originalBytes = Files.readAllBytes(largeFile.toPath());
        byte[] decodedBytes = Files.readAllBytes(decodedFile.toPath());
        assertArrayEquals(originalBytes, decodedBytes,
                "The original and decoded file contents should match for large files");

        largeFile.delete();
        decodedFile.delete();
    }
}
