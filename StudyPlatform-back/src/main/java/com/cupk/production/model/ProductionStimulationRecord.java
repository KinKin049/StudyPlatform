package com.cupk.production.model;

import java.time.LocalDateTime;

/**
 * 压裂酸化增产仿真记录。
 */
public record ProductionStimulationRecord(
        Long id,
        Long userId,
        String type,
        Double sandVolume,
        Double displacement,
        Double acidVolume,
        Double fractureLength,
        Double stimulationRatio,
        LocalDateTime createTime
) {
}
