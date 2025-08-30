package org.example.secureshare.controller;

import org.example.secureshare.payload.userutilsDTO.FindUserNameRequest;
import org.example.secureshare.payload.userutilsDTO.ForgotPasswordRequest;
import org.example.secureshare.payload.MessageResponse;
import org.example.secureshare.payload.userutilsDTO.ResetPasswordRequest;
import org.example.secureshare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/user-utils")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${spring.secure.app.jwtCookieName}")
    private String jwtCookie;

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
    public ResponseEntity<?> findUsername(@RequestBody FindUserNameRequest request) {
        try {
            userService.findUsernameByEmail(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Your username has been sent to your registered email."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            userService.deleteAccount(username);

            // Invalidate the JWT cookie
            ResponseCookie noCookie = ResponseCookie.from(jwtCookie, "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0) // Immediately expire
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, noCookie.toString())
                    .body(Map.of("message", "Account deleted successfully!"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete account."));
        }
    }

}
