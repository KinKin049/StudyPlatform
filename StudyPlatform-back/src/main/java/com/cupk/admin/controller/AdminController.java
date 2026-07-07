package com.cupk.admin.controller;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminUserUpdateRequest;
import com.cupk.admin.dto.AdminVoucherItemRequest;
import com.cupk.admin.service.AdminService;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
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

/**
 * 管理员控制器，提供用户管理、课程管理、题库管理和卡券管理等后台管理端点。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    /**
     * 构造函数，注入管理员服务。
     *
     * @param adminService 管理员服务
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 获取用户列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @return 用户列表
     */
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listUsers(currentUserId);
    }

    /**
     * 更新用户信息。
     *
     * @param currentUserId 当前登录管理员ID
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/users/{userId}")
    public AdminUserResponse updateUser(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long userId,
            @RequestBody AdminUserUpdateRequest request
    ) {
        return adminService.updateUser(currentUserId, userId, request);
    }

    /**
     * 删除用户。
     *
     * @param currentUserId 当前登录管理员ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/users/{userId}")
    public Map<String, Boolean> deleteUser(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long userId
    ) {
        adminService.deleteUser(currentUserId, userId);
        return Map.of("deleted", true);
    }

    /**
     * 获取课程列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @param resourceType 课程类型
     * @return 课程列表
     */
    @GetMapping("/courses")
    public List<AdminCourseResponse> listCourses(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestParam(defaultValue = "online-open-courses") String resourceType
    ) {
        return adminService.listCourses(currentUserId, resourceType);
    }

    /**
     * 保存课程信息（新增或更新）。
     *
     * @param currentUserId 当前登录管理员ID
     * @param request 课程请求
     * @return 课程响应
     */
    @PostMapping("/courses")
    public AdminCourseResponse saveCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminCourseRequest request
    ) {
        return adminService.saveCourse(currentUserId, request);
    }

    /**
     * 删除课程。
     *
     * @param currentUserId 当前登录管理员ID
     * @param resourceType 课程类型
     * @param courseId 课程ID
     * @return 删除结果
     */
    @DeleteMapping("/courses/{resourceType}/{courseId}")
    public Map<String, Boolean> deleteCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable String resourceType,
            @PathVariable String courseId
    ) {
        adminService.deleteCourse(currentUserId, resourceType, courseId);
        return Map.of("deleted", true);
    }

    /**
     * 获取课程评论列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @return 评论列表
     */
    @GetMapping("/reviews")
    public List<AdminCourseReviewResponse> listReviews(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listReviews(currentUserId);
    }

    /**
     * 删除课程评论。
     *
     * @param currentUserId 当前登录管理员ID
     * @param reviewId 评论ID
     * @return 删除结果
     */
    @DeleteMapping("/reviews/{reviewId}")
    public Map<String, Boolean> deleteReview(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long reviewId
    ) {
        adminService.deleteReview(currentUserId, reviewId);
        return Map.of("deleted", true);
    }

    /**
     * 获取题库集合列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @return 题库集合列表
     */
    @GetMapping("/question-bank/sets")
    public List<CourseQuestionBankSetResponse> listQuestionBankSets(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listQuestionBankSets(currentUserId);
    }

    /**
     * 保存题库集合信息（新增或更新）。
     *
     * @param currentUserId 当前登录管理员ID
     * @param request 题库集合请求
     * @return 题库集合响应
     */
    @PostMapping("/question-bank/sets")
    public CourseQuestionBankSetResponse saveQuestionBankSet(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminQuestionBankSetRequest request
    ) {
        return adminService.saveQuestionBankSet(currentUserId, request);
    }

    /**
     * 删除题库集合。
     *
     * @param currentUserId 当前登录管理员ID
     * @param setCode 题库编号
     * @return 删除结果
     */
    @DeleteMapping("/question-bank/sets/{setCode}")
    public Map<String, Boolean> deleteQuestionBankSet(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable String setCode
    ) {
        adminService.deleteQuestionBankSet(currentUserId, setCode);
        return Map.of("deleted", true);
    }

    /**
     * 获取题目列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @param setCode 题库编号
     * @return 题目列表
     */
    @GetMapping("/question-bank/questions")
    public List<CourseQuestionBankQuestionResponse> listQuestions(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestParam String setCode
    ) {
        return adminService.listQuestions(currentUserId, setCode);
    }

    /**
     * 创建题目。
     *
     * @param currentUserId 当前登录管理员ID
     * @param request 题目请求
     * @return 创建的题目ID
     */
    @PostMapping("/question-bank/questions")
    public Map<String, Long> createQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminQuestionRequest request
    ) {
        return Map.of("id", adminService.saveQuestion(currentUserId, null, request));
    }

    /**
     * 更新题目。
     *
     * @param currentUserId 当前登录管理员ID
     * @param questionId 题目ID
     * @param request 题目请求
     * @return 更新后的题目ID
     */
    @PutMapping("/question-bank/questions/{questionId}")
    public Map<String, Long> updateQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long questionId,
            @RequestBody AdminQuestionRequest request
    ) {
        return Map.of("id", adminService.saveQuestion(currentUserId, questionId, request));
    }

    /**
     * 删除题目。
     *
     * @param currentUserId 当前登录管理员ID
     * @param questionId 题目ID
     * @return 删除结果
     */
    @DeleteMapping("/question-bank/questions/{questionId}")
    public Map<String, Boolean> deleteQuestion(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable long questionId
    ) {
        adminService.deleteQuestion(currentUserId, questionId);
        return Map.of("deleted", true);
    }

    /**
     * 获取卡券列表。
     *
     * @param currentUserId 当前登录管理员ID
     * @return 卡券列表
     */
    @GetMapping("/vouchers")
    public List<VoucherItemResponse> listVouchers(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId
    ) {
        return adminService.listVouchers(currentUserId);
    }

    /**
     * 保存卡券信息（新增或更新）。
     *
     * @param currentUserId 当前登录管理员ID
     * @param request 卡券请求
     * @return 卡券响应
     */
    @PostMapping("/vouchers")
    public VoucherItemResponse saveVoucher(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @RequestBody AdminVoucherItemRequest request
    ) {
        return adminService.saveVoucher(currentUserId, request);
    }

    /**
     * 删除卡券（禁用）。
     *
     * @param currentUserId 当前登录管理员ID
     * @param voucherKey 卡券编号
     * @return 删除结果
     */
    @DeleteMapping("/vouchers/{voucherKey}")
    public Map<String, Boolean> deleteVoucher(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long currentUserId,
            @PathVariable String voucherKey
    ) {
        adminService.deleteVoucher(currentUserId, voucherKey);
        return Map.of("deleted", true);
    }
}
