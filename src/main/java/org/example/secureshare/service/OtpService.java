package org.example.secureshare.service;

import org.example.secureshare.model.Otp;
import org.example.secureshare.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpRepository OtpRepository;


    public void generateAndSendOtp(String email) {
        String OTPCode = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);

        Otp OTP = new Otp(email, OTPCode, expirationTime);
        OtpRepository.save(OTP);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP for Verification");
        message.setText("Hello,\n\nYour One-Time Password (OTP) is: " + OTPCode + "\n\nThis OTP is valid for 2 minutes.");
        mailSender.send(message);
    }

    public boolean verifyOtp(String email, String otp) {
        Otp storedOtp = OtpRepository.findById(email).orElse(null);
        if (storedOtp != null && storedOtp.getOtpCode().equals(otp) && storedOtp.getExpirationTime().isAfter(LocalDateTime.now())) {
            OtpRepository.delete(storedOtp); // Invalidate OTP after successful verification
            return true;
        }
        return false;
    }

}
