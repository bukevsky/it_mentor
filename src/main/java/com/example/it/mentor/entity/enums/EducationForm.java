package com.example.it.mentor.entity.enums;

public enum EducationForm {
    FULL_TIME("Очно"),
    PART_TIME("Очно-заочно"),
    DISTANCE("Заочно");

    private final String label;

    EducationForm(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
