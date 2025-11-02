package org.example.secureshare.service;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import org.example.secureshare.model.AuditLog;
import org.example.secureshare.model.File;
import org.example.secureshare.model.User;
import org.example.secureshare.payload.userutilsDTO.SettingsDTO;
import org.example.secureshare.repository.AuditLogRepository;
import org.example.secureshare.repository.FileRepository;
import org.example.secureshare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FileService fileService;

    @Value("${spring.sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String senderEmail;

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

        Email from = new Email(senderEmail);
        String subject = "Username Retrieval Request";
        Email to = new Email(email);
        String textContent = "Hello,\n\nYour username is: " + user.getUsername() + "\n\nIf you did not request this, please ignore this email.";
        Content content = new Content("text/plain", textContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                // Log the error but don't crash the user's request
                logger.error("Failed to send username email: {}", response.getBody());
            }
        } catch (IOException ex) {
            // Log the error
            logger.error("Error sending username email: {}", ex.getMessage());
        }
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        List<File> userFiles = fileRepository.findByOwnerId(user.getUserId());
        List<Long> userFileIds = userFiles.stream().map(File::getId).toList();

        for(Long fileId : userFileIds) {
            if(fileRepository.existbyOriginalFileIdAndOwnerId(fileId,user.getUserId())) {
                fileService.deleteFile(fileId, "everyone", new ArrayList<String>());
            }
            else{
                fileService.deleteFile(fileId,"me", new ArrayList<String>());
            }
        }

        List<AuditLog> auditLogList = auditLogRepository.findAuditLogsByUserId(user.getUserId());
        auditLogRepository.deleteAll(auditLogList);

        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public SettingsDTO getUserSettings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));


        SettingsDTO settings = new SettingsDTO();
        settings.setUsername(user.getUsername());
        settings.setEmail(user.getEmail());

        return settings;

    }
}