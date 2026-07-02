package com.cupk.production.dto;

import java.util.List;

/**
 * 采油生产仿真记录分页结果。
 *
 * @param total 总记录数
 * @param page 当前页码，从 1 开始
 * @param size 每页记录数
 * @param records 当前页数据
 * @param <T> 记录类型
 */
public record ProductionPage<T>(
        long total,
        int page,
        int size,
        List<T> records
) {
}
