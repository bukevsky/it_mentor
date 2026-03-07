package com.example.it.mentor.entity.enums;

public enum FileType {
    RESUME("Резюме"),
    PORTFOLIO("Портфолио"),
    ATTACHMENT("Вложение");

    private final String label;

    FileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
