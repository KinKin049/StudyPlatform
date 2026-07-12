package com.cupk.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员卡券项请求DTO，用于接收新增或更新卡券的参数。
 */
public record AdminVoucherItemRequest(
        /**
         * 卡券编号
         */
        String voucherKey,
        /**
         * 卡券类型
         */
        String voucherType,
        /**
         * 卡券名称
         */
        String name,
        /**
         * 卡券描述
         */
        String description,
        /**
         * 价格
         */
        Integer price,
        /**
         * 库存数量
         */
        Integer stockQuantity,
        /**
         * 是否不限库存
         */
        Boolean unlimitedStock,
        /**
         * 折扣类型
         */
        String discountType,
        /**
         * 门槛金额
         */
        BigDecimal thresholdAmount,
        /**
         * 折扣金额
         */
        BigDecimal discountAmount,
        /**
         * 折扣率
         */
        BigDecimal discountRate,
        /**
         * 最大折扣金额
         */
        BigDecimal maxDiscountAmount,
        /**
         * 有效期开始时间
         */
        LocalDateTime validFrom,
        /**
         * 有效期结束时间
         */
        LocalDateTime validUntil,
        /**
         * 是否启用
         */
        Boolean enabled,
        /**
         * 排序序号
         */
        Integer sortOrder
) {
}