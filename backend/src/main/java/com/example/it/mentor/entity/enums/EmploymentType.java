package com.example.it.mentor.entity.enums;

public enum EmploymentType {
    PRACTICE("Практика"),
    INTERNSHIP("Стажировка"),
    PART_TIME("Part-time"),
    FULL_TIME("Full-time"),
    PROJECT("Проектная"),
    OTHER("Другое");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
