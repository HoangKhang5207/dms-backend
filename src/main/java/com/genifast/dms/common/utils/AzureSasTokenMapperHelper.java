package com.genifast.dms.common.utils;

import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genifast.dms.service.azureStorage.AzureStorageService;

@Component
public class AzureSasTokenMapperHelper {

    @Autowired
    private AzureStorageService azureStorageService;

    @Named("generateSASToken")
    public String generateSASToken(String path) {
        return azureStorageService.getBlobUrlWithSasToken(path);
    }
}