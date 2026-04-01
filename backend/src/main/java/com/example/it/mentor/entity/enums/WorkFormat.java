package com.example.it.mentor.entity.enums;

public enum WorkFormat {
    REMOTE("Удалённо"),
    OFFICE("Офис"),
    HYBRID("Гибрид");

    private final String label;

    WorkFormat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
