package com.cupk.admin.dto;

/**
 * 管理员课程响应DTO，用于返回课程的详细信息。
 */
public record AdminCourseResponse(
        /**
         * 课程类型
         */
        String resourceType,
        /**
         * 课程编号
         */
        String id,
        /**
         * 课程名称
         */
        String name,
        /**
         * 教师姓名
         */
        String teacher,
        /**
         * 课程分类
         */
        String category,
        /**
         * 学校名称
         */
        String school,
        /**
         * 封面URL
         */
        String coverUrl,
        /**
         * 封面本地路径
         */
        String coverFilePath,
        /**
         * 开课时间
         */
        String startTime,
        /**
         * 参与人数
         */
        Integer participants,
        /**
         * 课程简介
         */
        String comment,
        /**
         * 课程详情描述
         */
        String description,
        /**
         * 学期计划
         */
        String semesterPlan,
        /**
         * 课程概述
         */
        String overview,
        /**
         * 视频本地路径
         */
        String videoFilePath,
        /**
         * 课程链接
         */
        String link,
        /**
         * 是否认证课程
         */
        boolean certified,
        /**
         * 认证标签
         */
        String certificationLabel
) {
}