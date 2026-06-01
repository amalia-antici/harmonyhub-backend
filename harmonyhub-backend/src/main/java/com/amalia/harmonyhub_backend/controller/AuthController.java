package com.amalia.harmonyhub_backend.controller;

import com.amalia.harmonyhub_backend.config.JwtUtils;
import com.amalia.harmonyhub_backend.dtos.LoginRequest;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.model.Role;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import com.amalia.harmonyhub_backend.repository.RoleRepository;
import com.amalia.harmonyhub_backend.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @Autowired private EmailService emailService;

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
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "roles", user.getRoles().stream().map(Role::getName).toList(),
                    "id", user.getId()
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/login/step1")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String emailOtp = String.format("%06d", new Random().nextInt(999999));
        user.setEmailOtpCode(emailOtp);
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), emailOtp);
        // In authenticateUser method, after saving the OTP:
        System.out.println(">>> DEMO OTP for " + user.getUsername() + ": " + emailOtp);

        String preAuthToken = jwtUtils.generatePartialToken(user.getUsername(), "AWAITING_MFA");
        return ResponseEntity.ok(Map.of(
                "token", preAuthToken,
                "nextStep", "MFA_CODE_PROMPT"
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
        return ResponseEntity.ok(Map.of(
                "token", finalToken,
                "username", user.getUsername(),
                "roles", user.getRoles().stream().map(Role::getName).toList(),
                "id", user.getId()
        ));
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


}