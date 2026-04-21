package edu.asu.ser594.resumeassistant.api.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 简历编辑请求
 * Resume edit request
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEditRequest {

    @NotNull(message = "{validation.versionId.required}")
    private UUID versionId;

    @NotBlank(message = "{validation.content.required}")
    private String content;
}
