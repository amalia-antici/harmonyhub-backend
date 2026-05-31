package com.amalia.harmonyhub_backend.login_register;

import com.amalia.harmonyhub_backend.config.JwtUtils;
import com.amalia.harmonyhub_backend.controller.AuthController;
import com.amalia.harmonyhub_backend.dtos.LoginRequest;
import com.amalia.harmonyhub_backend.model.Role;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.RoleRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import com.amalia.harmonyhub_backend.services.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    UserRepository userRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private RoleRepository roleRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private PasswordEncoder passwordEncoder;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private JwtUtils jwtUtils;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private EmailService emailService;

    private User sampleUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        sampleUser = new User();
        sampleUser.setId(100L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setSecurityQuestion("What was the name of your first pet?");
        sampleUser.setSecurityAnswer("encodedAnswer");
        sampleUser.setRoles(Collections.singletonList(userRole));
    }


    @Test
    void register_Success() throws Exception {
        User incoming = new User();
        incoming.setUsername("newuser");
        incoming.setEmail("new@example.com");
        incoming.setPassword("rawPassword");
        incoming.setSecurityQuestion("What city were you born in?");
        incoming.setSecurityAnswer("cluj");

        Mockito.when(userRepository.findByUsername("newuser")).thenReturn(null);
        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(userRole);
        Mockito.when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        Mockito.when(passwordEncoder.encode("cluj")).thenReturn("encodedAnswer");
        Mockito.when(userRepository.save(any(User.class))).thenReturn(incoming);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incoming)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void register_Failure_UsernameAlreadyTaken() throws Exception {
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already taken"));
    }

    @Test
    void register_Failure_MissingSecurityAnswer() throws Exception {
        User noAnswer = new User();
        noAnswer.setUsername("newuser");
        noAnswer.setPassword("pass");
        noAnswer.setSecurityQuestion("What city were you born in?");

        Mockito.when(userRepository.findByUsername("newuser")).thenReturn(null);
        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(userRole);
        Mockito.when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noAnswer)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("A security answer is mandatory for registration"));
    }



    @Test
    void login_Success() throws Exception {
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        Mockito.when(jwtUtils.generateToken("testuser")).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "testuser",
                                "password", "rawPassword"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void login_Failure_UserNotFound() throws Exception {
        Mockito.when(userRepository.findByUsername("unknown")).thenReturn(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "unknown",
                                "password", "anyPassword"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void login_Failure_WrongPassword() throws Exception {
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "testuser",
                                "password", "wrongPassword"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void loginStep1_Success_SendsOtpAndReturnsPartialToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("rawPassword");

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        Mockito.when(jwtUtils.generatePartialToken("testuser", "AWAITING_MFA")).thenReturn("partial-token-step1");
        Mockito.doNothing().when(emailService).sendOtpEmail(anyString(), anyString());

        mockMvc.perform(post("/api/auth/login/step1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("partial-token-step1"))
                .andExpect(jsonPath("$.nextStep").value("MFA_CODE_PROMPT"));

        // Verify OTP email was triggered
        Mockito.verify(emailService, Mockito.times(1)).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void loginStep1_Failure_InvalidCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongPassword");

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login/step1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        Mockito.verify(emailService, Mockito.never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void loginStep1_Failure_UserDoesNotExist() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("pass");

        Mockito.when(userRepository.findByUsername("ghost")).thenReturn(null);

        mockMvc.perform(post("/api/auth/login/step1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void loginStep2_Success_ValidOtp() throws Exception {
        sampleUser.setEmailOtpCode("123456");

        Mockito.when(jwtUtils.getUsernameFromToken("partial-token")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token")).thenReturn("AWAITING_MFA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        Mockito.when(jwtUtils.generatePartialToken("testuser", "AWAITING_QA")).thenReturn("partial-token-step2");

        mockMvc.perform(post("/api/auth/login/step2")
                        .header("Authorization", "Bearer partial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emailOtp", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("partial-token-step2"))
                .andExpect(jsonPath("$.nextStep").value("SECURITY_QUESTION_PROMPT"))
                .andExpect(jsonPath("$.question").value("What was the name of your first pet?"));
    }

    @Test
    void loginStep2_Failure_WrongOtp() throws Exception {
        sampleUser.setEmailOtpCode("123456");

        Mockito.when(jwtUtils.getUsernameFromToken("partial-token")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token")).thenReturn("AWAITING_MFA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/login/step2")
                        .header("Authorization", "Bearer partial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emailOtp", "999999"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email OTP code"));
    }

    @Test
    void loginStep2_Failure_WrongFlowState() throws Exception {
        Mockito.when(jwtUtils.getUsernameFromToken("partial-token")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token")).thenReturn("AWAITING_QA"); // wrong step

        mockMvc.perform(post("/api/auth/login/step2")
                        .header("Authorization", "Bearer partial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emailOtp", "123456"))))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid flow state"));
    }

    @Test
    void loginStep2_Failure_NullOtpInDatabase() throws Exception {
        sampleUser.setEmailOtpCode(null); // OTP was already used or never set

        Mockito.when(jwtUtils.getUsernameFromToken("partial-token")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token")).thenReturn("AWAITING_MFA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/login/step2")
                        .header("Authorization", "Bearer partial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emailOtp", "123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email OTP code"));
    }


    @Test
    void loginStep3_Success_CorrectSecurityAnswer() throws Exception {
        Mockito.when(jwtUtils.getUsernameFromToken("partial-token-step2")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token-step2")).thenReturn("AWAITING_QA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("fluffy", "encodedAnswer")).thenReturn(true);
        Mockito.when(jwtUtils.generateToken("testuser")).thenReturn("final-jwt-token");

        mockMvc.perform(post("/api/auth/login/step3")
                        .header("Authorization", "Bearer partial-token-step2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("securityAnswer", "fluffy"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("final-jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void loginStep3_Failure_WrongSecurityAnswer() throws Exception {
        Mockito.when(jwtUtils.getUsernameFromToken("partial-token-step2")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token-step2")).thenReturn("AWAITING_QA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.matches("wronganswer", "encodedAnswer")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login/step3")
                        .header("Authorization", "Bearer partial-token-step2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("securityAnswer", "wronganswer"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Security answer mismatch"));
    }

    @Test
    void loginStep3_Failure_WrongFlowState() throws Exception {
        Mockito.when(jwtUtils.getUsernameFromToken("partial-token-step2")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token-step2")).thenReturn("AWAITING_MFA"); // wrong step

        mockMvc.perform(post("/api/auth/login/step3")
                        .header("Authorization", "Bearer partial-token-step2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("securityAnswer", "fluffy"))))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid flow state"));
    }

    @Test
    void loginStep3_Failure_NullSecurityAnswer() throws Exception {
        Mockito.when(jwtUtils.getUsernameFromToken("partial-token-step2")).thenReturn("testuser");
        Mockito.when(jwtUtils.getStepFromToken("partial-token-step2")).thenReturn("AWAITING_QA");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/login/step3")
                        .header("Authorization", "Bearer partial-token-step2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Security answer mismatch"));
    }


    @Test
    void forgotPassword_Success_EmailExists() throws Exception {
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(sampleUser);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        Mockito.doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string("If that email is registered, a reset link has been sent."));

        Mockito.verify(emailService, Mockito.times(1)).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_Success_EmailDoesNotExist_SameResponseToPreventEnumeration() throws Exception {
        // Should return same message even if email doesn't exist (security: don't reveal emails)
        Mockito.when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "ghost@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string("If that email is registered, a reset link has been sent."));

        // Email service must NOT be called when user doesn't exist
        Mockito.verify(emailService, Mockito.never()).sendPasswordResetEmail(anyString(), anyString());
    }


    @Test
    void resetPassword_Success() throws Exception {
        sampleUser.setPasswordResetToken("valid-token");
        sampleUser.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        Mockito.when(userRepository.findByPasswordResetToken("valid-token")).thenReturn(sampleUser);
        Mockito.when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        Mockito.when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "valid-token",
                                "newPassword", "newPassword123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully."));
    }

    @Test
    void resetPassword_Failure_InvalidToken() throws Exception {
        Mockito.when(userRepository.findByPasswordResetToken("bad-token")).thenReturn(null);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "bad-token",
                                "newPassword", "newPassword123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired reset token."));
    }

    @Test
    void resetPassword_Failure_ExpiredToken() throws Exception {
        sampleUser.setPasswordResetToken("expired-token");
        sampleUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusMinutes(5)); // already expired

        Mockito.when(userRepository.findByPasswordResetToken("expired-token")).thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "expired-token",
                                "newPassword", "newPassword123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Reset token has expired."));
    }
}