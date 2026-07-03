package com.cupk.academy.controller;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentRequest;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.service.AcademyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService academyService;

    public AcademyController(AcademyService academyService) {
        this.academyService = academyService;
    }

    @GetMapping("/home")
    public List<AcademyHomeSectionResponse> getAcademyHome() {
        return academyService.getAcademyHome();
    }

    @GetMapping("/online-open-courses")
    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return academyService.listOnlineOpenCourses();
    }

    @GetMapping("/online-open-courses/{id}")
    public AcademyCourseResponse getOnlineOpenCourse(@PathVariable String id) {
        return academyService.getOnlineOpenCourse(id);
    }

    @PostMapping("/online-open-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollOnlineOpenCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("online-open-courses", id, request == null ? null : request.userId());
    }

    @GetMapping("/online-open-courses/{id}/reviews")
    public List<AcademyCourseReviewResponse> listOnlineOpenCourseReviews(@PathVariable String id) {
        return academyService.listCourseReviews("online-open-courses", id);
    }

    @PostMapping("/online-open-courses/{id}/reviews")
    public AcademyCourseReviewResponse saveOnlineOpenCourseReview(
            @PathVariable String id,
            @Valid @RequestBody AcademyCourseReviewRequest request
    ) {
        return academyService.saveCourseReview("online-open-courses", id, request);
    }

    @GetMapping("/online-open-courses/categories")
    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyService.listOnlineOpenCourseCategories();
    }

    @GetMapping("/general-courses")
    public List<AcademyCourseResponse> listGeneralCourses() {
        return academyService.listGeneralCourses();
    }

    @GetMapping("/general-courses/{id}")
    public AcademyCourseResponse getGeneralCourse(@PathVariable String id) {
        return academyService.getGeneralCourse(id);
    }

    @GetMapping("/general-courses/categories")
    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyService.listGeneralCourseCategories();
    }

    @GetMapping("/micro-major-courses")
    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return academyService.listMicroMajorCourses();
    }

    @GetMapping("/micro-major-courses/{id}")
    public AcademyCourseResponse getMicroMajorCourse(@PathVariable String id) {
        return academyService.getMicroMajorCourse(id);
    }

    @GetMapping("/micro-major-courses/categories")
    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyService.listMicroMajorCourseCategories();
    }

    @GetMapping("/textbooks")
    public List<AcademyTextbookResponse> listTextbooks() {
        return academyService.listTextbooks();
    }

    @GetMapping("/textbooks/categories")
    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyService.listTextbookCategories();
    }
}
