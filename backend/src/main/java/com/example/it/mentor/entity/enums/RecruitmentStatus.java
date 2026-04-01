package com.example.it.mentor.entity.enums;

public enum RecruitmentStatus {
    OPEN("Набираю"),
    PAUSED("Пауза"),
    CLOSED("Нет мест");

    private final String label;

    RecruitmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
