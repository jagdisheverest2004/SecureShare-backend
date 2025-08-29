package org.example.secureshare.controller;

import org.example.secureshare.model.AppRole;
import org.example.secureshare.model.Role;
import org.example.secureshare.model.User;
import org.example.secureshare.repository.RoleRepository;
import org.example.secureshare.repository.UserRepository;
import org.example.secureshare.security.jwt.JwtUtils;
import org.example.secureshare.security.request.LoginRequest;
import org.example.secureshare.security.request.SignUpRequest;
import org.example.secureshare.security.response.MessageResponse;
import org.example.secureshare.security.response.UserInfoResponse;
import org.example.secureshare.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
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

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try{
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword()
                            )
                    );

        }
        catch(AuthenticationException exception){
            Map<String,Object> body = new HashMap<>();
            body.put("message", "Bad credentials");
            body.put("status",false);

            return new ResponseEntity<Object>(body, HttpStatus.UNAUTHORIZED);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateTokenFromCookie(userDetails);
        List<String> roles = (List<String>) userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();
        UserInfoResponse userInfoResponse = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles,
                jwtCookie.toString()
        );
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString()).body(userInfoResponse);
    }

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

        // Create a new user's account
        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.setRole(userRole);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }


    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = (List<String>) userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UserInfoResponse userInfoResponse = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                roles
        );
        return ResponseEntity.ok(userInfoResponse);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = jwtUtils.generateNoTokenFromCookie();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User signed out successfully!");
        response.put("status", true);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }

}
