package com.example.it.mentor.entity.enums;

public enum LanguageLevel {
    A1("A1"),
    A2("A2"),
    B1("B1"),
    B2("B2"),
    C1("C1"),
    C2("C2"),
    NATIVE("Родной");

    private final String label;

    LanguageLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
