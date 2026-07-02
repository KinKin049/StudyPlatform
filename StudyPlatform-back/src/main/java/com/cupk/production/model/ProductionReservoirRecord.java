package com.cupk.production.model;

import java.time.LocalDateTime;

/**
 * 油藏动态仿真记录。
 */
public record ProductionReservoirRecord(
        Long id,
        Long userId,
        Double formationPressure,
        Double permeability,
        Double waterSaturation,
        Double viscosity,
        Double dailyOil,
        Double dailyWater,
        LocalDateTime createTime
) {
}
