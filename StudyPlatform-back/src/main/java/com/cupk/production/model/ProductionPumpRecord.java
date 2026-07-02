package com.cupk.production.model;

import java.time.LocalDateTime;

/**
 * 抽油机示功图仿真记录。
 */
public record ProductionPumpRecord(
        Long id,
        Long userId,
        Double stroke,
        Double strokeTimes,
        Double pumpDiameter,
        String workCondition,
        String indicatorChartData,
        LocalDateTime createTime
) {
}
