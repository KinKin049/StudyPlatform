package com.cupk.production.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 保存注水开发仿真记录的请求体。
 */
public record SaveWaterfloodRecordRequest(
        Long userId,
        @NotNull @DecimalMin("0.0") @DecimalMax("300.0") Double injectionRate,
        @NotNull @DecimalMin("0") Integer effectDay,
        @NotNull @DecimalMin("0") Integer waterBreakthroughDay,
        @NotNull @DecimalMin("0.0") Double peakOil,
        @NotBlank String productionCurve
) {
}
