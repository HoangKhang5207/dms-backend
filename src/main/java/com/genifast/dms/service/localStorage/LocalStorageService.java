package com.genifast.dms.service.localStorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService {

    @Value("${storage.local.path:./uploads}")
    private String storagePath;

    @Value("${storage.local.base-url:/uploads}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String uploadFile(MultipartFile file, boolean isSvg) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFilename = UUID.randomUUID().toString() + fileExtension;
        Path storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        Path destinationFile = storageDirectory.resolve(storedFilename).normalize();

        // Security check: ensure the file is stored within the allowed directory
        if (!destinationFile.startsWith(storageDirectory)) {
            throw new IOException("Cannot store file outside current directory.");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return storedFilename;
    }

    public String getBlobUrlWithSasToken(String filename) {
        // For local storage, we don't need SAS token
        return baseUrl + "/" + filename;
    }
}
