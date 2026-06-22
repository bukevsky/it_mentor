package com.example.it.mentor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LogEmailServiceTest {

    private final LogEmailService service = new LogEmailService();

    @Test
    void sendPasswordResetOtp_doesNotLogOtpCode(CapturedOutput output) {
        service.sendPasswordResetOtp("user@example.com", "123456");

        assertThat(output)
                .contains("EMAIL STUB")
                .contains("user@example.com")
                .doesNotContain("123456");
    }
}
