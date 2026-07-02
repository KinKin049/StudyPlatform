package com.cupk.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 保存压裂酸化增产仿真记录的请求体。
 */
public record SaveStimulationRecordRequest(
        Long userId,
        @NotBlank String type,
        @DecimalMin("0.0") Double sandVolume,
        @NotNull @DecimalMin("1.0") Double displacement,
        @DecimalMin("0.0") Double acidVolume,
        @NotNull @DecimalMin("0.0") Double fractureLength,
        @NotNull @DecimalMin("0.0") Double stimulationRatio
) {
}
