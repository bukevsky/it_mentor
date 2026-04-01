package com.example.it.mentor.service;

public interface EmailService {
    void sendPasswordResetOtp(String toEmail, String otpCode);
}
