package org.example.secureshare.service;

import jakarta.transaction.Transactional;
import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.File;
import org.example.secureshare.model.SharedFile;
import org.example.secureshare.model.User;
import org.example.secureshare.repository.AuditLogRepository;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.SharedFileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SharedFileRepository sharedFileRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));

        otpService.generateAndSendOtp(user.getEmail());
    }

    public void resetPassword(String email, String otp, String newPassword) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    public void findUsernameByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Username Retrieval Request");
        message.setText("Hello,\n\nYour username is: " + user.getUsername() + "\n\nIf you did not request this, please ignore this email.");
        mailSender.send(message);
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // 1. Delete all files the user owns (uploaded files)
        List<File> userFiles = fileRepository.findByOwner_UserId(user.getUserId());
        fileRepository.deleteAll(userFiles);

        // 2. Delete all shared file logs where the user is the sender
        List<SharedFile> sentSharedFiles = sharedFileRepository.findBySender_UserId(user.getUserId());
        sharedFileRepository.deleteAll(sentSharedFiles);

        // 3. Delete all shared file logs where the user is the recipient
        List<SharedFile> receivedSharedFiles = sharedFileRepository.findByRecipient_UserId(user.getUserId());
        // Get the IDs of the files received by the user
        List<Long> receivedFileIds = receivedSharedFiles.stream()
                .map(sharedFile -> sharedFile.getFile().getId())
                .toList();
        // Delete the shared file logs first to prevent integrity violations
        sharedFileRepository.deleteAll(receivedSharedFiles);
        // Delete the actual file records that were received by the user
        fileRepository.deleteAllById(receivedFileIds);

        // 4. Delete all audit logs of the user
        List<AuditLog> auditLogList = auditLogRepository.findByUser_UserId(user.getUserId());
        auditLogRepository.deleteAll(auditLogList);

        // 5. Finally, delete the user record itself
        userRepository.delete(user);
    }
}