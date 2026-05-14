package com.example.it.mentor.dto.student;

public record StudentCompletionResponse(
        int percent,
        boolean mainDone,
        boolean aboutDone,
        boolean skillsDone,
        boolean resumeDone
) {}
