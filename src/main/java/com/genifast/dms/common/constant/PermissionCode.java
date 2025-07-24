package com.genifast.dms.common.constant;

public enum PermissionCode {
    LIST_APPLICATION(1000),
    ADD_NEW_APPLICATION(1001),
    EDIT_APPLICATION(1002),
    REMOVE_APPLICATION(1003),

    LIST_USER(1000),
    EDIT_USER(1002),

    VIEW_APP_PROVIDER(1000),
    EDIT_APP_PROVIDER(1002);

    private final int code;

    PermissionCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
