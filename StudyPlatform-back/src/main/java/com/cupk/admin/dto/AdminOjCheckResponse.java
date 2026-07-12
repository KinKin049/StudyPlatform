package com.cupk.admin.dto;

import java.util.List;

/**
 * 管理员OJ题目校验响应DTO，用于返回OJ题目校验的整体结果。
 */
public record AdminOjCheckResponse(
        /**
         * 是否通过
         */
        Boolean passed,
        /**
         * 校验消息
         */
        String message,
        /**
         * 测试用例校验结果列表
         */
        List<AdminOjCaseCheckResponse> cases
) {
}