package com.cupk.auth.dto;

/**
 * 通用消息响应DTO，用于返回操作结果消息
 */
public record AuthMessageResponse(
        /**
         * 消息内容
         */
        String message
) {
}
