package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyHomeItemResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.repository.AcademyRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademyService {
    private final AcademyRepository academyRepository;

    public AcademyService(AcademyRepository academyRepository) {
        this.academyRepository = academyRepository;
    }

    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return withCourseCovers(academyRepository.findOnlineOpenCourses());
    }

    public List<AcademyHomeSectionResponse> getAcademyHome() {
        return List.of(
                new AcademyHomeSectionResponse("my-courses", "我的课程", List.of(
                        new AcademyHomeItemResponse("人工智能导论", "在线开放课程", "32 学时 · 8 个章节"),
                        new AcademyHomeItemResponse("大学生创新实践", "通识课程", "24 学时 · 项目制学习"),
                        new AcademyHomeItemResponse("数据分析微专业", "微专业课程", "6 门课 · 能力认证")
                )),
                new AcademyHomeSectionResponse("course-assignments", "课程作业", List.of(
                        new AcademyHomeItemResponse("C语言程序设计（下）", "待提交", "第 3 章函数练习 · 截止本周五"),
                        new AcademyHomeItemResponse("劳动通论", "进行中", "专题讨论 1 篇 · 已完成 60%"),
                        new AcademyHomeItemResponse("数据分析微专业", "待批阅", "项目报告已提交 · 等待教师反馈")
                )),
                new AcademyHomeSectionResponse("my-exams", "我的考试", List.of(
                        new AcademyHomeItemResponse("高等数学阶段测验", "未开始", "7 月 12 日 09:00 · 60 分钟"),
                        new AcademyHomeItemResponse("程序设计单元测试", "可进入", "7 月 8 日前完成 · 3 次机会"),
                        new AcademyHomeItemResponse("通识课程结课考试", "已预约", "线上闭卷 · 系统自动判分")
                ))
        );
    }

    public AcademyCourseResponse getOnlineOpenCourse(String id) {
        return withCourseCover(academyRepository.findOnlineOpenCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    public List<AcademyCourseResponse> listGeneralCourses() {
        return withCourseCovers(academyRepository.findGeneralCourses());
    }

    public AcademyCourseResponse getGeneralCourse(String id) {
        return withCourseCover(academyRepository.findGeneralCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return withCourseCovers(academyRepository.findMicroMajorCourses());
    }

    public AcademyCourseResponse getMicroMajorCourse(String id) {
        return withCourseCover(academyRepository.findMicroMajorCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    public List<AcademyTextbookResponse> listTextbooks() {
        return academyRepository.findTextbooks().stream()
                .map(textbook -> new AcademyTextbookResponse(
                        textbook.id(),
                        textbook.name(),
                        textbook.editor(),
                        textbook.category(),
                        textbook.publisher(),
                        textbook.publishDate(),
                        textbook.isbn(),
                        textbook.description(),
                        fileUrl(textbook.coverFilePath()),
                        textbook.coverUrl(),
                        textbook.coverFilePath(),
                        textbook.link()
                ))
                .toList();
    }

    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyRepository.findCategories("online_open_courses");
    }

    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyRepository.findCategories("general_courses");
    }

    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyRepository.findCategories("micro_major_courses");
    }

    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyRepository.findCategories("excellent_textbooks");
    }

    public AcademyCourseEnrollmentResponse enrollCourse(String resourceType, String courseId, Long userId) {
        ensureCourseExists(resourceType, courseId);
        academyRepository.enrollCourse(resourceType, courseId, userId);
        return new AcademyCourseEnrollmentResponse(true, "已参加课程");
    }

    public List<AcademyCourseReviewResponse> listCourseReviews(String resourceType, String courseId) {
        ensureCourseExists(resourceType, courseId);
        return academyRepository.findCourseReviews(resourceType, courseId);
    }

    public AcademyCourseReviewResponse saveCourseReview(
            String resourceType,
            String courseId,
            AcademyCourseReviewRequest request
    ) {
        ensureCourseExists(resourceType, courseId);
        String userName = request.userName().trim();
        String content = request.content().trim();
        if (userName.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评价内容不能为空");
        }
        return academyRepository.saveCourseReview(resourceType, courseId, userName, request.rating(), content);
    }

    private void ensureCourseExists(String resourceType, String courseId) {
        switch (resourceType) {
            case "online-open-courses" -> getOnlineOpenCourse(courseId);
            case "general-courses" -> getGeneralCourse(courseId);
            case "micro-major-courses" -> getMicroMajorCourse(courseId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程类型不支持");
        }
    }

    private List<AcademyCourseResponse> withCourseCovers(List<AcademyCourseResponse> courses) {
        return courses.stream()
                .map(this::withCourseCover)
                .toList();
    }

    private AcademyCourseResponse withCourseCover(AcademyCourseResponse course) {
        return new AcademyCourseResponse(
                course.id(),
                course.name(),
                course.teacher(),
                course.category(),
                course.school(),
                fileUrl(course.coverFilePath()),
                course.coverUrl(),
                course.coverFilePath(),
                course.startTime(),
                course.participants(),
                course.comment(),
                course.description(),
                course.link()
        );
    }

    private String fileUrl(String coverFilePath) {
        if (coverFilePath == null || coverFilePath.isBlank()) {
            return "";
        }
        String normalizedPath = coverFilePath.replace("\\", "/");
        if (normalizedPath.startsWith("storage/")) {
            normalizedPath = normalizedPath.substring("storage/".length());
        }
        String encodedPath = java.util.Arrays.stream(normalizedPath.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        return "/files/" + encodedPath;
    }
}
