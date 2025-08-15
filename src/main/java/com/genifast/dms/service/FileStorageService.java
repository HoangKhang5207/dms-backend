package com.genifast.dms.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.entity.Document;

public interface FileStorageService {
    void init();

    String store(MultipartFile file);

    Path load(String filename);

    Resource loadAsResource(String filename);

    void delete(String filename);

    List<Document> storeMultipleFiles(MultipartFile[] files, String password, Long categoryId, Integer accessType) throws Exception;

    byte[] retrieveFileById(String fileId, String password, boolean withWatermark) throws Exception;

    String getOriginalFileName(String fileId) throws Exception;

    String getOriginalDocumentType(String fileId) throws Exception;

    void deleteFileById(String fileId) throws Exception;

    void deleteMultipleFilesByIds(List<String> fileIds) throws Exception;

    List<Document> getAllDocuments();

    void changeFilePassword(String fileId, String oldPassword, String newPassword) throws Exception;

    boolean validateFilePassword(String fileId, String password) throws Exception;

    byte[] downloadMultipleFilesAsZip(List<Map<String, String>> fileDetails) throws Exception;

    boolean checkWatermark(String fileId) throws Exception;

    // Bổ sung phương thức cho Visitor
    byte[] retrieveFileForVisitor(Document document) throws Exception;
}
