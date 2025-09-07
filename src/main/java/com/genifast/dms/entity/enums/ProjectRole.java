package com.genifast.dms.entity.enums;

public enum ProjectRole {
    PROJECT_MANAGER(1),
    MEMBER(2),
    VIEWER(3);

    private final int value;

    ProjectRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ProjectRole fromValue(int value) {
        for (ProjectRole role : ProjectRole.values()) {
            if (role.getValue() == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid ProjectRole value: " + value);
    }
}
