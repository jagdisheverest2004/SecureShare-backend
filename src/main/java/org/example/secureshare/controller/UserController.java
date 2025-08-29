package org.example.secureshare.controller;

import org.example.secureshare.payload.ForgotPasswordRequest;
import org.example.secureshare.payload.MessageResponse;
import org.example.secureshare.payload.ResetPasswordRequest;
import org.example.secureshare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/user-utils")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePasswordReset(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Password reset OTP sent successfully to your email."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/find-username")
    public ResponseEntity<?> findUsername(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.findUsernameByEmail(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Your username has been sent to your registered email."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }
}
