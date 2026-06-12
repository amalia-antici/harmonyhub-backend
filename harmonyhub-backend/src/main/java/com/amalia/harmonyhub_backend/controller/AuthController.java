package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.config.JwtUtils;
import com.amalia.harmonyhub_backend.dtos.LoginRequest;
import com.amalia.harmonyhub_backend.dtos.ProfileUpdateRequest;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.model.Role;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import com.amalia.harmonyhub_backend.repository.RoleRepository;
import com.amalia.harmonyhub_backend.services.CloudinaryService;
import com.amalia.harmonyhub_backend.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @Autowired private EmailService emailService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        if (userRepository.findByUsername(newUser.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        Role userRole = roleRepository.findByName("ROLE_USER");
        newUser.setRoles(Collections.singletonList(userRole));

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        if (newUser.getSecurityAnswer() != null) {
            String normalizedAnswer = newUser.getSecurityAnswer().toLowerCase().trim();
            newUser.setSecurityAnswer(passwordEncoder.encode(normalizedAnswer));
        } else {
            return ResponseEntity.badRequest().body("A security answer is mandatory for registration");
        }

        // Upload profile photo to Cloudinary if provided
        if (newUser.getPhoto() != null && newUser.getPhoto().startsWith("data:image")) {
            String cloudUrl = cloudinaryService.uploadBase64Image(newUser.getPhoto());
            if (cloudUrl != null) {
                newUser.setPhoto(cloudUrl);
            }
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        User user = userRepository.findByUsername(username);

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            String token=jwtUtils.generateToken(user.getUsername());
            return ResponseEntity.ok(buildUserResponse(user, token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/login/step1")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String partialToken = jwtUtils.generatePartialToken(user.getUsername(), "AWAITING_QA");
        return ResponseEntity.ok(Map.of(
                "token", partialToken,
                "nextStep", "SECURITY_QUESTION_PROMPT",
                "question", user.getSecurityQuestion()
        ));
    }

    @PostMapping("/login/step2")
    public ResponseEntity<?> verifyEmailOtp(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody Map<String, String> body) {

        String token = tokenHeader.substring(7);
        String username = jwtUtils.getUsernameFromToken(token);
        String currentStep = jwtUtils.getStepFromToken(token);

        if (!"AWAITING_MFA".equals(currentStep)) {
            return ResponseEntity.status(403).body("Invalid flow state");
        }

        User user = userRepository.findByUsername(username);
        String submittedCode = body.get("emailOtp");

        if (user.getEmailOtpCode() == null || !user.getEmailOtpCode().equals(submittedCode)) {
            return ResponseEntity.status(401).body("Invalid email OTP code");
        }

        user.setEmailOtpCode(null);
        userRepository.save(user);

        String advancedToken = jwtUtils.generatePartialToken(username, "AWAITING_QA");
        return ResponseEntity.ok(Map.of(
                "token", advancedToken,
                "nextStep", "SECURITY_QUESTION_PROMPT",
                "question", user.getSecurityQuestion()
        ));
    }

    @PostMapping("/login/step3")
    public ResponseEntity<?> verifySecurityAnswer(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody Map<String, String> body) {

        String token = tokenHeader.substring(7);
        String username = jwtUtils.getUsernameFromToken(token);
        String currentStep = jwtUtils.getStepFromToken(token);

        if (!"AWAITING_QA".equals(currentStep)) {
            return ResponseEntity.status(403).body("Invalid flow state");
        }

        User user = userRepository.findByUsername(username);
        String answerInput = body.get("securityAnswer");

        if (answerInput == null || !passwordEncoder.matches(
                answerInput.toLowerCase().trim(), user.getSecurityAnswer())) {
            return ResponseEntity.status(401).body("Security answer mismatch");
        }

        String finalToken = jwtUtils.generateToken(username);
        return ResponseEntity.ok(buildUserResponse(user, finalToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
        }

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String resetLink = "https://10.212.192.97:5173/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        User user = userRepository.findByPasswordResetToken(token);

        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid or expired reset token.");
        }

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Reset token has expired.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully.");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userRepository.findByUsername(currentUsername);
        if (user == null) {
            return ResponseEntity.status(404).body("Error: Current session account user record not found.");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setCountry(request.getCountry());
        user.setCity(request.getCity());
        user.setOccupation(request.getOccupation());
        user.setInstagram(request.getInstagram());
        user.setSkills(request.getSkills());
        user.setBio(request.getBio());

        // Upload to Cloudinary if it's a new base64 image, otherwise keep existing URL
        if (request.getPhoto() != null && request.getPhoto().startsWith("data:image")) {
            String cloudUrl = cloudinaryService.uploadBase64Image(request.getPhoto());
            user.setPhoto(cloudUrl != null ? cloudUrl : user.getPhoto());
        } else if (request.getPhoto() != null && !request.getPhoto().isBlank()) {
            user.setPhoto(request.getPhoto()); // already a Cloudinary URL, keep it
        }
        // if photo is null/blank, leave the existing photo untouched

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    // Helper to avoid repeating this pattern everywhere
    private Map<String, Object> buildUserResponse(User user, String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles().stream().map(Role::getName).toList());
        response.put("id", user.getId());
        response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        response.put("email", user.getEmail() != null ? user.getEmail() : "");
        response.put("country", user.getCountry() != null ? user.getCountry() : "");
        response.put("city", user.getCity() != null ? user.getCity() : "");
        response.put("occupation", user.getOccupation() != null ? user.getOccupation() : "");
        response.put("instagram", user.getInstagram() != null ? user.getInstagram() : "");
        response.put("skills", user.getSkills() != null ? user.getSkills() : "");
        response.put("bio", user.getBio() != null ? user.getBio() : "");
        response.put("photo", user.getPhoto() != null ? user.getPhoto() : "");
        return response;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) return ResponseEntity.status(404).body("User not found");
        return ResponseEntity.ok(user);
    }

}