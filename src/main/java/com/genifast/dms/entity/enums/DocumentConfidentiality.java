package com.genifast.dms.entity.enums;

public enum DocumentConfidentiality {
    PUBLIC(1),
    INTERNAL(2),
    PRIVATE(3),
    LOCKED(4),
    PROJECT(5),
    EXTERNAL(6);

    private final int value;

    DocumentConfidentiality(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DocumentConfidentiality fromValue(int value) {
        for (DocumentConfidentiality confidentiality : DocumentConfidentiality.values()) {
            if (confidentiality.getValue() == value) {
                return confidentiality;
            }
        }
        throw new IllegalArgumentException("Invalid DocumentConfidentiality value: " + value);
    }
}
