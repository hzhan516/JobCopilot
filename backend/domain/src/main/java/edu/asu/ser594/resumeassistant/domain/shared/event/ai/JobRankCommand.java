package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

import java.util.List;
import java.util.Map;

/**
 * 职位精排请求命令
 * Job rank command
 *
 * @param matchId         匹配任务ID / Match task ID
 * @param userId          用户ID / User ID
 * @param resumeVersionId 简历版本ID / Resume version ID
 * @param resumeText      简历文本内容 / Resume text content
 * @param query           用户查询词 / User query
 * @param recalledJobIds  召回的职位ID列表 / Recalled job IDs
 * @param jobDetails      召回职位的详细信息映射 / Recalled job details map
 */
public record JobRankCommand(
        String matchId,
        String userId,
        String resumeVersionId,
        String resumeText,
        String query,
        List<String> recalledJobIds,
        Map<String, Object> jobDetails
) {
}
