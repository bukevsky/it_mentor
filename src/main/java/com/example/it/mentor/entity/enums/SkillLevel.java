package com.example.it.mentor.entity.enums;

public enum SkillLevel {
    BEGINNER("Начальный"),
    INTERMEDIATE("Средний"),
    CONFIDENT("Уверенный");

    private final String label;

    SkillLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
