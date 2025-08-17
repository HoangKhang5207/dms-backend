package com.genifast.dms.dto.bpmn.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveBRequest {
    private Long bpmnUploadId;

    @NotNull
    private String name;

    @NotNull
    private MultipartFile file;

    @NotNull
    private MultipartFile svgFile;

    private Boolean isPublished = false;
}
