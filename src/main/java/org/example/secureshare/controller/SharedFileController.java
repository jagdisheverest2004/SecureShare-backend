package org.example.secureshare.controller;

import org.example.secureshare.config.AppConstants;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.payload.sharedfileDTO.ShareFileRequest;
import org.example.secureshare.payload.sharedfileDTO.SharedFilesResponse;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.example.secureshare.service.SharedFileService;
import org.example.secureshare.service.OtpService;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/shared-files")
public class SharedFileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private SharedFileService sharedFileService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/share")
    public ResponseEntity<?> shareFile(@RequestBody ShareFileRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String senderUsername = authentication.getName();
            User sender = userRepository.findByUsername(senderUsername)
                    .orElseThrow(() -> new NoSuchElementException("Sender user not found: " + senderUsername));
            User recipient = userRepository.findByUsername(request.getRecipientUsername())
                    .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + request.getRecipientUsername()));

            Long sharedFileId = fileService.shareFile(request.getFileId(), senderUsername, request.getRecipientUsername());

            // Log the sharing transaction
            sharedFileService.logFileShare(
                    request.getFileId(),
                    sharedFileId,
                    sender.getUserId(),
                    recipient.getUserId(),
                    request.getIsSensitive()
            );

            auditLogService.logAction(sender,senderUsername, "FILE_SHARED", "File ID: " + request.getFileId() + " shared with " + request.getRecipientUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "File shared successfully!"));

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during file sharing: " + e.getMessage()));
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SharedFilesResponse sharedFiles = sharedFileService.getFilesSharedByMe(pageNumber,pageSize,sortBy,sortOrder,keyword,sensitive, username);
        auditLogService.logAction(authUtil.getLoggedInUser(),username, "FETCH_SHARED_FILES_BY_ME", "");
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SharedFilesResponse sharedFiles = sharedFileService.getFilesSharedToMe(pageNumber,pageSize,sortBy,sortOrder,keyword,sensitive, username);
        auditLogService.logAction(authUtil.getLoggedInUser(),username, "FETCH_SHARED_FILES_TO_ME", "");
        return ResponseEntity.ok(sharedFiles);
    }
}