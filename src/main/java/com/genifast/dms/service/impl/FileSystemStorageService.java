package com.genifast.dms.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.common.exception.StorageException;
import com.genifast.dms.common.exception.StorageFileNotFoundException;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Category;
import com.genifast.dms.entity.FileUpload;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.FileUploadRepository;
import com.genifast.dms.repository.UserDocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.CryptoService;
import com.genifast.dms.service.util.WatermarkService;
import com.lowagie.text.pdf.PdfEncryptor;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.genifast.dms.common.utils.JwtUtils;

import jakarta.annotation.PostConstruct;

@Service
public class FileSystemStorageService implements FileStorageService {

    private final Path rootLocation;
    private final CryptoService cryptoService;
    private final DocumentRepository documentRepository;
    private final FileUploadRepository fileUploadRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final WatermarkService watermarkService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public FileSystemStorageService(@Value("${storage.location}") String storageLocation, CryptoService cryptoService,
            DocumentRepository documentRepository, FileUploadRepository fileUploadRepository,
            UserDocumentRepository userDocumentRepository, WatermarkService watermarkService,
            UserRepository userRepository, CategoryRepository categoryRepository) {
        this.rootLocation = Paths.get(storageLocation);
        this.cryptoService = cryptoService;
        this.documentRepository = documentRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.userDocumentRepository = userDocumentRepository;
        this.watermarkService = watermarkService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location");
        }
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty() || originalFilename.contains("..")) {
                throw new StorageException("Failed to store file with relative path outside current directory.");
            }
            // Tạo tên file duy nhất để tránh xung đột
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String filename = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + "_" + filename + "." + extension;

            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return uniqueFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file.");
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file.");
        }
    }

    @Override
    public List<Document> storeMultipleFiles(MultipartFile[] files, String password, Long categoryId, Integer accessType) throws Exception {
        List<Document> uploadedDocuments = new ArrayList<>();
        for (MultipartFile file : files) {
            uploadedDocuments.add(storeFile(file, password, categoryId, accessType));
        }
        return uploadedDocuments;
    }

    private Document storeFile(MultipartFile file, String password, Long categoryId, Integer accessType) throws Exception {
        Path uploadPath = this.rootLocation.toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex);
        }

        byte[] fileBytes = file.getBytes();
        byte[] watermarkedBytes;

        String effectivePassword = (password != null && !password.isEmpty()) ? password : "default_password";

        // Generate a single unique file ID early
        String fileId = UUID.randomUUID().toString();

        // 1. Apply watermark
        InputStream watermarkedStream = null;
        String fileType = fileExtension.isEmpty() ? "" : fileExtension.substring(1);

        // Use the generated fileId as the watermark content
        if (fileType.equalsIgnoreCase("pdf")) {
            watermarkedStream = watermarkService.addWatermark(new ByteArrayInputStream(fileBytes), fileId);
        } else if (fileType.equalsIgnoreCase("docx")) {
            watermarkedStream = watermarkService.addWatermarkToDocx(new ByteArrayInputStream(fileBytes), fileId);
        } else if (fileType.equalsIgnoreCase("png") || fileType.equalsIgnoreCase("jpg")
                || fileType.equalsIgnoreCase("jpeg")) {
            watermarkedStream = watermarkService.addWatermarkToImage(
                    new ByteArrayInputStream(fileBytes),
                    fileId,
                    fileType);
        }

        if (watermarkedStream != null) {
            watermarkedBytes = watermarkedStream.readAllBytes();
        } else {
            // If no watermark is applied (unsupported file type), use the original bytes
            watermarkedBytes = fileBytes;
        }

        // 2. Encrypt the (potentially watermarked) file bytes
        byte[] encryptedBytes = cryptoService.encrypt(watermarkedBytes, effectivePassword);

        // 3. Generate a single unique file ID
        // String fileId = UUID.randomUUID().toString(); // Already generated above
        String storedFileName = fileId + fileExtension + ".enc";
        Path filePath = uploadPath.resolve(storedFileName);

        // 4. Save the single encrypted file
        Files.write(filePath, encryptedBytes);

        // 5. Save document metadata to database, pointing both paths to the same file
        Document document = new Document();
        document.setTitle(originalFileName);
        document.setFileId(fileId); // Use the single ID
        document.setFilePath(storedFileName); // Path to the single encrypted file
        document.setPhotoId(null); // Ensure photoId is null as it's not used for watermark ID
        document.setContent("Encrypted file content stored at " + filePath.toString());
        document.setStatus(1);

        // Get current user's login (email)
        String currentUserEmail = JwtUtils.getCurrentUserLogin().orElse("anonymous");
        document.setCreatedBy(currentUserEmail);

        document.setType(fileType);
        document.setOriginalFilename(originalFileName);
        document.setContentType(file.getContentType());
        document.setCreatedAt(java.time.Instant.now());
        document.setUpdatedAt(java.time.Instant.now());
        document.setStorageCapacity(file.getSize());
        document.setStorageUnit("bytes");
        document.setPassword(cryptoService.encryptString(effectivePassword, "password_encryption_key"));

        // Map category and related scopes
        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(cat -> {
                document.setCategory(cat);
                document.setDepartment(cat.getDepartment());
                document.setOrganization(cat.getOrganization());
            });
        }
        // accessType default = 1 (INTERNAL) if not provided
        document.setAccessType(accessType != null ? accessType : 1);

        Document savedDocument = documentRepository.save(document);

        // Save file upload metadata to database
        FileUpload fileUpload = new FileUpload();
        fileUpload.setDocumentId(savedDocument.getId());
        fileUpload.setFilePath(storedFileName); // Link to the single file

        // Find user by email and set their ID
        userRepository.findByEmail(currentUserEmail).ifPresent(user -> fileUpload.setUserId(user.getId()));

        fileUploadRepository.save(fileUpload);

        return savedDocument;
    }

    @Override
    public byte[] retrieveFileById(String fileId, String password, boolean withWatermark) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        Document document = documentOptional.get();
        String effectivePassword = (password != null && !password.isEmpty()) ? password : "default_password";

        Path downloadPath = this.rootLocation.toAbsolutePath().normalize();
        String fileNameToRetrieve = document.getFilePath();

        if (fileNameToRetrieve == null || fileNameToRetrieve.isEmpty()) {
            throw new Exception("File path for retrieval is empty.");
        }

        Path filePath = downloadPath.resolve(fileNameToRetrieve);

        if (!Files.exists(filePath)) {
            throw new Exception("File not found at path: " + filePath.toString());
        }

        byte[] encryptedContent = Files.readAllBytes(filePath);
        byte[] decryptedContent = cryptoService.decrypt(encryptedContent, effectivePassword);

        // If the user wants the file WITHOUT a watermark, remove it on-the-fly.
        if (!withWatermark) {
            String fileType = document.getType();
            String watermarkId = document.getFileId(); // Use fileId as the watermark ID
            InputStream cleanStream = null;

            if (watermarkId != null) {
                if (fileType.equalsIgnoreCase("pdf")) {
                    cleanStream = watermarkService.removePdfWatermark(new ByteArrayInputStream(decryptedContent),
                            watermarkId);
                } else if (fileType.equalsIgnoreCase("docx")) {
                    cleanStream = watermarkService.removeDocxWatermark(new ByteArrayInputStream(decryptedContent),
                            watermarkId);
                }
            }
            // Note: Image watermark removal is not implemented, so images will be returned
            // as-is.

            if (cleanStream != null) {
                return cleanStream.readAllBytes();
            }
        }

        // Return the decrypted (and still watermarked) content for viewing or for
        // download with watermark.
        return decryptedContent;
    }

    @Override
    public String getOriginalFileName(String fileId) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        return documentOptional.get().getTitle();
    }

    @Override
    public String getOriginalDocumentType(String fileId) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        return documentOptional.get().getType();
    }

    @Override
    @Transactional
    public void deleteFileById(String fileId) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            // If document not found, consider it already deleted and return without error
            return;
        }
        Document document = documentOptional.get();
        String storedFileName = document.getFilePath();

        // Delete associated user_documents records
        userDocumentRepository.deleteByDocumentId(document.getId());

        // Delete associated file_uploads records
        fileUploadRepository.deleteByDocumentId(document.getId());

        // Check if the storedFileName is a URL before attempting to delete a local
        // file
        if (!storedFileName.startsWith("http://") && !storedFileName.startsWith("https://")) {
            // Delete the physical file from the uploads directory
            Path filePath = this.rootLocation.toAbsolutePath().normalize().resolve(storedFileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                System.out.println(
                        "Warning: Physical file not found at " + filePath.toString()
                                + ", but proceeding with database deletion.");
            }

            // The photoId field is no longer used for physical file paths, so this block
            // is not needed.
            // Remove the block that checks and deletes photoId related files.

        } else {
            System.out.println("Info: Stored file path is a URL. Skipping local file deletion for: " + storedFileName);
        }

        // Delete the document record from the database
        documentRepository.delete(document);
    }

    @Override
    @Transactional
    public void deleteMultipleFilesByIds(List<String> fileIds) throws Exception {
        for (String fileId : fileIds) {
            deleteFileById(fileId);
        }
    }

    @Override
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public void changeFilePassword(String fileId, String oldPassword, String newPassword) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        Document document = documentOptional.get();
        String storedFileName = document.getFilePath();

        // Retrieve and decrypt the file with the old password
        byte[] decryptedBytes = retrieveFileById(fileId, oldPassword, false);

        // Encrypt the file with the new password
        byte[] newEncryptedBytes;
        if (newPassword != null && !newPassword.isEmpty()) {
            newEncryptedBytes = cryptoService.encrypt(decryptedBytes, newPassword);
        } else {
            newEncryptedBytes = cryptoService.encrypt(decryptedBytes, "default_password");
        }

        // Overwrite the existing encrypted file
        Path filePath = this.rootLocation.toAbsolutePath().normalize().resolve(storedFileName);
        Files.write(filePath, newEncryptedBytes);

        // Update the password in the document record
        if (newPassword != null && !newPassword.isEmpty()) {
            document.setPassword(cryptoService.encryptString(newPassword, "password_encryption_key"));
        } else {
            document.setPassword(cryptoService.encryptString("default_password", "password_encryption_key"));
        }
        document.setUpdatedAt(java.time.Instant.now());
        documentRepository.save(document);
    }

    @Override
    public boolean validateFilePassword(String fileId, String password) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        Document document = documentOptional.get();

        // Decrypt the stored password
        String storedPassword = cryptoService.decryptString(document.getPassword(), "password_encryption_key");

        // Compare with the provided password
        return storedPassword.equals(password);
    }

    @Override
    public byte[] downloadMultipleFilesAsZip(List<Map<String, String>> fileDetails) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map<String, String> fileDetail : fileDetails) {
                String fileId = fileDetail.get("fileId");
                String password = fileDetail.get("password");

                byte[] decryptedContent = retrieveFileById(fileId, password, false);
                String originalFileName = getOriginalFileName(fileId);

                ZipEntry entry = new ZipEntry(originalFileName);
                zos.putNextEntry(entry);
                zos.write(decryptedContent);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    @Override
    public boolean checkWatermark(String fileId) throws Exception {
        Optional<Document> documentOptional = documentRepository.findByFileId(fileId);
        if (documentOptional.isEmpty()) {
            throw new Exception("Document not found with fileId: " + fileId);
        }
        Document document = documentOptional.get();
        String fileType = document.getType();
        String expectedWatermarkId = document.getFileId(); // Use fileId as the expected watermark ID

        // Retrieve the raw (but encrypted) file content
        Path filePath = this.rootLocation.toAbsolutePath().normalize().resolve(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new Exception("File not found at path: " + filePath.toString());
        }
        byte[] encryptedContent = Files.readAllBytes(filePath);

        // We must decrypt the file before checking for a watermark
        // For simplicity, we assume a default or known password context for this check
        String effectivePassword = "default_password"; // Or retrieve based on context
        try {
            String storedPassword = cryptoService.decryptString(document.getPassword(), "password_encryption_key");
            effectivePassword = storedPassword;
        } catch (Exception e) {
            // Fallback to default if password decryption fails
            System.out.println("Could not decrypt password for check, falling back to default.");
        }

        byte[] decryptedContent = cryptoService.decrypt(encryptedContent, effectivePassword);
        InputStream inputStream = new ByteArrayInputStream(decryptedContent);

        List<String> foundWatermarks = new ArrayList<>();
        if (fileType.equalsIgnoreCase("pdf")) {
            foundWatermarks = watermarkService.extractPossibleWatermarkIdsFromPdf(inputStream);
        } else if (fileType.equalsIgnoreCase("docx")) {
            foundWatermarks = watermarkService.extractHiddenStringsFromDocx(inputStream);
        }

        // Check if any of the found watermarks match the expected fileId
        return foundWatermarks.contains(expectedWatermarkId);
    }

    // Tạo một phương thức mới dành riêng cho Visitor
    @Override
    public byte[] retrieveFileForVisitor(Document document) throws Exception {
        // 1. Lấy file đã giải mã (giả sử link public không cần password)
        byte[] decryptedContent = retrieveFileById(document.getFileId(), null, false); // Lấy file gốc, chưa có
                                                                                       // watermark

        String fileType = document.getType();
        InputStream finalStream = new ByteArrayInputStream(decryptedContent);

        // 2. Thêm Watermark dành riêng cho Visitor
        String visitorWatermark = "Guest Access - " + Instant.now().toString();
        if (fileType.equalsIgnoreCase("pdf")) {
            finalStream = watermarkService.addWatermark(finalStream, visitorWatermark);
        } else if (fileType.equalsIgnoreCase("docx")) {
            // ... xử lý cho docx
        }

        byte[] watermarkedBytes = finalStream.readAllBytes();

        // 3. Nếu là file PDF, áp dụng quyền hạn chế
        if (fileType.equalsIgnoreCase("pdf")) {
            ByteArrayOutputStream protectedOutputStream = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(watermarkedBytes);
            // Áp dụng quyền: chỉ cho phép xem, không cho in, không cho sửa, không cho copy
            PdfEncryptor.encrypt(reader, protectedOutputStream, null, null,
                    PdfWriter.ALLOW_SCREENREADERS, false);
            return protectedOutputStream.toByteArray();
        }

        return watermarkedBytes;
    }
}
