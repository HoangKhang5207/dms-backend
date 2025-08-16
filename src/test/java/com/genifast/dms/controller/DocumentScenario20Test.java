package com.genifast.dms.controller;

import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.FileStorageService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario20Test {

    private MockMvc mockMvc;

    @Mock private DocumentRepository documentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;

    private static Document buildPublicDoc06() {
        Document d = new Document();
        d.setId(6L);
        d.setShareToken("token-abc123");
        d.setPublicShareExpiryAt(Instant.now().plusSeconds(3600));
        d.setAllowPublicDownload(true);
        d.setOriginalFilename("ke-hoach-dao-tao-2025.pdf");
        d.setType("pdf");
        return d;
    }

    // [20.1-init] Khởi tạo thanh toán → 200, trả về paymentUrl
    @Test
    @DisplayName("[20.1-init] Khởi tạo thanh toán VNPay thành công (200)")
    void scenario201_InitiatePayment_Success() throws Exception {
        String token = "token-abc123";
        Document doc = buildPublicDoc06();
        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/public/{token}/initiate-payment", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentUrl").value("https://sandbox.vnpay.vn/paymentv2/vpcpay.html"));
    }

    // [20.1-view] Visitor đã thanh toán xem toàn bộ (paid) → 200
    @Test
    @DisplayName("[20.1-view] Visitor đã thanh toán xem toàn bộ tài liệu (200)")
    void scenario201_ViewAfterPaid_Success() throws Exception {
        String token = "token-abc123";
        Document doc = buildPublicDoc06();

        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));
        byte[] content = "PDF_BYTES_VIEW".getBytes();
        Mockito.when(fileStorageService.retrieveFileForVisitor(doc)).thenReturn(content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}", token)
                        .header("X-Payment-Status", "paid"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("inline")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes(content));
    }

    @BeforeEach
    void setup() {
        PublicDocumentController controller = new PublicDocumentController(documentRepository, fileStorageService, auditLogService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();
    }

    // [20.1] Visitor mua tài liệu PUBLIC thành công qua VNPay -> tải xuống OK (paid_download)
    @Test
    @DisplayName("[20.1] Visitor đã thanh toán tải doc-06 thành công (200)")
    void scenario201_DownloadAfterPaid_Success() throws Exception {
        String token = "token-abc123";
        Document doc = buildPublicDoc06();

        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));
        byte[] content = "PDF_BYTES".getBytes();
        Mockito.when(fileStorageService.retrieveFileForVisitor(doc)).thenReturn(content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/download", token)
                        .header("X-Payment-Status", "paid_download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes(content));
    }

    // [20.2] Visitor thanh toán thất bại -> download bị chặn 403 (Payment required for download.)
    @Test
    @DisplayName("[20.2] Visitor nhập sai OTP: download bị chặn (403)")
    void scenario202_DownloadWhenPaymentFailed_Forbidden() throws Exception {
        String token = "token-abc123";
        Document doc = buildPublicDoc06();
        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/download", token)
                        .header("X-Payment-Status", "failed"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Payment required for download."));
    }

    // [20.3] Visitor hủy thanh toán -> download bị chặn 403 (Payment required for download.)
    @Test
    @DisplayName("[20.3] Visitor hủy thanh toán: download bị chặn (403)")
    void scenario203_DownloadWhenPaymentCancelled_Forbidden() throws Exception {
        String token = "token-abc123";
        Document doc = buildPublicDoc06();
        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/download", token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Payment required for download."));
    }
}
