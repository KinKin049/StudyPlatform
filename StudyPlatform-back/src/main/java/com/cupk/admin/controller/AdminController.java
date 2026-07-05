package com.cupk.admin.controller;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminUserUpdateRequest;
import com.cupk.admin.service.AdminService;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> listUsers(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listUsers(currentUserId);
    }

    @PutMapping("/users/{userId}")
    public AdminUserResponse updateUser(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long userId,
            @RequestBody AdminUserUpdateRequest request
    ) {
        return adminService.updateUser(currentUserId, userId, request);
    }

    @DeleteMapping("/users/{userId}")
    public Map<String, Boolean> deleteUser(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long userId
    ) {
        adminService.deleteUser(currentUserId, userId);
        return Map.of("deleted", true);
    }

    @GetMapping("/courses")
    public List<AdminCourseResponse> listCourses(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestParam(defaultValue = "online-open-courses") String resourceType
    ) {
        return adminService.listCourses(currentUserId, resourceType);
    }

    @PostMapping("/courses")
    public AdminCourseResponse saveCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminCourseRequest request
    ) {
        return adminService.saveCourse(currentUserId, request);
    }

    @DeleteMapping("/courses/{resourceType}/{courseId}")
    public Map<String, Boolean> deleteCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable String resourceType,
            @PathVariable String courseId
    ) {
        adminService.deleteCourse(currentUserId, resourceType, courseId);
        return Map.of("deleted", true);
    }

    @GetMapping("/reviews")
    public List<AdminCourseReviewResponse> listReviews(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listReviews(currentUserId);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public Map<String, Boolean> deleteReview(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long reviewId
    ) {
        adminService.deleteReview(currentUserId, reviewId);
        return Map.of("deleted", true);
    }

    @GetMapping("/question-bank/sets")
    public List<CourseQuestionBankSetResponse> listQuestionBankSets(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listQuestionBankSets(currentUserId);
    }

    @PostMapping("/question-bank/sets")
    public CourseQuestionBankSetResponse saveQuestionBankSet(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminQuestionBankSetRequest request
    ) {
        return adminService.saveQuestionBankSet(currentUserId, request);
    }

    @DeleteMapping("/question-bank/sets/{setCode}")
    public Map<String, Boolean> deleteQuestionBankSet(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable String setCode
    ) {
        adminService.deleteQuestionBankSet(currentUserId, setCode);
        return Map.of("deleted", true);
    }

    @GetMapping("/question-bank/questions")
    public List<CourseQuestionBankQuestionResponse> listQuestions(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestParam String setCode
    ) {
        return adminService.listQuestions(currentUserId, setCode);
    }

    @PostMapping("/question-bank/questions")
    public Map<String, Long> createQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminQuestionRequest request
    ) {
        return Map.of("id", adminService.saveQuestion(currentUserId, null, request));
    }

    @PutMapping("/question-bank/questions/{questionId}")
    public Map<String, Long> updateQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long questionId,
            @RequestBody AdminQuestionRequest request
    ) {
        return Map.of("id", adminService.saveQuestion(currentUserId, questionId, request));
    }

    @DeleteMapping("/question-bank/questions/{questionId}")
    public Map<String, Boolean> deleteQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long questionId
    ) {
        adminService.deleteQuestion(currentUserId, questionId);
        return Map.of("deleted", true);
    }
}
