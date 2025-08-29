package org.example.secureshare.controller;

import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFileResponse;
import org.example.secureshare.payload.fiteDTO.UploadFileResponse;
import org.example.secureshare.payload.sharedfileDTO.ShareFileRequest;
import org.example.secureshare.repository.UserRepository;
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
    public ResponseEntity<?> fetchAllFilesForUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            List<FetchFileResponse> files = fileService.getAllFilesForUser(username);
            return ResponseEntity.ok(files);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve files."));
        }
    }

    @PostMapping("/share/initiate")
    public ResponseEntity<?> initiateFileShare(@RequestBody ShareFileRequest request) {
        if (request.isSensitive()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String senderUsername = authentication.getName();

            User sender = userRepository.findByUsername(senderUsername).orElseThrow(() -> new NoSuchElementException("Sender not found: " + senderUsername));

            String senderEmail = sender.getEmail();

            otpService.generateAndSendOtp(senderEmail);
            return ResponseEntity.ok(Map.of("message", "Sensitive file share initiated. OTP sent to your email."));
        } else {
            // For non-sensitive files, proceed to the final share endpoint immediately
            return ResponseEntity.ok(Map.of("message", "File sharing initiated. No OTP required."));
        }
    }
}