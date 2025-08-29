package org.example.secureshare.controller;

import org.example.secureshare.payload.OtpRequest;
import org.example.secureshare.payload.OtpVerificationRequest;
import org.example.secureshare.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(@RequestBody OtpRequest request) {
        try {
            otpService.generateAndSendOtp(request.getEmail());
            return ResponseEntity.ok("OTP sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending OTP: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpVerificationRequest request) {
        if (otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            return ResponseEntity.ok("OTP verified successfully!");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired OTP.");
        }
    }
}