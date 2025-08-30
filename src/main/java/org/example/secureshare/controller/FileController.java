package org.example.secureshare.controller;

import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.payload.fiteDTO.UploadFileResponse;
import org.example.secureshare.payload.sharedfileDTO.ShareFileRequest;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.example.secureshare.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("filename") String fileName,
            @RequestParam("description") String description,
            @RequestParam("category") String category) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Long fileId = fileService.storeFile(file, fileName, description, category, username);
            UploadFileResponse response = new UploadFileResponse(fileId, fileName, "File uploaded successfully!");
            auditLogService.logAction(username, "FILE_UPLOAD", fileName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchFile(@RequestParam("filename") String filename) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            FetchFileResponse response = fileService.getFileByFilename(filename, username);
            auditLogService.logAction(username, "FILE_FETCH", filename);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/fetch-all")
    public ResponseEntity<?> fetchAllFilesForUser(
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            List<FetchFileResponse> files = fileService.getAllFilesForUser(keyword,username);
            auditLogService.logAction(username, "FETCH_ALL_FILES", "");
            return ResponseEntity.ok(files);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve files."));
        }
    }

}