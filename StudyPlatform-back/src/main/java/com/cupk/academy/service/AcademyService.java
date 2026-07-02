package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
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
