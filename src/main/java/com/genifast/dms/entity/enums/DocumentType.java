package com.genifast.dms.entity.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    OUTGOING("1", "Công văn đi"),
    INCOMING("2", "Công văn đến");

    private final String code;
    private final String label;

    DocumentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DocumentType fromCode(String code) {
        for (DocumentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid DocumentType code: " + code);
    }
}
