package com.example.it.mentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Заглушка {@link EmailService}, которая пишет письма в лог вместо реальной отправки.
 *
 * <p>Используется как fallback, когда SMTP-провайдер не сконфигурирован.</p>
 */
@Slf4j
@Service
@ConditionalOnMissingBean(SmtpEmailService.class)
public class LogEmailService implements EmailService {

    /**
     * Логирует параметры письма для локальной разработки и тестовых запусков.
     *
     * @param toEmail адрес получателя
     * @param otpCode одноразовый код сброса пароля
     */
    @Override
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        log.info("=== [EMAIL STUB] OTP-код сформирован для отправки: to={}, step={} ===",
                toEmail, "password_reset_otp_stub");
    }

    @Override
    public void send(EmailMessage message) {
        log.info("=== [EMAIL STUB] Письмо сформировано для отправки: to={}, subject={}, step={} ===",
                message.to(), message.subject(), "email_stub_send");
    }
}
