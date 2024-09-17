# Distributed Cloud Storage Implementation with Reed-Solomon's Erasure Coding Algorithm
![diagram1.png](resources%2Fdiagram1.png)

Passion Project, Reed-Solomon Erasure coding implementation using Google Drive in Java via RestAPIs.

This project implements file sharding (encoding) and reconstruction (decoding) using the Reed-Solomon erasure code 
algorithm. Reed-Solomon encoding generates parity shards, which allows for the reconstruction of the original file even 
if some shards are missing. This is particularly useful for distributed storage systems or in environments where data 
redundancy and integrity are critical and where you would like to optimize storage overhead as much as possible.

## How does it work?
Reed-Solomon Erasure Coding is a method of data protection that uses a specific type of error-correcting code. It was 
originally developed to correct errors in data transmission over noisy communication channels. The key idea is to split 
data into several data shards and generate additional parity shards using mathematical operations. Even if a few data or
parity shards are lost or corrupted, the algorithm can reconstruct the original data using the remaining shards.

By distributing the 6 shards across 6 separate drives or servers, you can tolerate up to 2 disk or server failures and 
still successfully recover the original file. This level of fault tolerance is achieved while only using 150% of the 
original file size for storage, offering a highly efficient balance between redundancy and storage overhead.

## How does it _work_?
### Encoding:  
The Reed-Solomon algorithm works by:
1. Calculate the encoding matrix 
   1. First, find the Vandermonde Matrix (matrix[r][c] = pow(r, c)) of size 6, 4
   2. Second, get the inverse upper part of matrix of size 4 x 4
   3. Third, multiply the Vandermonde matrix with the inner upper part to get a encoding matrix of size 6, 4
```
Vandermonde     Inverse upper   Encoding
matrix          matrix          matrix

01 00 00 00                     01 00 00 00
01 01 01 01     01 00 00 00     00 01 00 00
01 02 04 08  x  7b 01 8e f4  =  00 00 01 00
01 03 05 0f     00 7a f4 8e     00 00 00 01
01 04 10 40     7a 7a 7a 7a     1b 1c 12 14
01 05 11 55                     1c 1b 14 12
```
2. Form the data into a 2D matrix with size 4, n (n can be any size)
3. Multiply the encoding matrix with the data matrix to form a 2D matrix with size 6, n.
4. Store each row into a separate drive/server.

### Decoding:
1. Retrieve the rows of data from each drive/server.
2. For each corresponding row of maximum 4 rows, take the respective rows from the encoding matrix.
   1. For e.g. row 0 and 5 are lost, take rows 1, 2, 3, 4 from the encoding matrix to decode the remaining shards of 
   data.
2. Invert the encoding sub matrix.
3. Multiply the encoding sub matrix with the recovered data to get back the original restored data.

## Why did I make this?

This project started as a passion project to explore and benchmark the performance and storage efficiency of
Reed-Solomon erasure coding compared to traditional RAID1 systems. From my experience, I’ve observed many companies
using a leader-follower database backup system for storing confidential files, which led me to ask, "What other
alternatives are there for this storage setup?" This curiosity drove me to create a modular, REST API-based solution
that allows for easy redundancy in file storage. The modularity comes from the use of interfaces like ShardUploader and
ShardRetriever, which means I can easily swap out or customize storage solutions in future projects, making this
implementation flexible and scalable.

## Project Structure

This project is designed to be highly modular and extensible through the use of interfaces and dependency abstraction. 
The main components that handle file sharding, uploading, and retrieval are abstracted into interfaces, making it easy 
to extend or replace any part of the project without modifying the core logic. If you want to integrate your own 
database or cloud storage provider, you can simply create a new implementation of the respective interface and inject it
into the project.

### Key Components
- **ShardHandler**:  
  Handles the sharding (encoding) and reconstruction (decoding) of files. By implementing this interface, you can change
how files are split into data and parity shards or how they are reconstructed from shards.
    - Methods:
        - `encodeFile(File inputFile)`: Encodes a file into data and parity shards.
        - `decodeFile(List<byte[]> shardFiles, String filename, long originalFileLength)`: Reconstructs the original 
      file from its shards.

- **ShardUploader**:  
  Manages uploading file shards to a remote storage service (e.g., Google Drive, AWS S3). This allows the project to 
easily switch between different storage systems by implementing this interface with the desired storage logic.
    - Methods:
        - `getAccessToken()`: Retrieves an OAuth2 access token to authenticate with the storage service.
        - `uploadFileToDrive(String accessToken, File file, int index)`: Uploads the file shard to the storage system.

- **ShardRetriever**:  
  Responsible for retrieving file shards from the remote storage service. Like `ShardUploader`, you can swap in 
different storage services by implementing this interface for other storage solutions.
    - Methods:
        - `downloadFilesFromDrive(List<FileInfo> fileInfos)`: Downloads file shards from the storage service based on 
      metadata.


### Prerequisites

1. Install Java 11 or higher.
2. Set up a Spring Boot environment with necessary dependencies (Maven/Gradle).
3. Ensure you have a database (e.g., MySQL, PostgreSQL) and a storage solution (e.g., AWS S3, Google Drive) for storing 
file shards.

## Technologies Used

- **Java 11**
- **Spring Boot**
- **Reed-Solomon Erasure Code** (via [Backblaze library](https://github.com/Backblaze/JavaReedSolomon) with some minor 
edits)
- **JWT Authentication** for secure file uploads and retrievals