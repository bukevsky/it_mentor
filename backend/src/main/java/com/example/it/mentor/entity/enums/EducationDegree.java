package com.example.it.mentor.entity.enums;

public enum EducationDegree {
    BACHELOR("Бакалавр"),
    SPECIALIST("Специалист"),
    MASTER("Магистр"),
    COURSE("Курсы"),
    OTHER("Другое");

    private final String label;

    EducationDegree(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
