package com.example.it.mentor.entity.enums;

public enum MentoringChannel {
    CHAT("Чат"),
    CALLS("Созвоны"),
    MIXED("Смешанный");

    private final String label;

    MentoringChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
