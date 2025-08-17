package com.genifast.dms.repository.bpmn;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.bpmn.BpmnUploadHistory;
import com.genifast.dms.repository.BaseRepository;

@Repository
public interface BpmnUploadHistoryRepository extends BaseRepository<BpmnUploadHistory, Long> {
  Optional<BpmnUploadHistory> findByBpmnUploadIdAndVersion(Long bpmnUploadId, int version);

  Optional<BpmnUploadHistory> findTopByBpmnUploadIdOrderByVersionDesc(Long bpmnUploadId);

  List<BpmnUploadHistory> findByBpmnUploadId(Long bpmnUploadId);
}
