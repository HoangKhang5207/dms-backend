package com.genifast.dms.entity.enums;

public enum DeviceType {
    COMPANY_DEVICE(1),
    EXTERNAL_DEVICE(2);

    private final int value;

    DeviceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DeviceType fromValue(int value) {
        for (DeviceType type : DeviceType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid DeviceType value: " + value);
    }
}
