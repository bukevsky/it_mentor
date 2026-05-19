package com.example.it.mentor.dto.dict;

public record InteractionTypeResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}
