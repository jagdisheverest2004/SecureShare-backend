package org.example.secureshare.service;

import org.example.secureshare.model.Otp;
import org.example.secureshare.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.NoSuchElementException;

@Service
public class OtpService {

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpRepository otpRepository;

    @Transactional
    public void generateAndSendOtp(String email) {
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);

        Otp otp = new Otp(email, otpCode, expirationTime);
        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("Your OTP for Verification");
        message.setText("Hello,\n\nYour One-Time Password (OTP) is: " + otpCode + "\n\nThis OTP is valid for 2 minutes.");
        mailSender.send(message);
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