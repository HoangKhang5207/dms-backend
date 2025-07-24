package com.genifast.dms.common.constant;

public enum PermissionModule {
    APPLICATION("GeneralConfiguration_AuthenticationVinid"),
    APPLICATION_USER("GeneralConfiguration_AuthenAppUser"),
    APP_PROVIDER("GeneralConfiguration_AppProvider");

    private final String value;

    PermissionModule(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
