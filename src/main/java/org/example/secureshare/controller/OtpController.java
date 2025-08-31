package org.example.secureshare.controller;

import org.example.secureshare.payload.MessageResponse;
import org.example.secureshare.payload.otpDTO.OtpRequest;
import org.example.secureshare.payload.otpDTO.OtpVerificationRequest;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.service.OtpService;
import org.example.secureshare.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        try {
            otpService.generateAndSendOtp(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("OTP sent successfully to your email."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send OTP."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request) {
        try {
            boolean isVerified = otpService.verifyOtp(request.getEmail(), request.getOtp());
            if (isVerified) {
                return ResponseEntity.ok(new MessageResponse("OTP verified successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Invalid or expired OTP."));
            }
        } catch (NoSuchElementException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }
}