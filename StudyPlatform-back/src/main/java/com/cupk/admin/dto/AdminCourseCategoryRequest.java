package com.cupk.admin.dto;

/**
 * 管理员课程分类请求DTO，用于接收新增或更新课程分类的参数。
 */
public record AdminCourseCategoryRequest(
        /**
         * 课程类型
         */
        String resourceType,
        /**
         * 分类名称
         */
        String name
) {
}