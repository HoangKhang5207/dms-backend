package com.genifast.dms.service.bpmn;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.dto.bpmn.BpmnUploadDTO;
import com.genifast.dms.dto.bpmn.BpmnUploadHistoryDTO;
import com.genifast.dms.dto.bpmn.VersionInfoDTO;
import com.genifast.dms.dto.bpmn.request.SaveBRequest;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.entity.bpmn.BpmnUploadHistory;
import com.genifast.dms.mapper.bpmn.BpmnUploadHistoryMapper;
import com.genifast.dms.mapper.bpmn.BpmnUploadMapper;
import com.genifast.dms.repository.OrganizationRepository;
import com.genifast.dms.repository.bpmn.BpmnUploadHistoryRepository;
import com.genifast.dms.repository.bpmn.BpmnUploadRepository;
import com.genifast.dms.service.BaseServiceImpl;
import com.genifast.dms.service.azureStorage.AzureStorageService;

@Service
@AllArgsConstructor
@Slf4j
public class BpmnServiceImpl extends BaseServiceImpl<BpmnUpload, Long, BpmnUploadRepository>
    implements BpmnService {
  private final BpmnUploadHistoryRepository bpmnUploadHistoryRepository;

  private final AzureStorageService azureStorageService;

  private final BpmnUploadMapper bpmnUploadMapper;
  private final BpmnUploadHistoryMapper bpmnUploadHistoryMapper;

  private final OrganizationRepository organizationRepository;

  @Override
  public List<BpmnUploadDTO> getBpmnList(Long organizationId) {
    log.info("[getBpmnList] Input: organizationId={}", organizationId);
    try {
      List<BpmnUpload> bpmnUploads = repository.findByOrganizationIdAndIsDeletedFalse(organizationId);
      List<BpmnUploadDTO> dtos = bpmnUploadMapper.toDtos(bpmnUploads);
      log.info("[getBpmnList] Success, result size={}", dtos.size());
      return dtos;
    } catch (Exception e) {
      log.error("[getBpmnList] Error: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public BpmnUploadHistoryDTO getBpmnInfo(Long bpmnUploadId, int version) {
    log.info("[getBpmnInfo] Input: bpmnUploadId={}, version={}", bpmnUploadId, version);
    try {
      BpmnUploadHistory bpmnUploadHistory = null;
      if (version == -1) {
        bpmnUploadHistory = bpmnUploadHistoryRepository
            .findTopByBpmnUploadIdOrderByVersionDesc(bpmnUploadId)
            .orElse(null);
      } else {
        bpmnUploadHistory = bpmnUploadHistoryRepository
            .findByBpmnUploadIdAndVersion(bpmnUploadId, version)
            .orElse(null);
      }
      if (bpmnUploadHistory != null) {
        log.info("[getBpmnInfo] Success, found");
        return bpmnUploadHistoryMapper.toDto(bpmnUploadHistory);
      }
      log.warn("[getBpmnInfo] Not found");
      return null;
    } catch (Exception e) {
      log.error("[getBpmnInfo] Error: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public List<BpmnUploadDTO> getPublishedBpmnList(Long organizationId) {
    log.info("[getPublishedBpmnList] Input: organizationId={}", organizationId);
    try {
      List<BpmnUpload> bpmnUploads = repository.findByOrganizationIdAndIsPublishedTrueAndIsDeletedFalse(organizationId);
      List<BpmnUploadDTO> dtos = bpmnUploadMapper.toDtos(bpmnUploads);
      log.info("[getPublishedBpmnList] Success, result size={}", dtos.size());
      return dtos;
    } catch (Exception e) {
      log.error("[getPublishedBpmnList] Error: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public List<VersionInfoDTO> getVersionInfo(Long bpmnUploadId) {
    log.info("[getVersionInfo] Input: bpmnUploadId={}", bpmnUploadId);
    try {
      List<BpmnUploadHistory> bpmnUploadHistories = bpmnUploadHistoryRepository.findByBpmnUploadId(bpmnUploadId);
      if (bpmnUploadHistories != null) {
        List<VersionInfoDTO> versions = bpmnUploadHistories.stream().map(bpmnUploadHistoryMapper::toVersionInfo)
            .toList();
        log.info("[getVersionInfo] Success, found");
        return versions;
      }
      log.warn("[getVersionInfo] Not found");
    } catch (Exception e) {
      log.error("[getVersionInfo] Error: {}", e.getMessage(), e);
      throw e;
    }
    return List.of();
  }

  @Override
  public BpmnUpload getBpmnUpload(Long id) {
    log.info("[getBpmnUpload] Input: id={}", id);
    BpmnUpload result = repository.findById(id).orElse(null);
    if (result == null) {
      log.warn("[getBpmnUpload] Not found BPMN with id={}", id);
    } else {
      log.info("[getBpmnUpload] Found BPMN with id={}", id);
    }
    return result;
  }

  @Override
  @Transactional
  public BpmnUploadDTO saveBpmnUpload(SaveBRequest saveBRequest, Long organizationId) {
    BpmnUpload bpmnUpload;
    boolean isUpdate = saveBRequest.getBpmnUploadId() != null;
    if (isUpdate) {
      bpmnUpload = repository.findById(saveBRequest.getBpmnUploadId()).orElse(null);
      if (bpmnUpload == null) {
        String msg = "BPMN file with id=" + saveBRequest.getBpmnUploadId() + " not found";
        log.warn("[saveBpmn] {}", msg);
        throw new IllegalArgumentException(msg);
      }
      if (Boolean.TRUE.equals(bpmnUpload.getIsDeployed())) {
        String msg = "Cannot update or upload files for a deployed BPMN (id=" + bpmnUpload.getId() + ")";
        log.warn("[saveBpmn] {}", msg);
        throw new IllegalArgumentException(msg);
      }
      bpmnUpload.setVersion(bpmnUpload.getVersion() + 1);
    } else {
      bpmnUpload = new BpmnUpload();
      bpmnUpload.setVersion(1);
    }

    try {
      String bpmnUrl = azureStorageService.uploadFile(saveBRequest.getFile(), false);
      String svgUrl = azureStorageService.uploadFile(saveBRequest.getSvgFile(), true);

      bpmnUpload.setName(saveBRequest.getName());
      bpmnUpload.setPath(bpmnUrl);
      bpmnUpload.setPathSvg(svgUrl);
      bpmnUpload.setIsPublished(saveBRequest.getIsPublished());

      Organization organization = this.organizationRepository.findById(organizationId)
          .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST,
              ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage()));

      bpmnUpload.setOrganization(organization);
    } catch (java.io.IOException e) {
      log.error("[saveBpmnUpload] File upload error: {}", e.getMessage(), e);
      throw new RuntimeException("File upload failed: " + e.getMessage(), e);
    }

    // 1. Lưu bản ghi chính
    BpmnUpload savedBpmnUpload = createOrUpdate(bpmnUpload);
    log.info("[saveBpmnUpload] Save successful, id={}", savedBpmnUpload.getId());

    // *** LOGIC MỚI: TẠO BẢN GHI LỊCH SỬ ***
    // 2. Tạo và lưu bản ghi lịch sử
    BpmnUploadHistory history = bpmnUploadHistoryMapper.fromBpmnUpload(savedBpmnUpload);
    bpmnUploadHistoryRepository.save(history);
    log.info("[saveBpmnUpload] Created history record for BpmnUpload ID {} version {}", savedBpmnUpload.getId(),
        savedBpmnUpload.getVersion());
    // *** KẾT THÚC LOGIC MỚI ***

    BpmnUploadDTO bpmnUploadDto = bpmnUploadMapper.toDto(savedBpmnUpload);
    log.info("[saveBpmnUpload] Output DTO: {}", bpmnUploadDto);
    return bpmnUploadDto;
  }

  @Override
  public void deleteBpmn(Long bpmnUploadId) {
    BpmnUpload bpmnUpload = repository.findById(bpmnUploadId).orElse(null);
    if (bpmnUpload == null || bpmnUpload.getIsDeleted()) {
      String msg = "BPMN with id=" + bpmnUploadId + " not found";
      log.warn("[deleteBpmn] {}", msg);
      throw new IllegalArgumentException(msg);
    }
    if (Boolean.TRUE.equals(bpmnUpload.getIsDeployed())) {
      String msg = "Cannot delete a deployed BPMN (id=" + bpmnUploadId + ")";
      log.warn("[deleteBpmn] {}", msg);
      throw new IllegalArgumentException(msg);
    }
    delete(bpmnUpload.getId());
    log.info("[deleteBpmn] Deleted BPMN with id={}", bpmnUploadId);
  }
}
