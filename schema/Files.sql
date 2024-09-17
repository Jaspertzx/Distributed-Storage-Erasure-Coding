CREATE TABLE [file] (
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    original_file_size int NOT NULL,
    shard_index INT NOT NULL,
    filesha256 CHAR(64) NOT NULL,
    byte_size INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (filename, shard_index),
    FOREIGN KEY (user_id) REFERENCES [user](id)
    );