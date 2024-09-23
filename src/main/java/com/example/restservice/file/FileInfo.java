/**
 * This class represents the metadata information of a file that has been uploaded by a user.
 * Each file is sharded and stored with its corresponding shard index and SHA-256 hash for verification.
 * The class contains information such as the file's name, user who uploaded it, shard index, file size,
 * and the time it was created.
 * Author: Jasper Tan
 */
package com.example.restservice.file;

import jakarta.persistence.*;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Entity
@Table(name = "[file]")
public class FileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "filename", nullable = false)
    private String fileName;

    @Column(name = "shard_index", nullable = false)
    private Integer shard_index;

    @Column(name = "filesha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "byte_size", nullable = false)
    private Integer byteSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "original_file_size", nullable = false)
    private long orginalFileSize;

    private int shardsRetrievable;

    private int shardsOriginalCount;

    public FileInfo() {
    }

    public FileInfo(File file, Long userId, Integer shard_index, String originalFilename, long orginalFileSize) throws Exception {
        this.userId = userId;
        this.fileName = file.getName();
        this.shard_index = shard_index;
        this.byteSize = (int) file.length(); // size in bytes
        this.fileSha256 = calculateSha256(file); // generate SHA-256 hash
        this.createdAt = LocalDateTime.now(); // set created_at to the current time
        this.originalFilename = originalFilename;
        this.orginalFileSize = orginalFileSize;
    }

    private String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] hash = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hash);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getShardIndex() {
        return shard_index;
    }

    public void setShardIndex(Integer shardIndex) {
        this.shard_index = shardIndex;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public Integer getByteSize() {
        return byteSize;
    }

    public void setByteSize(Integer byteSize) {
        this.byteSize = byteSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getOrginalFileSize() {
        return orginalFileSize;
    }

    public int getShardsRetrievable() {
        return shardsRetrievable;
    }

    public void setShardsRetrievable(int shardsRetrievable) {
        this.shardsRetrievable = shardsRetrievable;
    }

    public void setShardsOriginalCount(int shardsOriginalCount) {
        this.shardsOriginalCount = shardsOriginalCount;
    }
    public int getShardsOriginalCount() {
        return shardsOriginalCount;
    }

    public void setOriginalFilename(String filename) {
    }

    public void setOrginalFileSize(int i) {
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", fileName='" + fileName + '\'' +
                ", shardIndex=" + shard_index +
                ", fileSha256='" + fileSha256 + '\'' +
                ", byteSize=" + byteSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
