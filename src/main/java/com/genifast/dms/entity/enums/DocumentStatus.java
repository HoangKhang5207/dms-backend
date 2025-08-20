package com.genifast.dms.entity.enums;

public enum DocumentStatus {
    DRAFT(1),
    PENDING(2),
    APPROVED(3),
    REJECTED(4),
    ARCHIVED(5);

    private final int value;

    DocumentStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DocumentStatus fromValue(int value) {
        for (DocumentStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Unknown DocumentStatus value: " + value);
    }
}
