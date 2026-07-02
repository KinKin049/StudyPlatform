package com.cupk.production.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 保存油藏动态仿真记录的请求体。
 */
public record SaveReservoirRecordRequest(
        Long userId,
        @NotNull @DecimalMin("10.0") @DecimalMax("40.0") Double formationPressure,
        @NotNull @DecimalMin("1.0") @DecimalMax("1000.0") Double permeability,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double waterSaturation,
        @NotNull @DecimalMin("1.0") @DecimalMax("50.0") Double viscosity,
        @NotNull @DecimalMin("0.0") Double dailyOil,
        @NotNull @DecimalMin("0.0") Double dailyWater
) {
}
