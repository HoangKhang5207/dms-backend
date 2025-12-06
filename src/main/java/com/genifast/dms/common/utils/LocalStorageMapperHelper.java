package com.genifast.dms.common.utils;

import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalStorageMapperHelper {

    @Value("${storage.local.base-url:/storage}")
    private String baseUrl;

    @Named("generateSASToken")
    public String generateSASToken(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        // Return direct URL to the file without SAS token
        return baseUrl + "/" + fileName;
    }
}
