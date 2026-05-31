package com.amalia.harmonyhub_backend.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String recipientEmail, String otpCode) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            System.err.println("EmailService Error: Attempted to send OTP token but recipient address is empty.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("harmomyhub205@gmail.com");
            message.setTo(recipientEmail);
            message.setSubject("HarmonyHub - 3-Way Authentication Verification Code");

            String emailBody = String.format(
                    "Hello,\n\n" +
                            "You are attempting to access your HarmonyHub profile.\n" +
                            "Your Step 2 security channel verification code is: %s\n\n" +
                            "This token is strictly single-use and valid for a short window. " +
                            "If you did not request this code, please secure your account credentials immediately.\n\n" +
                            "Best regards,\n" +
                            "The HarmonyHub Security Team",
                    otpCode
            );

            message.setText(emailBody);
            mailSender.send(message);
            System.out.println(">>> Out-of-band email validation token successfully dispatched to: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to dispatch verification payload via SMTP transporter: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("HarmonyHub - Password Reset Request");
        message.setText("Click the link below to reset your password. This link expires in 15 minutes.\n\n" + resetLink + "\n\nIf you didn't request this, ignore this email.");
        mailSender.send(message);
    }
}
