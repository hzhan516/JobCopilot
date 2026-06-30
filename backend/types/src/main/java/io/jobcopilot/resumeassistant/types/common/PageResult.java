package io.jobcopilot.resumeassistant.types.common;

import java.util.List;

/** 分页结果（不依赖 Spring Data）/ Paginated result (no Spring Data dependency) */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long total) {
        return new PageResult<>(content, page, size, total, size > 0 ? (int) Math.ceil((double) total / size) : 0);
    }
}
