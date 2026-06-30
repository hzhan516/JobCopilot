package io.jobcopilot.resumeassistant.api.admin.facade;

import io.jobcopilot.resumeassistant.api.admin.dto.ComponentHealthResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.SystemStatsResponse;
import io.jobcopilot.resumeassistant.api.version.dto.VersionResponse;

public interface AdminSystemFacade {
    SystemStatsResponse getStats();
    ComponentHealthResponse getHealth();
    VersionResponse getVersion();
}
