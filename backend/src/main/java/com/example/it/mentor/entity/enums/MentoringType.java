package com.example.it.mentor.entity.enums;

public enum MentoringType {
    PRACTICE("Практика"),
    INTERNSHIP("Стажировка"),
    PROJECT("Проектное сопровождение");

    private final String label;

    MentoringType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
