package com.example.restservice.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

public class FileInfoConstructorTest {

    private File testFile;

    @BeforeEach
    public void setUp() throws Exception {
        testFile = File.createTempFile("testFile", ".txt");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("Test file content".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testConstructorPopulatesSha256_Success() throws Exception {
        Long userId = 1L;
        Integer shardIndex = 0;
        String originalFilename = "originalTestFile.txt";
        long originalFileSize = testFile.length();

        FileInfo fileInfo = new FileInfo(testFile, userId, shardIndex, originalFilename, originalFileSize);

        String expectedSha256 = calculateExpectedSha256(testFile);

        assertEquals(expectedSha256, fileInfo.getFileSha256());
    }

    private String calculateExpectedSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        byte[] hash = digest.digest(fileBytes);

        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
