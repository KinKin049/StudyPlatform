package com.cupk.welllog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 保存测井仿真记录的请求体，报告内容由前端生成后以JSON字符串提交。
 */
public record SaveWellLogRecordRequest(
        /** 操作用户主键，允许为空以支持未登录演示场景。 */
        Long userId,
        /** 孔隙度百分比，取值范围 0-35。 */
        @NotNull @DecimalMin("0.0") @DecimalMax("35.0") Double porosity,
        /** 含油饱和度百分比，取值范围 0-100。 */
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double oilSaturation,
        /** 前端生成的解释报告 JSON 字符串。 */
        @NotBlank String reportJson
) {
}
