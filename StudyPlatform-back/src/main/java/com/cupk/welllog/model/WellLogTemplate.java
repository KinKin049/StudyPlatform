package com.cupk.welllog.model;

import java.time.LocalDateTime;

/**
 * 测井基础模板，仅保存深度序列与基础GR曲线，不承载任何仿真计算逻辑。
 */
public record WellLogTemplate(
        /** 模板主键。 */
        Long id,
        /** 模板名称。 */
        String templateName,
        /** 深度序列 JSON 数组。 */
        String depthArray,
        /** 基础 GR 曲线 JSON 数组。 */
        String grBase,
        /** 模板备注。 */
        String remark,
        /** 模板创建时间。 */
        LocalDateTime createTime,
        /** 模板更新时间。 */
        LocalDateTime updateTime
) {
}
