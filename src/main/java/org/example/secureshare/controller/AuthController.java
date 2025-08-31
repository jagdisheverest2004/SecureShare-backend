package org.example.secureshare.controller;

import org.example.secureshare.model.AppRole;
import org.example.secureshare.model.Role;
import org.example.secureshare.model.User;
import org.example.secureshare.repository.RoleRepository;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.security.jwt.JwtUtils;
import org.example.secureshare.security.request.LoginRequest;
import org.example.secureshare.security.request.SignUpRequest;
import org.example.secureshare.security.request.VerifyOtpRequest;
import org.example.secureshare.security.response.MessageResponse;
import org.example.secureshare.security.response.UserInfoResponse;
import org.example.secureshare.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.example.secureshare.service.AuditLogService;
import org.example.secureshare.service.KeyService;
import org.example.secureshare.service.OtpService;
import org.example.secureshare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private KeyService keyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        try {
            KeyPair keyPair = keyService.generateRsaKeyPair();
            user.setPublicKey(keyService.encodePublicKey(keyPair.getPublic()));
            user.setPrivateKey(keyService.encodePrivateKey(keyPair.getPrivate()));
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: Key generation failed."));
        }

        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.setRole(userRole);
        userRepository.save(user);
        auditLogService.logAction(user,user.getUsername(), "USER_REGISTERED", "");
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            otpService.generateAndSendOtp(userDetails.getEmail());
            return ResponseEntity.ok(new MessageResponse("OTP sent to your registered email. Please verify to sign in."));
        } catch (AuthenticationException exception) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Bad credentials");
            body.put("status", false);
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtpAndLogin(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        String username = verifyOtpRequest.getUsername();
        String otp = verifyOtpRequest.getOtp();

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("User not found."));

            if (otpService.verifyOtp(user.getEmail(), otp)) {
                // Manually create an Authentication object for the security context
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(), user.getPassword(), Collections.singletonList((GrantedAuthority) () -> "ROLE_USER"));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserDetailsImpl userDetails = new UserDetailsImpl(user.getUserId(), user.getUsername(), user.getEmail(), user.getPassword(), Collections.singletonList((GrantedAuthority) () -> "ROLE_USER"));

                ResponseCookie jwtCookie = jwtUtils.generateTokenFromCookie(userDetails);
                List<String> roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

                UserInfoResponse userInfoResponse = new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        roles,
                        jwtCookie.toString()
                );

                auditLogService.logAction(user,username, "USER_SIGNED_IN" , "");
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(userInfoResponse);
            }
        } catch (NoSuchElementException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("An unexpected error occurred."));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {

        User user = authUtil.getLoggedInUser();

        ResponseCookie cookie = jwtUtils.generateNoTokenFromCookie();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User signed out successfully!");
        response.put("status", true);
        auditLogService.logAction(user, user.getUsername(), "USER_SIGN_OUT", "");
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }
}