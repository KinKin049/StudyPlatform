package com.cupk.production.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 保存抽油机示功图仿真记录的请求体。
 */
public record SavePumpRecordRequest(
        Long userId,
        @NotNull @DecimalMin("1.0") @DecimalMax("6.0") Double stroke,
        @NotNull @DecimalMin("1.0") @DecimalMax("12.0") Double strokeTimes,
        @NotNull @DecimalMin("1.0") Double pumpDiameter,
        @NotBlank String workCondition,
        @NotBlank String indicatorChartData
) {
}
