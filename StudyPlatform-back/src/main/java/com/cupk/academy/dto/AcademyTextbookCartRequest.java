package com.cupk.academy.dto;

/**
 * 教材购物车请求DTO，用于接收添加教材到购物车的请求参数。
 */
public record AcademyTextbookCartRequest(
        Long userId,
        String textbookId,
        Integer quantity
) {
}
