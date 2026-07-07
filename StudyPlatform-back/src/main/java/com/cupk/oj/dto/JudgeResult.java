package com.cupk.oj.dto;

import com.cupk.oj.model.SubmissionStatus;
import java.util.List;

/**
 * 判题结果DTO。
 */
public record JudgeResult(
        /** 提交状态。 */
        SubmissionStatus status,
        /** 得分。 */
        Integer score,
        /** 用时（毫秒）。 */
        Integer timeUsedMs,
        /** 内存使用（千字节）。 */
        Integer memoryUsedKb,
        /** 消息。 */
        String message,
        /** 测试用例结果列表。 */
        List<JudgeCaseResult> cases
) {
}
