package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.model.File;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.fiteDTO.FetchFilesResponse;
import org.example.secureshare.payload.sharedfileDTO.FetchUsersResponse;
import org.example.secureshare.payload.sharedfileDTO.ShareFileRequest;
import org.example.secureshare.payload.sharedfileDTO.SharedFilesResponse;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.example.secureshare.service.SharedFileService;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth/shared-files")
public class SharedFileController {

    private static final Logger logger = LoggerFactory.getLogger(SharedFileController.class); // <-- ADD THIS

    @Autowired
    private FileService fileService;

    @Autowired
    private SharedFileService sharedFileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AuditLogService auditLogService;


    @PostMapping("/share")
    public ResponseEntity<?> shareFile(@RequestBody ShareFileRequest request) {
        try {
            // The service now handles validation, saving, and logging in one transaction.
            Long sharedFileId = fileService.shareFile(
                    request.getFileId(),
                    request.getRecipientUsername(),
                    request.getIsSensitive()
            );
            // --- END OF SIMPLIFIED CALL ---

            // The logFileShare call is no longer needed here, it's in FileService

            auditLogService.logAction("FILE_SHARED", "File ID: " + request.getFileId() + " shared with " + request.getRecipientUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "File shared successfully!", "newFileId", sharedFileId));

        } catch (NoSuchElementException e) {
            logger.warn("Share failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Share failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) { // <-- Catches "Recipient already has access"
            logger.warn("Share failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("An unexpected error occurred during file sharing controller logic", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/by-me")
    public ResponseEntity<SharedFilesResponse> getFilesSharedByMe(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sensitive", required = false) String sensitive,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.SHARED_FILES_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_SHARED_FILES_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_SHARED_FILES_DIR,required = false) String sortOrder
    ) {
        SharedFilesResponse sharedFiles = sharedFileService.getFilesSharedByMe(pageNumber,pageSize,sortBy,sortOrder,keyword,sensitive);
        auditLogService.logAction("FETCH_SHARED_FILES_BY_ME", "");
        return ResponseEntity.ok(sharedFiles);
    }

    @GetMapping("/to-me")
    public ResponseEntity<SharedFilesResponse> getFilesSharedToMe(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sensitive", required = false) String sensitive,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.SHARED_FILES_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_SHARED_FILES_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_SHARED_FILES_DIR,required = false) String sortOrder
    ) {
        SharedFilesResponse sharedFiles = sharedFileService.getFilesSharedToMe(pageNumber,pageSize,sortBy,sortOrder,keyword,sensitive);
        auditLogService.logAction("FETCH_SHARED_FILES_TO_ME", "");
        return ResponseEntity.ok(sharedFiles);
    }

    @GetMapping("fetch-shared/{fileId}")
    public ResponseEntity<?> fetchUsersFileIsSharedWith(
            @PathVariable Long fileId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConstants.USERNAME_PAGE_SIZE,required = false)  Integer pageSize,
            @RequestParam(name = "sortBy" , defaultValue = AppConstants.SORT_USERNAMES_BY,required = false) String sortBy,
            @RequestParam(name = "sortOrder" , defaultValue = AppConstants.SORT_USERNAMES_DIR,required = false) String sortOrder) {
        try {
            FetchUsersResponse sharedUsers = sharedFileService.getUsersFileIsSharedWith(fileId,keyword, pageNumber, pageSize, sortBy, sortOrder);
            auditLogService.logAction("FETCH_SHARED_USERS", "File ID: " + fileId);
            return ResponseEntity.ok(sharedUsers);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve shared users."));
        }
    }

}