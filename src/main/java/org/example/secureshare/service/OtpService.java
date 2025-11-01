package org.example.secureshare.service;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import org.example.secureshare.model.Otp;
import org.example.secureshare.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.NoSuchElementException;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String senderEmail;

    @Value("${spring.sendgrid.api-key}")
    private String sendGridApiKey;

    @Autowired
    private OtpRepository otpRepository;

    @Transactional
    public void generateAndSendOtp(String email) {
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);

        Otp otp = new Otp(email, otpCode, expirationTime);
        otpRepository.save(otp);

        Email from = new Email(senderEmail);
        String subject = "Your OTP for Verification";
        Email to = new Email(email);
        String textContent = "Hello,\n\nYour One-Time Password (OTP) is: " + otpCode + "\n\nThis OTP is valid for 2 minutes.";
        Content content = new Content("text/plain", textContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            // You can log the response for debugging
            logger.debug("SendGrid Response Code: {}", response.getStatusCode());

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                // Throw an exception if SendGrid failed
                throw new RuntimeException("Failed to send email via SendGrid: " + response.getBody());
            }

        } catch (IOException ex) {
            // This catches network errors
            throw new RuntimeException("Error sending email: " + ex.getMessage());
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Otp storedOtp = otpRepository.findById(email).orElse(null);

        if (storedOtp == null) {
            throw new NoSuchElementException("No OTP found for this email.");
        }

        if (storedOtp.getExpirationTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(storedOtp);
            throw new IllegalArgumentException("OTP has expired.");
        }

        if (!storedOtp.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP.");
        }

        otpRepository.delete(storedOtp);
        return true;
    }
}