package com.example.restservice.file;

import com.example.restservice.security.JwtTokenProvider;
import com.example.restservice.shardhandler.ShardHandler;
import com.example.restservice.shardretriever.ShardRetriever;
import com.example.restservice.sharduploader.ShardUploader;
import com.example.restservice.user.User;
import com.example.restservice.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShardUploader shardUploader;

    @Mock
    private ShardRetriever shardRetriever;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ShardHandler shardHandler;

    @InjectMocks
    private FileController fileController;

    public User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("user");
    }

    @Test
    void testUploadFile_Success() throws Exception {
        MultipartFile multipartFile = mock(MultipartFile.class);
        String authHeader = "Bearer valid.token";

        User mockUser = new User();
        mockUser.setId(1L);
        when(jwtTokenProvider.getUsernameFromJWT(anyString())).thenReturn("user");
        when(userRepository.findByUsername(anyString())).thenReturn(mockUser);
        when(multipartFile.getOriginalFilename()).thenReturn("testFile.txt");
        when(multipartFile.getBytes()).thenReturn("test content".getBytes());

        File mockFile = new File("testFile.txt");
        when(shardUploader.getAccessToken()).thenReturn("accessToken");
        doNothing().when(shardUploader).uploadFileToDrive(anyString(), any(File.class), anyInt());
        when(shardHandler.encodeFile(any())).thenReturn(new File[]{mockFile});
        ResponseEntity<?> responseEntity = fileController.uploadFile(multipartFile, authHeader);

        assertEquals(HttpStatusCode.valueOf(200), responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Test
    void testRetrieveFile_Success() throws Exception {
        String authHeader = "Bearer valid.token";
        String filename = "testFile.txt";

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(filename);
        fileInfo.setShardIndex(0);
        fileInfo.setByteSize(100);
        fileInfo.setOriginalFilename(filename);
        fileInfo.setOrginalFileSize(500);

        File inputFile = File.createTempFile("testfile", ".txt");
        Files.write(inputFile.toPath(), "abcdefabcdefabcdefabcdefabcdefabcdef".getBytes());

        List<FileInfo> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.add(fileInfo);
        }

        when(fileService.getFileShards(any(), any())).thenReturn(list);
        when(shardRetriever.downloadFilesFromDrive(anyList())).thenReturn(List.of("test content".getBytes()));
        when(jwtTokenProvider.getUsernameFromJWT(anyString())).thenReturn("user");
        when(userRepository.findByUsername(anyString())).thenReturn(mockUser);
        when(shardHandler.decodeFile(any(), any(), anyLong())).thenReturn(inputFile);

        ResponseEntity<?> responseEntity = fileController.retrieveFile(filename, authHeader);

        System.out.println("Response Status Code: " + responseEntity.getStatusCode());
        System.out.println("Response Body: " + responseEntity.getBody());

        assertEquals(HttpStatusCode.valueOf(200), responseEntity.getStatusCode());
        assertTrue(responseEntity.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).contains(filename));
        assertInstanceOf(ByteArrayResource.class, responseEntity.getBody());
    }
}
