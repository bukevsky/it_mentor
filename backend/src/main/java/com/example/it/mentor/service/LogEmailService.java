package com.example.it.mentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(SmtpEmailService.class)
public class LogEmailService implements EmailService {

    @Override
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        log.info("=== [EMAIL STUB] Кому: {} | OTP: {} ===", toEmail, otpCode);
    }
}
