package com.genifast.dms.service.azureStorage;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AzureStorageService {

  @Value("${azure.storage.account-name}")
  private String accountName;

  @Value("${azure.storage.account-key}")
  private String accountKey;

  @Value("${azure.storage.endpoint}")
  private String endpoint;

  @Value("${azure.storage.container-name}")
  private String containerName;

  private BlobContainerClient getContainerClient() {
    String connectionString = String.format(
        "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
        accountName, accountKey);
    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString)
        .buildClient();
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  public String uploadFile(MultipartFile file, boolean isSvg) throws IOException {
    BlobContainerClient containerClient = getContainerClient();

    if (!containerClient.exists()) {
      containerClient.create();
    }

    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    BlobClient blobClient = containerClient.getBlobClient(fileName);

    String contentType = isSvg ? "image/svg+xml" : "application/octet-stream";
    BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);

    try (InputStream inputStream = file.getInputStream()) {
      blobClient.uploadWithResponse(
          new BlobParallelUploadOptions(inputStream).setHeaders(headers), null, null);
    }

    return blobClient.getBlobUrl();
  }

  public String getBlobUrlWithSasToken(String fullBlobUrl) {
    try {
      URL url = new URL(fullBlobUrl);
      String[] pathParts = url.getPath().split("/", 3); // [ "", "container", "blobName" ]
      if (pathParts.length < 3) {
        throw new IllegalArgumentException("Invalid blob URL: " + fullBlobUrl);
      }

      String actualContainer = pathParts[1];
      String blobName = pathParts[2];

      String connectionString = String.format(
          "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
          accountName, accountKey);

      BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString)
          .buildClient();
      BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(actualContainer);
      BlobClient blobClient = containerClient.getBlobClient(blobName);

      BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
      BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusHours(1),
          permission);

      String sasToken = blobClient.generateSas(values);
      return blobClient.getBlobUrl() + "?" + sasToken;

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate SAS token for: " + fullBlobUrl, e);
    }
  }
}
