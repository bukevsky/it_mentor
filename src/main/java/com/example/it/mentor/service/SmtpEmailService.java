package com.example.it.mentor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP-реализация {@link EmailService} для отправки писем через {@link JavaMailSender}.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "spring.mail.host")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * Отправляет пользователю письмо с OTP-кодом для сброса пароля.
     *
     * @param toEmail адрес получателя
     * @param otpCode одноразовый код сброса пароля
     */
    @Override
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Код сброса пароля IT Mentor");
        msg.setText("""
                Ваш код для сброса пароля: %s

                Код действителен 15 минут.
                Если вы не запрашивали сброс пароля — проигнорируйте это письмо.
                """.formatted(otpCode));
        mailSender.send(msg);
        log.info("OTP-код отправлен на: {}", toEmail);
    }
}
