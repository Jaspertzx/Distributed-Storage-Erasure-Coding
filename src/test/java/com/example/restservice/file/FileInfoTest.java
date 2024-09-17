package com.example.restservice.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FileInfoTest {

    private File testFile;

    @BeforeEach
    public void setUp() throws IOException {
        testFile = File.createTempFile("testFile", ".txt");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("Test file content".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testConstructorWithFile_Success() throws Exception {
        Long userId = 1L;
        Integer shardIndex = 0;
        String originalFilename = "originalTestFile.txt";
        long originalFileSize = testFile.length();

        FileInfo fileInfo = new FileInfo(testFile, userId, shardIndex, originalFilename, originalFileSize);

        assertEquals(userId, fileInfo.getUserId());
        assertEquals(shardIndex, fileInfo.getShardIndex());
        assertEquals((int) testFile.length(), fileInfo.getByteSize());
        assertNotNull(fileInfo.getFileSha256());
        assertEquals(originalFilename, fileInfo.getOriginalFilename());
        assertEquals(originalFileSize, fileInfo.getOrginalFileSize());
        assertNotNull(fileInfo.getCreatedAt());
    }

    @Test
    public void testCalculateSha256UsingReflection_Success() throws Exception {
        FileInfo fileInfo = new FileInfo();

        Method calculateSha256Method = FileInfo.class.getDeclaredMethod("calculateSha256", File.class);
        calculateSha256Method.setAccessible(true);

        String actualSha256 = (String) calculateSha256Method.invoke(fileInfo, testFile);

        String expectedSha256 = calculateExpectedSha256(testFile);

        assertEquals(expectedSha256, actualSha256);
    }


    private String calculateExpectedSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
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

    @Test
    public void testEmptyConstructor() {
        FileInfo fileInfo = new FileInfo();

        assertNull(fileInfo.getId());
        assertNull(fileInfo.getFileName());
        assertNull(fileInfo.getUserId());
        assertNull(fileInfo.getFileSha256());
        assertNull(fileInfo.getCreatedAt());
    }
}
