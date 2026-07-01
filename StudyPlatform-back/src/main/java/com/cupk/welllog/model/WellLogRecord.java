package com.cupk.welllog.model;

import java.time.LocalDateTime;

/**
 * 测井仿真记录，保存用户参数和前端生成的解释报告JSON。
 */
public record WellLogRecord(
        /** 记录主键。 */
        Long id,
        /** 操作用户主键，匿名记录为空。 */
        Long userId,
        /** 孔隙度百分比。 */
        Double porosity,
        /** 含油饱和度百分比。 */
        Double oilSaturation,
        /** 前端生成的解释报告 JSON 字符串。 */
        String reportJson,
        /** 记录创建时间。 */
        LocalDateTime createTime
) {
}
