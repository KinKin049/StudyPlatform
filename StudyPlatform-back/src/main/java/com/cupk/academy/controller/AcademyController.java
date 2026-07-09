package com.cupk.academy.controller;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyAssignmentAnswerRequest;
import com.cupk.academy.dto.AcademyAssignmentDetailResponse;
import com.cupk.academy.dto.AcademyAssignmentSubmitResponse;
import com.cupk.academy.dto.AcademyAssignmentSummaryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentRequest;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReplyRequest;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyExamAnswerRequest;
import com.cupk.academy.dto.AcademyExamDetailResponse;
import com.cupk.academy.dto.AcademyExamSubmitResponse;
import com.cupk.academy.dto.AcademyExamSummaryResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookCartItemResponse;
import com.cupk.academy.dto.AcademyTextbookCartRequest;
import com.cupk.academy.dto.AcademyTextbookDetailResponse;
import com.cupk.academy.dto.AcademyTextbookOrderRequest;
import com.cupk.academy.dto.AcademyTextbookOrderResponse;
import com.cupk.academy.dto.AcademyTextbookCommentResponse;
import com.cupk.academy.dto.AcademyTextbookReviewRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学习中心控制器，提供课程浏览、作业管理、考试管理、教材商城等学习相关端点。
 */
@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService academyService;
    private final AcademyAssignmentService assignmentService;
    private final AcademyExamService examService;

    /**
     * 构造函数，注入学习服务组件。
     *
     * @param academyService 学习服务
     * @param assignmentService 作业服务
     * @param examService 考试服务
     */
    public AcademyController(
            AcademyService academyService,
            AcademyAssignmentService assignmentService,
            AcademyExamService examService
    ) {
        this.academyService = academyService;
        this.assignmentService = assignmentService;
        this.examService = examService;
    }

    /**
     * 获取学习中心首页数据。
     *
     * @return 首页分区数据列表
     */
    @GetMapping("/home")
    public List<AcademyHomeSectionResponse> getAcademyHome() {
        return academyService.getAcademyHome();
    }

    /**
     * 获取用户已参加的课程列表。
     *
     * @param userId 用户ID
     * @return 已参加课程列表
     */
    @GetMapping("/my-courses")
    public List<AcademyEnrolledCourseResponse> listMyCourses(
            @RequestParam(required = false) Long userId
    ) {
        return academyService.listMyCourses(userId);
    }

    /**
     * 获取作业列表。
     *
     * @param userId 用户ID
     * @return 作业列表
     */
    @GetMapping("/assignments")
    public List<AcademyAssignmentSummaryResponse> listAssignments(
            @RequestParam(required = false) Long userId
    ) {
        return assignmentService.listAssignments(userId);
    }

    /**
     * 获取作业详情。
     *
     * @param assignmentCode 作业编号
     * @param userId 用户ID
     * @return 作业详情
     */
    @GetMapping("/assignments/{assignmentCode}")
    public AcademyAssignmentDetailResponse getAssignment(
            @PathVariable String assignmentCode,
            @RequestParam(required = false) Long userId
    ) {
        return assignmentService.getAssignment(assignmentCode, userId);
    }

    /**
     * 保存作业草稿。
     *
     * @param assignmentCode 作业编号
     * @param request 作业答案请求
     * @return 作业提交响应
     */
    @PostMapping("/assignments/{assignmentCode}/draft")
    public AcademyAssignmentSubmitResponse saveAssignmentDraft(
            @PathVariable String assignmentCode,
            @RequestBody(required = false) AcademyAssignmentAnswerRequest request
    ) {
        return assignmentService.saveDraft(assignmentCode, request);
    }

    /**
     * 提交作业。
     *
     * @param assignmentCode 作业编号
     * @param request 作业答案请求
     * @return 作业提交响应
     */
    @PostMapping("/assignments/{assignmentCode}/submit")
    public AcademyAssignmentSubmitResponse submitAssignment(
            @PathVariable String assignmentCode,
            @RequestBody(required = false) AcademyAssignmentAnswerRequest request
    ) {
        return assignmentService.submitAssignment(assignmentCode, request);
    }

    /**
     * 获取考试列表。
     *
     * @param userId 用户ID
     * @return 考试列表
     */
    @GetMapping("/exams")
    public List<AcademyExamSummaryResponse> listExams(
            @RequestParam(required = false) Long userId
    ) {
        return examService.listExams(userId);
    }

    /**
     * 获取考试详情。
     *
     * @param examCode 考试编号
     * @param userId 用户ID
     * @return 考试详情
     */
    @GetMapping("/exams/{examCode}")
    public AcademyExamDetailResponse getExam(
            @PathVariable String examCode,
            @RequestParam(required = false) Long userId
    ) {
        return examService.getExam(examCode, userId);
    }

    /**
     * 开始考试。
     *
     * @param examCode 考试编号
     * @param request 考试答案请求
     * @return 考试详情
     */
    @PostMapping("/exams/{examCode}/start")
    public AcademyExamDetailResponse startExam(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.startExam(examCode, request == null ? null : request.userId());
    }

    /**
     * 保存考试草稿。
     *
     * @param examCode 考试编号
     * @param request 考试答案请求
     * @return 考试提交响应
     */
    @PostMapping("/exams/{examCode}/draft")
    public AcademyExamSubmitResponse saveExamDraft(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.saveDraft(examCode, request);
    }

    /**
     * 提交考试。
     *
     * @param examCode 考试编号
     * @param request 考试答案请求
     * @return 考试提交响应
     */
    @PostMapping("/exams/{examCode}/submit")
    public AcademyExamSubmitResponse submitExam(
            @PathVariable String examCode,
            @RequestBody(required = false) AcademyExamAnswerRequest request
    ) {
        return examService.submitExam(examCode, request);
    }

    /**
     * 获取在线开放课程列表。
     *
     * @return 在线开放课程列表
     */
    @GetMapping("/online-open-courses")
    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return academyService.listOnlineOpenCourses();
    }

    /**
     * 获取教师已发布的在线开放课程列表。
     *
     * @param userId 用户ID
     * @return 教师已发布课程列表
     */
    @GetMapping("/online-open-courses/teacher/mine")
    public List<AcademyCourseResponse> listMyPublishedOnlineOpenCourses(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return academyService.listMyPublishedOnlineOpenCourses(userId);
    }

    /**
     * 获取在线开放课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    @GetMapping("/online-open-courses/{id}")
    public AcademyCourseResponse getOnlineOpenCourse(@PathVariable String id) {
        return academyService.getOnlineOpenCourse(id);
    }

    /**
     * 发布在线开放课程。
     *
     * @param userId 用户ID
     * @param courseName 课程名称
     * @param startTime 开课时间
     * @param category 课程分类
     * @param semesterPlan 学期计划
     * @param courseDetail 课程详情
     * @param courseOverview 课程概述
     * @param cover 课程封面
     * @param video 课程视频
     * @return 课程响应
     */
    @PostMapping(value = "/online-open-courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AcademyCourseResponse publishOnlineOpenCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestParam String courseName,
            @RequestParam String startTime,
            @RequestParam String category,
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
                category,
                semesterPlan,
                courseDetail,
                courseOverview,
                cover,
                video
        );
    }

    /**
     * 删除教师已发布的在线开放课程。
     *
     * @param userId 用户ID
     * @param id 课程ID
     * @return 课程报名响应
     */
    @DeleteMapping("/online-open-courses/{id}")
    public AcademyCourseEnrollmentResponse deletePublishedOnlineOpenCourse(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @PathVariable String id
    ) {
        return academyService.deletePublishedOnlineOpenCourse(userId, id);
    }

    /**
     * 报名参加在线开放课程。
     *
     * @param id 课程ID
     * @param request 课程报名请求
     * @return 课程报名响应
     */
    @PostMapping("/online-open-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollOnlineOpenCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("online-open-courses", id, request == null ? null : request.userId());
    }

    /**
     * 退出在线开放课程。
     *
     * @param id 课程ID
     * @param userId 用户ID
     * @return 课程报名响应
     */
    @DeleteMapping("/online-open-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollOnlineOpenCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("online-open-courses", id, userId);
    }

    /**
     * 获取在线开放课程评论列表。
     *
     * @param id 课程ID
     * @return 评论列表
     */
    @GetMapping("/online-open-courses/{id}/reviews")
    public List<AcademyCourseReviewResponse> listOnlineOpenCourseReviews(@PathVariable String id) {
        return academyService.listCourseReviews("online-open-courses", id);
    }

    /**
     * 保存在线开放课程评论。
     *
     * @param id 课程ID
     * @param request 评论请求
     * @return 评论响应
     */
    @PostMapping("/online-open-courses/{id}/reviews")
    public AcademyCourseReviewResponse saveOnlineOpenCourseReview(
            @PathVariable String id,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @Valid @RequestBody AcademyCourseReviewRequest request
    ) {
        return academyService.saveCourseReview("online-open-courses", id, userId, request);
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public AcademyCourseReviewResponse replyCourseReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @Valid @RequestBody AcademyCourseReplyRequest request
    ) {
        return academyService.replyCourseReview(userId, reviewId, request == null ? null : request.content());
    }

    /**
     * 获取在线开放课程分类列表。
     *
     * @return 分类列表
     */
    @GetMapping("/online-open-courses/categories")
    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyService.listOnlineOpenCourseCategories();
    }

    /**
     * 获取通识课程列表。
     *
     * @return 通识课程列表
     */
    @GetMapping("/general-courses")
    public List<AcademyCourseResponse> listGeneralCourses() {
        return academyService.listGeneralCourses();
    }

    /**
     * 获取通识课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    @GetMapping("/general-courses/{id}")
    public AcademyCourseResponse getGeneralCourse(@PathVariable String id) {
        return academyService.getGeneralCourse(id);
    }

    /**
     * 报名参加通识课程。
     *
     * @param id 课程ID
     * @param request 课程报名请求
     * @return 课程报名响应
     */
    @PostMapping("/general-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollGeneralCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("general-courses", id, request == null ? null : request.userId());
    }

    /**
     * 退出通识课程。
     *
     * @param id 课程ID
     * @param userId 用户ID
     * @return 课程报名响应
     */
    @DeleteMapping("/general-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollGeneralCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("general-courses", id, userId);
    }

    /**
     * 获取通识课程分类列表。
     *
     * @return 分类列表
     */
    @GetMapping("/general-courses/categories")
    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyService.listGeneralCourseCategories();
    }

    /**
     * 获取微专业课程列表。
     *
     * @return 微专业课程列表
     */
    @GetMapping("/micro-major-courses")
    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return academyService.listMicroMajorCourses();
    }

    /**
     * 获取微专业课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    @GetMapping("/micro-major-courses/{id}")
    public AcademyCourseResponse getMicroMajorCourse(@PathVariable String id) {
        return academyService.getMicroMajorCourse(id);
    }

    /**
     * 报名参加微专业课程。
     *
     * @param id 课程ID
     * @param request 课程报名请求
     * @return 课程报名响应
     */
    @PostMapping("/micro-major-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse enrollMicroMajorCourse(
            @PathVariable String id,
            @RequestBody(required = false) AcademyCourseEnrollmentRequest request
    ) {
        return academyService.enrollCourse("micro-major-courses", id, request == null ? null : request.userId());
    }

    /**
     * 退出微专业课程。
     *
     * @param id 课程ID
     * @param userId 用户ID
     * @return 课程报名响应
     */
    @DeleteMapping("/micro-major-courses/{id}/enroll")
    public AcademyCourseEnrollmentResponse unenrollMicroMajorCourse(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.unenrollCourse("micro-major-courses", id, userId);
    }

    /**
     * 获取微专业课程分类列表。
     *
     * @return 分类列表
     */
    @GetMapping("/micro-major-courses/categories")
    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyService.listMicroMajorCourseCategories();
    }

    /**
     * 获取教材列表。
     *
     * @return 教材列表
     */
    @GetMapping("/textbooks")
    public List<AcademyTextbookResponse> listTextbooks() {
        return academyService.listTextbooks();
    }

    /**
     * 获取教材详情。
     *
     * @param id 教材ID
     * @param userId 用户ID
     * @return 教材详情
     */
    @GetMapping("/textbooks/{id}")
    public AcademyTextbookDetailResponse getTextbook(
            @PathVariable String id,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.getTextbook(id, userId);
    }

    /**
     * 获取教材购物车列表。
     *
     * @param userId 用户ID
     * @return 购物车列表
     */
    @GetMapping("/textbook-cart")
    public List<AcademyTextbookCartItemResponse> listTextbookCart(
            @RequestParam(required = false) Long userId
    ) {
        return academyService.listTextbookCart(userId);
    }

    /**
     * 添加教材到购物车。
     *
     * @param request 购物车请求
     * @return 更新后的购物车列表
     */
    @PostMapping("/textbook-cart")
    public List<AcademyTextbookCartItemResponse> addTextbookCartItem(
            @RequestBody(required = false) AcademyTextbookCartRequest request
    ) {
        return academyService.addTextbookCartItem(request);
    }

    /**
     * 删除购物车中的教材。
     *
     * @param itemId 购物车条目ID
     * @param userId 用户ID
     * @return 更新后的购物车列表
     */
    @DeleteMapping("/textbook-cart/{itemId}")
    public List<AcademyTextbookCartItemResponse> deleteTextbookCartItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.deleteTextbookCartItem(userId, itemId);
    }

    /**
     * 更新购物车中教材的数量。
     *
     * @param itemId 购物车条目ID
     * @param request 购物车请求
     * @return 更新后的购物车列表
     */
    @PutMapping("/textbook-cart/{itemId}")
    public List<AcademyTextbookCartItemResponse> updateTextbookCartItem(
            @PathVariable Long itemId,
            @RequestBody(required = false) AcademyTextbookCartRequest request
    ) {
        return academyService.updateTextbookCartItem(request == null ? null : request.userId(), itemId, request == null ? null : request.quantity());
    }

    /**
     * 创建教材订单。
     *
     * @param request 订单请求
     * @return 订单响应
     */
    @PostMapping("/textbook-orders")
    public AcademyTextbookOrderResponse createTextbookOrder(
            @RequestBody(required = false) AcademyTextbookOrderRequest request
    ) {
        return academyService.createTextbookOrder(request);
    }

    /**
     * 支付教材订单。
     *
     * @param orderNo 订单编号
     * @param userId 用户ID
     * @return 订单响应
     */
    @PostMapping("/textbook-orders/{orderNo}/pay")
    public AcademyTextbookOrderResponse payTextbookOrder(
            @PathVariable String orderNo,
            @RequestParam(required = false) Long userId
    ) {
        return academyService.payTextbookOrder(orderNo, userId);
    }

    /**
     * 保存教材评论。
     *
     * @param id 教材ID
     * @param request 评论请求
     * @return 评论响应
     */
    @PostMapping("/textbooks/{id}/reviews")
    public AcademyTextbookCommentResponse saveTextbookReview(
            @PathVariable String id,
            @RequestBody(required = false) AcademyTextbookReviewRequest request
    ) {
        return academyService.saveTextbookReview(id, request);
    }

    /**
     * 获取教材分类列表。
     *
     * @return 分类列表
     */
    @GetMapping("/textbooks/categories")
    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyService.listTextbookCategories();
    }
}
