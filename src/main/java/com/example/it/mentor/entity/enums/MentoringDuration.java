package com.example.it.mentor.entity.enums;

public enum MentoringDuration {
    ONE_MONTH("1 месяц"),
    THREE_MONTHS("3 месяца"),
    FLEXIBLE("Гибко");

    private final String label;

    MentoringDuration(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
