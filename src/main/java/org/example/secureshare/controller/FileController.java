package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.DeleteFileRequest;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.example.secureshare.service.OtpService;
import org.example.secureshare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/auth/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("description") String description,
            @RequestParam("category") String category) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            User loggedInUser = authUtil.getLoggedInUser();
            List<Long> fileIds = fileService.storeFiles(files, description, category, username);

            for(MultipartFile file : files) {
                // Pass the User object instead of the username
                auditLogService.logAction(loggedInUser, "FILE_UPLOAD", file.getOriginalFilename());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", files.length + " files uploaded successfully!", "fileIds", fileIds));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload files. " + e.getMessage()));
        }
    }

    // Inside the downloadFileById method

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFileById(@PathVariable("fileId") Long fileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            // Retrieve both the file data and its metadata, including the original filename
            Map<String, Object> fileDownloadData = fileService.downloadFileAndGetMetadata(fileId, username);

            byte[] fileData = (byte[]) fileDownloadData.get("fileData");
            String originalFilename = (String) fileDownloadData.get("originalFilename");
            String contentType = (String) fileDownloadData.get("contentType");

            // Log the file download action
            auditLogService.logAction(authUtil.getLoggedInUser(), "FILE_DOWNLOAD", originalFilename);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            // Set the content type based on the stored value
            headers.setContentType(MediaType.parseMediaType(contentType));
            // Use the originalFilename for the Content-Disposition header
            headers.setContentDispositionFormData("attachment", originalFilename);
            headers.setContentLength(fileData.length);

            return ResponseEntity.ok().headers(headers).body(fileData);
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
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.FILE_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_FILES_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_FILES_DIR,required = false) String sortOrder
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            FetchFilesResponse files = fileService.getAllFilesForUser(keyword, username, pageNumber, pageSize, sortBy, sortOrder);
            auditLogService.logAction(authUtil.getLoggedInUser(), "FETCH_ALL_FILES", "");
            return ResponseEntity.ok(files);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve files."));
        }
    }

    // Change to POST with a request body
    @PostMapping("/delete/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            @RequestBody DeleteFileRequest deleteFileRequest
    ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            System.out.println("Received delete request for file ID: " + fileId + " with deletion type: " + deleteFileRequest.getDeletionType());
            fileService.deleteFile(fileId, username , deleteFileRequest.getDeletionType(),deleteFileRequest.getRecipientUsernames());
            auditLogService.logAction(authUtil.getLoggedInUser(), "FILE_DELETE", "File ID: " + fileId);
            return ResponseEntity.ok(Map.of("message", "File deletion processed successfully."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during file deletion: " + e.getMessage()));
        }
    }

}