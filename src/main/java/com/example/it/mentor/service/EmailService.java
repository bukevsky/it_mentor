package com.example.it.mentor.service;

/**
 * Контракт сервиса отправки email-уведомлений.
 */
public interface EmailService {

    /**
     * Отправляет одноразовый код для сброса пароля.
     *
     * @param toEmail адрес получателя
     * @param otpCode одноразовый код подтверждения
     */
    void sendPasswordResetOtp(String toEmail, String otpCode);
}
