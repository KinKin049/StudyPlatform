package com.cupk.production.model;

import java.time.LocalDateTime;

/**
 * 注水开发仿真记录。
 */
public record ProductionWaterfloodRecord(
        Long id,
        Long userId,
        Double injectionRate,
        Integer effectDay,
        Integer waterBreakthroughDay,
        Double peakOil,
        String productionCurve,
        LocalDateTime createTime
) {
}
