package com.cupk.academy.controller;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyAssignmentAnswerRequest;
import com.cupk.academy.dto.AcademyAssignmentDetailResponse;
import com.cupk.academy.dto.AcademyAssignmentSubmitResponse;
import com.cupk.academy.dto.AcademyAssignmentSummaryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentRequest;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyExamAnswerRequest;
import com.cupk.academy.dto.AcademyExamDetailResponse;
import com.cupk.academy.dto.AcademyExamSubmitResponse;
import com.cupk.academy.dto.AcademyExamSummaryResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.service.AcademyAssignmentService;
import com.cupk.academy.service.AcademyExamService;
import com.cupk.academy.service.AcademyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService academyService;
    private final AcademyAssignmentService assignmentService;
    private final AcademyExamService examService;

    public AcademyController(
            AcademyService academyService,
            AcademyAssignmentService assignmentService,
            AcademyExamService examService
    ) {
        this.academyService = academyService;
        this.assignmentService = assignmentService;
        this.examService = examService;
    }

    @GetMapping("/home")
    public List<AcademyHomeSectionResponse> getAcademyHome() {
        return academyService.getAcademyHome();
    }

    @GetMapping("/my-courses")
    public List<AcademyEnrolledCourseResponse> listMyCourses(
            @RequestParam(required = false) Long userId
    ) {
        return academyService.listMyCourses(userId);
    }

    @GetMapping("/assignments")
    public List<AcademyAssignmentSummaryResponse> listAssignments(
            @RequestParam(required = false) Long userId
    ) {
        return assignmentService.listAssignments(userId);
    }

    @GetMapping("/assignments/{assignmentCode}")
    public AcademyAssignmentDetailResponse getAssignment(
            @PathVariable String assignmentCode,
            @RequestParam(required = false) Long userId
    ) {
        return assignmentService.getAssignment(assignmentCode, userId);
    }

    @PostMapping("/assignments/{assignmentCode}/draft")
    public AcademyAssignmentSubmitResponse saveAssignmentDraft(
            @PathVariable String assignmentCode,
            @RequestBody(required = false) AcademyAssignmentAnswerRequest request
    ) {
        return assignmentService.saveDraft(assignmentCode, request);
    }

    @PostMapping("/assignments/{assignmentCode}/submit")
    public AcademyAssignmentSubmitResponse submitAssignment(
            @PathVariable String assignmentCode,
            @RequestBody(required = false) AcademyAssignmentAnswerRequest request
    ) {
        return assignmentService.submitAssignment(assignmentCode, request);
    }

    @GetMapping("/exams")
    public List<AcademyExamSummaryResponse> listExams(
            @RequestParam(required = false) Long userId
    ) {
        return examService.listExams(userId);
    }

    @GetMapping("/exams/{examCode}")
    public AcademyExamDetailResponse getExam(
            @PathVariable String examCode,
            @RequestParam(required = false) Long userId
    ) {
        return examService.getExam(examCode, userId);
    }

    @PostMapping("/exams/{examCode}/start")
    public AcademyExamDetailResponse startExam(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.startExam(examCode, request == null ? null : request.userId());
    }

    @PostMapping("/exams/{examCode}/draft")
    public AcademyExamSubmitResponse saveExamDraft(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.saveDraft(examCode, request);
    }

    @PostMapping("/exams/{examCode}/submit")
    public AcademyExamSubmitResponse submitExam(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.submitExam(examCode, request);
    }

    @GetMapping("/online-open-courses")
    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return academyService.listOnlineOpenCourses();
    }

    @GetMapping("/online-open-courses/teacher/mine")
    public List<AcademyCourseResponse> listMyPublishedOnlineOpenCourses(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return academyService.listMyPublishedOnlineOpenCourses(userId);
    }

    @GetMapping("/online-open-courses/{id}")
    public AcademyCourseResponse getOnlineOpenCourse(@PathVariable String id) {
        return academyService.getOnlineOpenCourse(id);
    }

    @PostMapping(value = "/online-open-courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AcademyCourseResponse publishOnlineOpenCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestParam String courseName,
            @RequestParam String startTime,
            @RequestParam String semesterPlan,
            @RequestParam String courseDetail,
            @RequestParam String courseOverview,
            @RequestParam("cover") MultipartFile cover,
            @RequestParam("video") MultipartFile video
    ) {
        return academyService.publishOnlineOpenCourse(
                userId,
                courseName,
                startTime,
                semesterPlan,
                courseDetail,
                courseOverview,
                cover,
                video
        );
    }

    @DeleteMapping("/online-open-courses/{id}")
    public AcademyCourseEnrollmentResponse deletePublishedOnlineOpenCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @PathVariable String id
    ) {
        return academyService.deletePublishedOnlineOpenCourse(userId, id);
    }

    @PostMapping("/online-open-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollOnlineOpenCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("online-open-courses", id, request == null ? null : request.userId());
    }

    @DeleteMapping("/online-open-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollOnlineOpenCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("online-open-courses", id, userId);
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

    @PostMapping("/general-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollGeneralCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("general-courses", id, request == null ? null : request.userId());
    }

    @DeleteMapping("/general-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollGeneralCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("general-courses", id, userId);
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

    @PostMapping("/micro-major-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollMicroMajorCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("micro-major-courses", id, request == null ? null : request.userId());
    }

    @DeleteMapping("/micro-major-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollMicroMajorCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("micro-major-courses", id, userId);
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
