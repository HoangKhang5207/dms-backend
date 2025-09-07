package com.genifast.dms.entity.enums;

public enum ProjectStatus {
    ACTIVE(1),
    COMPLETED(2),
    SUSPENDED(3);

    private final int value;

    ProjectStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ProjectStatus fromValue(int value) {
        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ProjectStatus value: " + value);
    }
}
