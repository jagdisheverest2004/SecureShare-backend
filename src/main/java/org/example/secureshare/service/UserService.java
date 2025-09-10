package org.example.secureshare.service;

import jakarta.transaction.Transactional;
import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.File;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.userutilsDTO.SettingsDTO;
import org.example.secureshare.repository.AuditLogRepository;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FileService fileService;

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

        List<File> userFiles = fileRepository.findByOwnerId(user.getUserId());
        List<Long> userFileIds = userFiles.stream().map(File::getId).toList();

        for(Long fileId : userFileIds) {
            fileService.deleteFile(fileId, "everyone",new ArrayList<String>()); // false indicates not to log this action again
        }

        List<AuditLog> auditLogList = auditLogRepository.findAuditLogsByUserId(user.getUserId());
        auditLogRepository.deleteAll(auditLogList);

        userRepository.delete(user);
    }

    public SettingsDTO getUserSettings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));


        SettingsDTO settings = new SettingsDTO();
        settings.setUsername(user.getUsername());
        settings.setEmail(user.getEmail());

        return settings;

    }
}