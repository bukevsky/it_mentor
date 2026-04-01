package com.example.it.mentor.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.otp")
@Getter
@Setter
@NoArgsConstructor
public class OtpProperties {

    @Min(1)
    private int expirationMinutes = 15;

    @Min(1)
    private int maxAttempts = 5;
}
