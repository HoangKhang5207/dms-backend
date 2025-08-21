package com.genifast.dms.controller;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.genifast.dms.common.constant.MessageCode.CommonMessage.*;

import com.genifast.dms.dto.BaseResponseDto;
import com.genifast.dms.dto.bpmn.BpmnUploadDTO;
import com.genifast.dms.dto.bpmn.BpmnUploadHistoryDTO;
import com.genifast.dms.dto.bpmn.VersionInfoDTO;
import com.genifast.dms.dto.bpmn.request.SaveBRequest;
import com.genifast.dms.service.bpmn.BpmnService;

@Validated
@RestController
@RequestMapping("/api/v1/bpmn")
@RequiredArgsConstructor
@Slf4j
public class BpmnController extends BaseController {
    private final BpmnService bpmnService;

    // *** ENDPOINT MỚI ĐỂ LÀM PROXY ***
    @GetMapping("/file-proxy")
    public ResponseEntity<Resource> proxyBpmnFile(@RequestParam("url") String fileUrl) {
        log.info("[proxyBpmnFile] Proxifying request for URL: {}", fileUrl);
        try {
            // Mở một stream trực tiếp từ URL của Azure
            URL url = new URL(fileUrl);
            InputStream inputStream = url.openStream();

            // Bọc stream trong một InputStreamResource để Spring Boot có thể xử lý
            InputStreamResource resource = new InputStreamResource(inputStream);

            // Trả về nội dung file với đúng content type
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML) // BPMN file là XML
                    .body(resource);

        } catch (Exception e) {
            log.error("[proxyBpmnFile] Error while proxifying file: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(null); // Trả về lỗi 500 với resource rỗng
        }
    }
    // *** KẾT THÚC ENDPOINT MỚI ***

    @GetMapping("/organization/{organization_id}")
    public ResponseEntity<BaseResponseDto> getBpmnList(
            @PathVariable("organization_id") Long organizationId) {
        log.info("[getBpmnList] Input: organizationId={}", organizationId);
        try {
            List<BpmnUploadDTO> bpmnUploadDtos = bpmnService.getBpmnList(organizationId);
            log.info("[getBpmnList] Success, result size={}", bpmnUploadDtos.size());
            return success(bpmnUploadDtos, SUCCESS_GET_DATA);
        } catch (Exception e) {
            log.error("[getBpmnList] Error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }

    @GetMapping("/organization/{organization_id}/bpmn_upload/{bpmn_upload_id}")
    public ResponseEntity<BaseResponseDto> getBpmnInfo(
            @PathVariable("bpmn_upload_id") Long bpmnUploadId, @RequestParam int version) {
        log.info("[getBpmnInfo] Input: bpmnUploadId={}, version={}", bpmnUploadId, version);
        try {
            BpmnUploadHistoryDTO bpmnUploadHistoryDto = bpmnService.getBpmnInfo(bpmnUploadId, version);
            log.info(
                    "[getBpmnInfo] Success, result {}", bpmnUploadHistoryDto != null ? "found" : "not found");
            return success(bpmnUploadHistoryDto, SUCCESS_GET_DATA);
        } catch (Exception e) {
            log.error("[getBpmnInfo] Error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }

    @GetMapping("/organization/{organization_id}/published")
    public ResponseEntity<BaseResponseDto> getPublishedBpmnList(
            @PathVariable("organization_id") Long organizationId) {
        log.info("[getBpmnInfo] Input: organizationId={}", organizationId);
        try {
            List<BpmnUploadDTO> bpmnUploadDtos = bpmnService.getPublishedBpmnList(organizationId);
            log.info("[getBpmnInfo] Success, result {}", bpmnUploadDtos != null ? "found" : "not found");
            return success(bpmnUploadDtos, SUCCESS_GET_DATA);
        } catch (Exception e) {
            log.error("[getBpmnInfo] Error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }

    @GetMapping("/organization/{organization_id}/bpmn_upload/{bpmn_upload_id}/versions")
    public ResponseEntity<BaseResponseDto> getVersionInfo(
            @PathVariable("bpmn_upload_id") Long bpmnUploadId) {
        log.info("[getVersionInfo] Input: bpmnUploadId={}", bpmnUploadId);
        try {
            List<VersionInfoDTO> versionInfo = bpmnService.getVersionInfo(bpmnUploadId);
            log.info("[getVersionInfo] Success, result {}", versionInfo != null ? "found" : "not found");
            return success(versionInfo, SUCCESS_GET_DATA);
        } catch (Exception e) {
            log.error("[getVersionInfo] Error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }

    @PostMapping("/organization/{organization_id}/save")
    @Transactional
    public ResponseEntity<BaseResponseDto> saveBpmn(
            @ModelAttribute SaveBRequest saveBRequest,
            @PathVariable("organization_id") Long organizationId) {
        try {
            if (saveBRequest.getFile() == null
                    || saveBRequest.getFile().isEmpty()
                    || saveBRequest.getSvgFile() == null
                    || saveBRequest.getSvgFile().isEmpty()) {
                String msg = "BPMN file/svgFile must not be empty";
                log.warn("[saveBpmn] {}", msg);
                return badRequest("The request could not be completed");
            }
            log.info(
                    "[saveBpmn] Input: bpmnFileId={}, file={}, svgFile={}",
                    saveBRequest.getBpmnUploadId(),
                    saveBRequest.getFile().getOriginalFilename(),
                    saveBRequest.getSvgFile().getOriginalFilename());

            BpmnUploadDTO bpmnUploadDto = bpmnService.saveBpmnUpload(saveBRequest, organizationId);
            return success(bpmnUploadDto, SUCCESS_UPDATE_DATA);
        } catch (IllegalArgumentException e) {
            log.error("[saveBpmn] Business error: {}", e.getMessage(), e);
            return badRequest("Business error: " + e.getMessage());
        } catch (Exception e) {
            log.error("[saveBpmn] System error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }

    @DeleteMapping("/organization/{organization_id}/bpmn_upload/{bpmn_upload_id}")
    @Transactional
    public ResponseEntity<BaseResponseDto> deleteBpmn(
            @PathVariable("bpmn_upload_id") Long bpmnUploadId) {
        try {
            bpmnService.deleteBpmn(bpmnUploadId);
            log.info("[deleteBpmn] Deleted BPMN with id={}", bpmnUploadId);
            return success(SUCCESS_DELETE_DATA);
        } catch (IllegalArgumentException e) {
            log.error("[deleteBpmn] Business error: {}", e.getMessage(), e);
            return badRequest("Business error: " + e.getMessage());
        } catch (Exception e) {
            log.error("[deleteBpmn] System error: {}", e.getMessage(), e);
            return internalServerError("System error: " + e.getMessage());
        }
    }
}
