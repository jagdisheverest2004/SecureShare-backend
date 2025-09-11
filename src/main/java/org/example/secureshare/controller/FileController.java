package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.payload.fiteDTO.DeleteFileRequest;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private AuditLogService auditLogService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("description") String description,
            @RequestParam("category") String category) {


        try {
            List<Long> fileIds = fileService.storeFiles(files, description, category);

            for(MultipartFile file : files) {
                auditLogService.logAction("FILE_UPLOAD", file.getOriginalFilename());
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

        try {
            // Retrieve both the file data and its metadata, including the original filename
            Map<String, Object> fileDownloadData = fileService.downloadFileAndGetMetadata(fileId);

            byte[] fileData = (byte[]) fileDownloadData.get("fileData");
            String originalFilename = (String) fileDownloadData.get("originalFilename");
            String contentType = (String) fileDownloadData.get("contentType");

            // Log the file download action
            auditLogService.logAction("FILE_DOWNLOAD", originalFilename);

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
        try {
            FetchFilesResponse files = fileService.getAllFilesForUser(keyword, pageNumber, pageSize, sortBy, sortOrder);
            auditLogService.logAction("FETCH_ALL_FILES", "");
            return ResponseEntity.ok(files);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve files."));
        }
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            @RequestBody DeleteFileRequest deleteFileRequest
    ) {
        try {
            fileService.deleteFile(fileId , deleteFileRequest.getDeletionType(),deleteFileRequest.getRecipientUsernames());
            auditLogService.logAction("FILE_DELETE", "File ID: " + fileId);
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

    @GetMapping("/download/encrypted/{fileId}")
    public ResponseEntity<?> downloadEncryptedFile(@PathVariable("fileId") Long fileId) {
        try {
            Map<String, Object> encryptedData = fileService.downloadEncryptedFileAndSendKeys(fileId);

            byte[] encryptedFile = (byte[]) encryptedData.get("encryptedFileData");
            String filename = (String) encryptedData.get("originalFilename");
            String contentType = (String) encryptedData.get("contentType");

            // Log the file download action
            auditLogService.logAction("ENCRYPTED_FILE_DOWNLOAD", filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(encryptedFile.length);

            return ResponseEntity.ok().headers(headers).body(encryptedFile);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }



}