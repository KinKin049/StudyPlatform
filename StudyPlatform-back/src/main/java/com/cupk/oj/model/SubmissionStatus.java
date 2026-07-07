package com.cupk.oj.model;

/**
 * 提交状态枚举
 */
public enum SubmissionStatus {
    /**
     * 等待评测
     */
    PENDING,
    /**
     * 评测中
     */
    JUDGING,
    /**
     * 答案正确
     */
    ACCEPTED,
    /**
     * 答案错误
     */
    WRONG_ANSWER,
    /**
     * 超时
     */
    TIME_LIMIT_EXCEEDED,
    /**
     * 内存超限
     */
    MEMORY_LIMIT_EXCEEDED,
    /**
     * 运行时错误
     */
    RUNTIME_ERROR,
    /**
     * 编译错误
     */
    COMPILE_ERROR,
    /**
     * 系统错误
     */
    SYSTEM_ERROR
}