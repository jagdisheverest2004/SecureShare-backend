package org.example.secureshare.controller;

import org.example.secureshare.model.User;
import org.example.secureshare.payload.MessageResponse;
import org.example.secureshare.payload.sharedfileDTO.SharedFileResponse;
import org.example.secureshare.payload.sharedfileDTO.ShareFileRequest;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.FileService;
import org.example.secureshare.service.SharedFileService;
import org.example.secureshare.service.OtpService;
import org.example.secureshare.repository.UserRepository;
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
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/share")
    public ResponseEntity<?> shareFile(@RequestBody ShareFileRequest request, @RequestParam(value = "otp", required = false) String otp) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String senderUsername = authentication.getName();
            User sender = userRepository.findByUsername(senderUsername).orElseThrow(() -> new NoSuchElementException("Sender user not found: " + senderUsername));

            // OTP verification for sensitive files
            if (request.isSensitive()) {

                String senderEmail = sender.getEmail();
                if (otp == null || !otpService.verifyOtp(senderEmail, otp)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid or missing OTP for sensitive file share."));
                }
            }

            Long sharedFileId = fileService.shareFile(request.getFileId(), senderUsername, request.getRecipientUsername(), request.isSensitive());

            // Log the sharing transaction
            sharedFileService.logFileShare(
                    sharedFileId,
                    userRepository.findByUsername(senderUsername).get().getUserId(),
                    userRepository.findByUsername(request.getRecipientUsername()).get().getUserId(),
                    request.isSensitive()
            );

            auditLogService.logAction(senderUsername, "FILE_SHARED", "File ID: " + request.getFileId() + " shared with " + request.getRecipientUsername());
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
    public ResponseEntity<List<SharedFileResponse>> getFilesSharedByMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<SharedFileResponse> sharedFiles = sharedFileService.getFilesSharedByMe(username);
        auditLogService.logAction(username, "FETCH_SHARED_FILES_BY_ME", "");
        return ResponseEntity.ok(sharedFiles);
    }

    @GetMapping("/to-me")
    public ResponseEntity<List<SharedFileResponse>> getFilesSharedToMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<SharedFileResponse> sharedFiles = sharedFileService.getFilesSharedToMe(username);
        auditLogService.logAction(username, "FETCH_SHARED_FILES_TO_ME", "");
        return ResponseEntity.ok(sharedFiles);
    }
}