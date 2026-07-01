package com.cupk.welllog.dto;

import com.cupk.welllog.model.WellLogRecord;
import java.util.List;

/**
 * 简化分页结果，供前端展示当前用户的测井仿真历史。
 */
public record WellLogRecordPage(
        /** 总记录数。 */
        long total,
        /** 当前页码，从 1 开始。 */
        int page,
        /** 每页记录数。 */
        int size,
        /** 当前页记录列表。 */
        List<WellLogRecord> records
) {
}
