package com.example.it.mentor.service;

public record EmailMessage(
        String to,
        String subject,
        String htmlBody,
        String textBody
) {}
