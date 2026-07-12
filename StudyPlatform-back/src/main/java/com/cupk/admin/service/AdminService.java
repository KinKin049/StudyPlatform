package com.cupk.admin.service;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminOjCaseCheckResponse;
import com.cupk.admin.dto.AdminOjCheckResponse;
import com.cupk.admin.dto.AdminOjProblemRequest;
import com.cupk.admin.dto.AdminOjProblemResponse;
import com.cupk.admin.dto.AdminOjTestCaseRequest;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminUserUpdateRequest;
import com.cupk.admin.dto.AdminVoucherItemRequest;
import com.cupk.admin.repository.AdminRepository;
import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.service.AcademyService;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.oj.dto.JudgeCaseResult;
import com.cupk.oj.dto.JudgeResult;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjTestCase;
import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import com.cupk.oj.model.SubmissionStatus;
import com.cupk.oj.service.JudgeSandboxClient;
import com.cupk.rewards.VoucherCatalog;
import com.cupk.rewards.dto.VoucherItemResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 管理员服务，提供用户管理、课程管理、题库管理和卡券管理等后台业务逻辑。
 */
@Service
public class AdminService {
    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_ROLE = "admin";

    private final AdminRepository adminRepository;
    private final AcademyService academyService;
    private final PasswordEncoder passwordEncoder;
    private final JudgeSandboxClient judgeSandboxClient;

    /**
     * 构造函数，注入依赖组件。
     *
     * @param adminRepository 管理员数据访问层
     * @param passwordEncoder 密码编码器
     */
    public AdminService(
            AdminRepository adminRepository,
            AcademyService academyService,
            PasswordEncoder passwordEncoder,
            JudgeSandboxClient judgeSandboxClient
    ) {
        this.adminRepository = adminRepository;
        this.academyService = academyService;
        this.passwordEncoder = passwordEncoder;
        this.judgeSandboxClient = judgeSandboxClient;
    }

    /**
     * 获取用户列表。验证管理员权限后查询所有用户。
     *
     * @param currentUserId 当前登录用户ID
     * @return 用户列表
     */
    public List<AdminUserResponse> listUsers(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findUsers();
    }

    /**
     * 更新用户信息。验证管理员权限，检查用户是否为管理员账号，更新用户数据。
     *
     * @param currentUserId 当前登录用户ID
     * @param userId 待更新用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    public AdminUserResponse updateUser(Long currentUserId, long userId, AdminUserUpdateRequest request) {
        ensureAdmin(currentUserId);
        AdminUserResponse current = adminRepository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (isAdminUser(current)) {
            String password = request == null || request.password() == null ? "" : request.password().trim();
            if (password.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员账号只能修改密码");
            }
            adminRepository.updateUserPassword(userId, passwordEncoder.encode(password));
            return adminRepository.findUser(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        }

        String username = required(request == null ? null : request.username(), "用户名", 64);
        String email = required(request == null ? null : request.email(), "邮箱", 128).toLowerCase(Locale.ROOT);
        if (ADMIN_EMAIL.equals(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员邮箱只能属于固定管理员账号");
        }
        if (adminRepository.emailBelongsToOtherUser(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被其他用户使用");
        }

        String roleType = clean(request.roleType(), current.roleType(), 24).toLowerCase(Locale.ROOT);
        if (!"student".equals(roleType) && !"teacher".equals(roleType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户权限只能设置为学生或教师");
        }

        String password = request.password() == null ? "" : request.password().trim();
        String passwordHash = password.isBlank() ? null : passwordEncoder.encode(password);
        long coinAdjustment = resolveCoinAdjustment(current, request);
        adminRepository.updateUser(
                userId,
                username,
                email,
                roleType,
                clean(request.learningGoal(), "", 255),
                clean(request.school(), "", 128),
                clean(request.teacherName(), "", 64),
                coinAdjustment,
                clean(request.dataNote(), "", 512),
                passwordHash
        );
        return adminRepository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    /**
     * 解析金币调整值。支持设置金币总数或直接调整金币数量。
     *
     * @param current 当前用户信息
     * @param request 更新请求
     * @return 金币调整值
     */
    private long resolveCoinAdjustment(AdminUserResponse current, AdminUserUpdateRequest request) {
        if (request.coinTotal() == null) {
            return request.coinAdjustment() == null ? current.coinAdjustment() : request.coinAdjustment();
        }
        long targetCoinTotal = request.coinTotal();
        if (targetCoinTotal < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "金币总数不能小于 0");
        }
        long coinBase = current.coinTotal() - current.coinAdjustment();
        return targetCoinTotal - coinBase;
    }

    /**
     * 删除用户。验证管理员权限，禁止删除管理员账号。
     *
     * @param currentUserId 当前登录用户ID
     * @param userId 待删除用户ID
     */
    public void deleteUser(Long currentUserId, long userId) {
        ensureAdmin(currentUserId);
        AdminUserResponse user = adminRepository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (isAdminUser(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员账号不能删除");
        }
        adminRepository.deleteUser(userId);
    }

    /**
     * 获取课程列表。验证管理员权限后按课程类型查询。
     *
     * @param currentUserId 当前登录用户ID
     * @param resourceType 课程类型
     * @return 课程列表
     */
    public List<AdminCourseResponse> listCourses(Long currentUserId, String resourceType) {
        ensureAdmin(currentUserId);
        return adminRepository.findCourses(normalizeResourceType(resourceType));
    }

    /**
     * 保存课程信息（新增或更新）。验证管理员权限，标准化课程请求后保存。
     *
     * @param currentUserId 当前登录用户ID
     * @param request 课程请求
     * @return 课程响应
     */
    public AdminCourseResponse saveCourse(Long currentUserId, AdminCourseRequest request) {
        ensureAdmin(currentUserId);
        AdminCourseRequest safeRequest = normalizeCourseRequest(request);
        adminRepository.upsertCourse(safeRequest);
        return adminRepository.findCourse(safeRequest.resourceType(), safeRequest.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程保存失败"));
    }

    public List<AcademyCategoryResponse> listCourseCategories(Long currentUserId, String resourceType) {
        ensureAdmin(currentUserId);
        return adminRepository.findCourseCategories(normalizeResourceType(resourceType));
    }

    public List<AcademyCategoryResponse> saveCourseCategory(Long currentUserId, String resourceType, String name) {
        ensureAdmin(currentUserId);
        String safeResourceType = normalizeResourceType(resourceType);
        String safeName = required(name, "分类名称", 80);
        adminRepository.upsertCourseCategory(safeResourceType, safeName);
        return adminRepository.findCourseCategories(safeResourceType);
    }

    public List<AcademyCategoryResponse> deleteCourseCategory(Long currentUserId, String resourceType, String name) {
        ensureAdmin(currentUserId);
        String safeResourceType = normalizeResourceType(resourceType);
        String safeName = required(name, "分类名称", 80);
        int deleted = adminRepository.deleteCourseCategory(safeResourceType, safeName);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        return adminRepository.findCourseCategories(safeResourceType);
    }

    public List<AcademyCategoryResponse> listOjCategories(Long currentUserId) {
        requireOjEditor(currentUserId);
        return adminRepository.findOjCategories();
    }

    public List<AcademyCategoryResponse> saveOjCategory(Long currentUserId, String name) {
        ensureAdmin(currentUserId);
        String safeName = required(name, "OJ分类名称", 80);
        adminRepository.upsertOjCategory(safeName);
        return adminRepository.findOjCategories();
    }

    public List<AcademyCategoryResponse> deleteOjCategory(Long currentUserId, String name) {
        ensureAdmin(currentUserId);
        String safeName = required(name, "OJ分类名称", 80);
        int deleted = adminRepository.deleteOjCategory(safeName);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OJ分类不存在");
        }
        return adminRepository.findOjCategories();
    }

    /**
     * 删除课程。验证管理员权限后删除指定课程。
     *
     * @param currentUserId 当前登录用户ID
     * @param resourceType 课程类型
     * @param courseId 课程ID
     */
    public void deleteCourse(Long currentUserId, String resourceType, String courseId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteCourse(normalizeResourceType(resourceType), required(courseId, "课程编号", 128));
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
    }

    /**
     * 获取课程评论列表。验证管理员权限后查询所有评论。
     *
     * @param currentUserId 当前登录用户ID
     * @return 评论列表
     */
    public List<AdminCourseReviewResponse> listReviews(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findReviews();
    }

    /**
     * 删除课程评论。验证管理员权限后删除指定评论。
     *
     * @param currentUserId 当前登录用户ID
     * @param reviewId 评论ID
     */
    public void deleteReview(Long currentUserId, String reviewType, long reviewId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteReview(normalizeReviewType(reviewType), reviewId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
    }

    public AdminCourseReviewResponse replyReview(Long currentUserId, String reviewType, long reviewId, String content) {
        AdminRepository.AdminAuthRow admin = requireAdmin(currentUserId);
        String safeType = normalizeReviewType(reviewType);
        String safeContent = clean(content, "", 2000);
        if (safeContent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        }
        if ("course".equals(safeType)) {
            academyService.replyCourseReview(currentUserId, reviewId, safeContent);
            return adminRepository.findReviews().stream()
                    .filter(item -> safeType.equals(item.reviewType()) && item.id() == reviewId)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
        }
        int updated = adminRepository.replyReview(safeType, reviewId, admin, safeContent);
        if (updated <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        return adminRepository.findReviews().stream()
                .filter(item -> safeType.equals(item.reviewType()) && item.id() == reviewId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
    }

    public AdminCourseReviewResponse clearReviewReply(Long currentUserId, String reviewType, long reviewId) {
        ensureAdmin(currentUserId);
        String safeType = normalizeReviewType(reviewType);
        int updated = adminRepository.clearReviewReply(safeType, reviewId);
        if (updated <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        return adminRepository.findReviews().stream()
                .filter(item -> safeType.equals(item.reviewType()) && item.id() == reviewId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
    }

    /**
     * 获取题库集合列表。验证管理员权限后查询所有题库集合。
     *
     * @param currentUserId 当前登录用户ID
     * @return 题库集合列表
     */
    public List<CourseQuestionBankSetResponse> listQuestionBankSets(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findQuestionBankSets();
    }

    /**
     * 保存题库集合信息（新增或更新）。验证管理员权限，标准化请求后保存。
     *
     * @param currentUserId 当前登录用户ID
     * @param request 题库集合请求
     * @return 题库集合响应
     */
    public CourseQuestionBankSetResponse saveQuestionBankSet(Long currentUserId, AdminQuestionBankSetRequest request) {
        ensureAdmin(currentUserId);
        AdminQuestionBankSetRequest safeRequest = normalizeQuestionBankSetRequest(request);
        adminRepository.upsertQuestionBankSet(safeRequest);
        return adminRepository.findQuestionBankSets().stream()
                .filter(set -> set.code().equals(safeRequest.code()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题库保存失败"));
    }

    /**
     * 删除题库集合。验证管理员权限后删除指定题库集合。
     *
     * @param currentUserId 当前登录用户ID
     * @param setCode 题库编号
     */
    public void deleteQuestionBankSet(Long currentUserId, String setCode) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteQuestionBankSet(required(setCode, "题库编号", 64));
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "题库不存在");
        }
    }

    /**
     * 获取题目列表。验证管理员权限后按题库编号查询题目。
     *
     * @param currentUserId 当前登录用户ID
     * @param setCode 题库编号
     * @return 题目列表
     */
    public List<CourseQuestionBankQuestionResponse> listQuestions(Long currentUserId, String setCode) {
        ensureAdmin(currentUserId);
        return adminRepository.findQuestions(required(setCode, "题库编号", 64));
    }

    /**
     * 保存题目信息（新增或更新）。验证管理员权限，标准化请求后保存。
     *
     * @param currentUserId 当前登录用户ID
     * @param questionId 题目ID，为空表示新增
     * @param request 题目请求
     * @return 题目ID
     */
    public long saveQuestion(Long currentUserId, Long questionId, AdminQuestionRequest request) {
        ensureAdmin(currentUserId);
        AdminQuestionRequest safeRequest = normalizeQuestionRequest(request);
        return adminRepository.upsertQuestion(questionId, safeRequest);
    }

    /**
     * 删除题目。验证管理员权限后删除指定题目。
     *
     * @param currentUserId 当前登录用户ID
     * @param questionId 题目ID
     */
    public void deleteQuestion(Long currentUserId, long questionId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteQuestion(questionId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在");
        }
    }

    /**
     * 获取卡券列表。验证管理员权限后查询所有卡券。
     *
     * @param currentUserId 当前登录用户ID
     * @return 卡券列表
     */
    public List<AdminOjProblemResponse> listOjProblems(Long currentUserId) {
        AdminRepository.AdminAuthRow editor = requireOjEditor(currentUserId);
        return isAdminAuth(editor) ? adminRepository.findOjProblems() : adminRepository.findOjProblemsByOwner(editor.id());
    }

    public AdminOjProblemResponse getOjProblem(Long currentUserId, long problemId) {
        AdminRepository.AdminAuthRow editor = requireOjEditor(currentUserId);
        AdminOjProblemResponse problem = adminRepository.findOjProblem(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OJ题目不存在"));
        ensureOjProblemEditable(editor, problem);
        return problem;
    }

    @Transactional
    public AdminOjProblemResponse saveOjProblem(Long currentUserId, Long problemId, AdminOjProblemRequest request) {
        AdminRepository.AdminAuthRow editor = requireOjEditor(currentUserId);
        if (problemId != null) {
            AdminOjProblemResponse existing = adminRepository.findOjProblem(problemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OJ题目不存在"));
            ensureOjProblemEditable(editor, existing);
        }
        AdminOjProblemRequest safeRequest = normalizeOjProblemRequest(problemId, request);
        long savedId = adminRepository.upsertOjProblem(problemId, safeRequest, editor.id());
        return getOjProblem(currentUserId, savedId);
    }

    public void deleteOjProblem(Long currentUserId, long problemId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteOjProblem(problemId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OJ题目不存在");
        }
    }

    public AdminOjCheckResponse checkOjProblem(Long currentUserId, AdminOjProblemRequest request) {
        requireOjEditor(currentUserId);
        AdminOjProblemRequest safeRequest = normalizeOjProblemRequest(request == null ? null : request.id(), request);
        if (safeRequest.standardCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先填写标准代码再校验");
        }
        return checkOjProblemForNormalizedRequest(safeRequest);
    }

    public List<VoucherItemResponse> listVouchers(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findVoucherItems();
    }

    /**
     * 保存卡券信息（新增或更新）。验证管理员权限，标准化请求后保存。
     *
     * @param currentUserId 当前登录用户ID
     * @param request 卡券请求
     * @return 卡券响应
     */
    public VoucherItemResponse saveVoucher(Long currentUserId, AdminVoucherItemRequest request) {
        ensureAdmin(currentUserId);
        AdminVoucherItemRequest safeRequest = normalizeVoucherRequest(request);
        adminRepository.upsertVoucherItem(safeRequest);
        return adminRepository.findVoucherItem(safeRequest.voucherKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卡券保存失败"));
    }

    /**
     * 删除卡券（禁用）。验证管理员权限后禁用指定卡券。
     *
     * @param currentUserId 当前登录用户ID
     * @param voucherKey 卡券编号
     */
    public void deleteVoucher(Long currentUserId, String voucherKey) {
        ensureAdmin(currentUserId);
        int updated = adminRepository.disableVoucherItem(VoucherCatalog.normalize(required(voucherKey, "卡券编号", 64)));
        if (updated <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "卡券不存在");
        }
    }

    /**
     * 验证管理员权限。检查当前用户是否为管理员账号。
     *
     * @param currentUserId 当前登录用户ID
     */
    private void ensureAdmin(Long currentUserId) {
        requireAdmin(currentUserId);
    }

    private AdminRepository.AdminAuthRow requireAdmin(Long currentUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录管理员账号");
        }
        AdminRepository.AdminAuthRow user = adminRepository.findAuthRow(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录管理员账号"));
        if (!ADMIN_EMAIL.equalsIgnoreCase(user.email()) || !ADMIN_ROLE.equalsIgnoreCase(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }

    private AdminRepository.AdminAuthRow requireOjEditor(Long currentUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        AdminRepository.AdminAuthRow user = adminRepository.findAuthRow(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
        if (!isAdminAuth(user) && !"teacher".equalsIgnoreCase(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理员或教师可以管理OJ题目");
        }
        return user;
    }

    private boolean isAdminAuth(AdminRepository.AdminAuthRow user) {
        return user != null && ADMIN_EMAIL.equalsIgnoreCase(user.email()) && ADMIN_ROLE.equalsIgnoreCase(user.roleType());
    }

    private void ensureOjProblemEditable(AdminRepository.AdminAuthRow editor, AdminOjProblemResponse problem) {
        if (isAdminAuth(editor)) {
            return;
        }
        if (problem.createdBy() == null || problem.createdBy() != editor.id()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能修改自己创建的OJ题目");
        }
    }

    private String normalizeReviewType(String reviewType) {
        String type = clean(reviewType, "", 32);
        if (!List.of("course", "textbook").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论类型不支持");
        }
        return type;
    }

    /**
     * 判断用户是否为管理员账号。
     *
     * @param user 用户信息
     * @return 是管理员返回true，否则返回false
     */
    private boolean isAdminUser(AdminUserResponse user) {
        return ADMIN_EMAIL.equalsIgnoreCase(user.email()) || ADMIN_ROLE.equalsIgnoreCase(user.roleType());
    }

    /**
     * 标准化课程请求参数。验证必填字段，清理字符串并设置默认值。
     *
     * @param request 课程请求
     * @return 标准化后的课程请求
     */
    private AdminCourseRequest normalizeCourseRequest(AdminCourseRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程信息不能为空");
        }
        String resourceType = normalizeResourceType(request.resourceType());
        String category = required(request.category(), "分类", 80);
        if (!adminRepository.courseCategoryExists(resourceType, category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程分类只能从管理员维护的分类中选择");
        }
        String certificationLabel = clean(request.certificationLabel(), "", 80);
        String coverFilePath = localResourcePath(request.coverFilePath(), "封面本地路径", 255);
        String videoFilePath = localResourcePath(request.videoFilePath(), "视频本地路径", 512);
        return new AdminCourseRequest(
                resourceType,
                required(request.id(), "课程编号", 128),
                required(request.name(), "课程名称", 120),
                clean(request.teacher(), "", 80),
                category,
                clean(request.school(), "", 120),
                "",
                coverFilePath,
                clean(request.startTime(), "", 64),
                request.participants() == null ? 0 : Math.max(0, request.participants()),
                clean(request.comment(), "", 512),
                clean(request.description(), "", 4000),
                clean(request.semesterPlan(), "", 512),
                clean(request.overview(), "", 1200),
                videoFilePath,
                clean(request.link(), "", 512),
                !certificationLabel.isBlank(),
                certificationLabel
        );
    }

    /**
     * 标准化题库集合请求参数。验证必填字段，清理字符串并设置默认值。
     *
     * @param request 题库集合请求
     * @return 标准化后的题库集合请求
     */
    private AdminQuestionBankSetRequest normalizeQuestionBankSetRequest(AdminQuestionBankSetRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库信息不能为空");
        }
        String code = required(request.code(), "题库编号", 64);
        String coverFilePath = localResourcePath(request.coverFilePath(), "题库封面本地路径", 255);
        return new AdminQuestionBankSetRequest(
                required(request.categoryCode(), "分类编号", 64),
                required(request.categoryName(), "分类名称", 64),
                clean(request.categoryDescription(), "", 512),
                code,
                required(request.title(), "题库名称", 128),
                clean(request.subtitle(), "", 128),
                clean(request.description(), "", 512),
                "",
                coverFilePath,
                clean(request.difficultyLabel(), "", 64),
                clean(request.statusLabel(), "", 64),
                clean(request.sourceName(), "", 128),
                clean(request.sourceUrl(), "", 512),
                request.sourceRefs() == null ? List.of() : request.sourceRefs(),
                clean(request.routePath(), "/academy/question-bank/courses/" + code, 255),
                request.sortOrder() == null ? 0 : request.sortOrder()
        );
    }

    private String localResourcePath(String value, String fieldName, int maxLength) {
        String path = clean(value, "", maxLength).replace("\\", "/");
        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "只能填写本地文件路径");
        }
        return path;
    }

    /**
     * 标准化卡券请求参数。验证卡券类型、折扣类型，清理字符串并设置默认值。
     *
     * @param request 卡券请求
     * @return 标准化后的卡券请求
     */
    private AdminVoucherItemRequest normalizeVoucherRequest(AdminVoucherItemRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卡券信息不能为空");
        }
        String voucherType = clean(request.voucherType(), "DISCOUNT", 32).toUpperCase(Locale.ROOT);
        if (!"DISCOUNT".equals(voucherType) && !"GAME_ITEM".equals(voucherType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卡券类型只能是优惠券或游戏券");
        }
        String discountType = clean(request.discountType(), "GAME_ITEM".equals(voucherType) ? "NONE" : "AMOUNT", 24).toUpperCase(Locale.ROOT);
        if (!List.of("NONE", "AMOUNT", "PERCENT").contains(discountType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "折扣类型不合法");
        }
        boolean unlimitedStock = Boolean.TRUE.equals(request.unlimitedStock());
        Integer stockQuantity = unlimitedStock ? null : Math.max(0, request.stockQuantity() == null ? 0 : request.stockQuantity());
        return new AdminVoucherItemRequest(
                VoucherCatalog.normalize(required(request.voucherKey(), "卡券编号", 64)),
                voucherType,
                required(request.name(), "卡券名称", 128),
                clean(request.description(), "", 255),
                Math.max(0, request.price() == null ? 0 : request.price()),
                stockQuantity,
                unlimitedStock,
                discountType,
                positiveMoney(request.thresholdAmount()),
                positiveMoney(request.discountAmount()),
                normalizeDiscountRate(request.discountRate()),
                positiveMoney(request.maxDiscountAmount()),
                request.validFrom(),
                request.validUntil(),
                !Boolean.FALSE.equals(request.enabled()),
                request.sortOrder() == null ? 0 : request.sortOrder()
        );
    }

    /**
     * 将金额转换为非负数。
     *
     * @param value 金额值
     * @return 非负金额值
     */
    private BigDecimal positiveMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.max(BigDecimal.ZERO);
    }

    /**
     * 标准化折扣率，限制在0到1之间。
     *
     * @param value 折扣率
     * @return 标准化后的折扣率
     */
    private BigDecimal normalizeDiscountRate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    /**
     * 标准化题目请求参数。验证题目类型，清理字符串并设置默认值。
     *
     * @param request 题目请求
     * @return 标准化后的题目请求
     */
    private AdminOjProblemRequest normalizeOjProblemRequest(Long problemId, AdminOjProblemRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OJ题目信息不能为空");
        }
        String difficulty = clean(request.difficulty(), "EASY", 16).toUpperCase(Locale.ROOT);
        if (!List.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OJ题目难度不支持");
        }
        String status = clean(request.status(), problemId == null ? "PUBLISHED" : "DRAFT", 16).toUpperCase(Locale.ROOT);
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OJ题目状态不支持");
        }
        String standardCode = clean(request.standardCode(), "", 200000);
        String category = required(request.category(), "OJ分类", 80);
        if (!adminRepository.ojCategoryExists(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OJ分类只能从在线OJ已有算法分类中选择");
        }
        String tags = normalizeOjTags(request.tags(), category);
        List<AdminOjTestCaseRequest> testCases = normalizeOjTestCases(request.testCases());
        boolean hasAnyManualOutput = testCases.stream().anyMatch(item -> !item.expectedOutput().isBlank());
        if (standardCode.isBlank() && !hasAnyManualOutput) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手动输出和标准代码至少填写一个");
        }
        if (standardCode.isBlank() && testCases.stream().anyMatch(item -> item.expectedOutput().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未填写标准代码时，每个测试点都需要手动输出");
        }
        if (!standardCode.isBlank() && testCases.stream().anyMatch(item -> item.expectedOutput().isBlank())) {
            testCases = fillMissingOutputsWithStandardCode(problemId, request, testCases, standardCode, difficulty, status);
        }
        return new AdminOjProblemRequest(
                problemId,
                required(request.title(), "OJ题目标题", 128),
                required(request.slug(), "OJ题目标识", 128),
                category,
                required(request.description(), "题目描述", 200000),
                clean(request.inputDescription(), "", 200000),
                clean(request.outputDescription(), "", 200000),
                standardCode,
                difficulty,
                Math.min(30000, Math.max(100, request.timeLimitMs() == null ? 1000 : request.timeLimitMs())),
                Math.min(1048576, Math.max(1024, request.memoryLimitKb() == null ? 262144 : request.memoryLimitKb())),
                tags,
                status,
                testCases
        );
    }

    private String normalizeOjTags(String rawTags, String category) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add(category);
        for (String item : clean(rawTags, "", 1000).split(",")) {
            String tag = item.trim();
            if (tag.isBlank()) {
                continue;
            }
            if (!adminRepository.ojCategoryExists(tag)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OJ算法标签只能从在线OJ已有分类中选择");
            }
            tags.add(tag);
        }
        return String.join(",", tags);
    }

    private List<AdminOjTestCaseRequest> normalizeOjTestCases(List<AdminOjTestCaseRequest> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少添加一个测试点");
        }
        List<AdminOjTestCaseRequest> normalized = new ArrayList<>();
        for (int index = 0; index < testCases.size(); index += 1) {
            AdminOjTestCaseRequest item = testCases.get(index);
            if (item == null) {
                continue;
            }
            String inputData = clean(item.inputData(), "", 200000);
            if (inputData.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "测试点输入不能为空");
            }
            normalized.add(new AdminOjTestCaseRequest(
                    item.id(),
                    inputData,
                    clean(item.expectedOutput(), "", 200000),
                    Boolean.TRUE.equals(item.sample()),
                    Math.max(1, item.weight() == null ? 1 : item.weight()),
                    item.sortOrder() == null ? index + 1 : item.sortOrder()
            ));
        }
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少添加一个测试点");
        }
        return normalized;
    }

    private List<AdminOjTestCaseRequest> fillMissingOutputsWithStandardCode(
            Long problemId,
            AdminOjProblemRequest request,
            List<AdminOjTestCaseRequest> testCases,
            String standardCode,
            String difficulty,
            String status
    ) {
        AdminOjCheckResponse checkResponse = checkOjProblemForNormalizedRequest(new AdminOjProblemRequest(
                problemId,
                request.title(),
                request.slug(),
                request.category(),
                request.description(),
                request.inputDescription(),
                request.outputDescription(),
                standardCode,
                difficulty,
                request.timeLimitMs(),
                request.memoryLimitKb(),
                request.tags(),
                status,
                testCases
        ));
        if (checkResponse.cases().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法根据标准代码生成测试点输出");
        }
        List<AdminOjTestCaseRequest> filled = new ArrayList<>();
        for (int index = 0; index < testCases.size(); index += 1) {
            AdminOjTestCaseRequest item = testCases.get(index);
            String generated = checkResponse.cases().size() > index ? checkResponse.cases().get(index).actualOutput() : "";
            filled.add(new AdminOjTestCaseRequest(
                    item.id(),
                    item.inputData(),
                    item.expectedOutput().isBlank() ? generated : item.expectedOutput(),
                    item.sample(),
                    item.weight(),
                    item.sortOrder()
            ));
        }
        return filled;
    }

    private AdminOjCheckResponse checkOjProblemForNormalizedRequest(AdminOjProblemRequest safeRequest) {
        List<OjTestCase> testCases = new ArrayList<>();
        for (int index = 0; index < safeRequest.testCases().size(); index += 1) {
            AdminOjTestCaseRequest item = safeRequest.testCases().get(index);
            testCases.add(new OjTestCase(
                    (long) index + 1,
                    safeRequest.id() == null ? 0L : safeRequest.id(),
                    item.inputData(),
                    item.expectedOutput(),
                    Boolean.TRUE.equals(item.sample()),
                    item.weight() == null ? 1 : item.weight(),
                    item.sortOrder() == null ? index + 1 : item.sortOrder(),
                    LocalDateTime.now()
            ));
        }
        OjProblem problem = new OjProblem(
                safeRequest.id() == null ? 0L : safeRequest.id(),
                safeRequest.title(),
                safeRequest.slug(),
                safeRequest.category(),
                safeRequest.description(),
                safeRequest.inputDescription(),
                safeRequest.outputDescription(),
                safeRequest.standardCode(),
                null,
                ProblemDifficulty.valueOf(safeRequest.difficulty()),
                safeRequest.timeLimitMs() == null ? 1000 : safeRequest.timeLimitMs(),
                safeRequest.memoryLimitKb() == null ? 262144 : safeRequest.memoryLimitKb(),
                safeRequest.tags(),
                ProblemStatus.valueOf(safeRequest.status()),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        OjSubmission submission = new OjSubmission(
                0L,
                problem.id(),
                null,
                "cpp",
                safeRequest.standardCode(),
                SubmissionStatus.PENDING,
                0,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        JudgeResult result = judgeSandboxClient.judge(problem, submission, testCases);
        int standardCodeLength = safeRequest.standardCode() == null ? 0 : safeRequest.standardCode().length();
        if (result == null || result.cases() == null || result.cases().isEmpty()) {
            String detail = result == null ? "" : clean(result.message(), "", 1000);
            String message = "标准代码未返回可用输出，已收到代码长度 " + standardCodeLength + "。";
            if (!detail.isBlank()) {
                message += "沙箱提示：" + detail;
            } else {
                message += "请检查 judge-sandbox 是否已启动。";
            }
            return new AdminOjCheckResponse(false, message, List.of());
        }
        List<AdminOjCaseCheckResponse> cases = new ArrayList<>();
        boolean passed = true;
        for (int index = 0; index < safeRequest.testCases().size(); index += 1) {
            AdminOjTestCaseRequest item = safeRequest.testCases().get(index);
            long caseNumber = index + 1L;
            JudgeCaseResult caseResult = findJudgeCaseResult(result.cases(), index, caseNumber);
            String actualOutput = caseResult == null || caseResult.actualOutput() == null
                    ? null
                    : clean(caseResult.actualOutput(), "", 200000);
            boolean hasManualOutput = !item.expectedOutput().isBlank();
            boolean hasActualOutput = actualOutput != null;
            boolean matched = hasActualOutput && (!hasManualOutput || normalizeOutput(item.expectedOutput()).equals(normalizeOutput(actualOutput)));
            if (!matched) {
                passed = false;
            }
            cases.add(new AdminOjCaseCheckResponse(
                    item.sortOrder(),
                    item.inputData(),
                    item.expectedOutput(),
                    actualOutput,
                    matched,
                    buildOjCaseCheckMessage(caseResult, hasManualOutput, matched)
            ));
        }
        String summary = passed ? "测试点校验通过" : "存在输出不一致或未返回输出的测试点，仍可保存";
        return new AdminOjCheckResponse(passed, summary + "（已收到标准代码长度 " + standardCodeLength + "）", cases);
    }

    private String normalizeOutput(String output) {
        return (output == null ? "" : output)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+(?=\\n)", "")
                .strip();
    }

    private JudgeCaseResult findJudgeCaseResult(List<JudgeCaseResult> caseResults, int index, long caseNumber) {
        if (caseResults == null || caseResults.isEmpty()) {
            return null;
        }
        return caseResults.stream()
                .filter(candidate -> candidate.testCaseId() != null && candidate.testCaseId() == caseNumber)
                .findFirst()
                .orElse(index < caseResults.size() ? caseResults.get(index) : null);
    }

    private String buildOjCaseCheckMessage(JudgeCaseResult caseResult, boolean hasManualOutput, boolean matched) {
        if (caseResult == null) {
            return "判题沙箱未返回该测试点结果";
        }
        String sandboxMessage = clean(caseResult.message(), "", 1000);
        if (caseResult.actualOutput() == null) {
            String suffix = sandboxMessage.isBlank() ? "" : "；沙箱提示：" + sandboxMessage;
            return "判题沙箱未返回 actualOutput，请重启 judge-sandbox 或确认沙箱代码已更新" + suffix;
        }
        if (matched) {
            return hasManualOutput ? "输出正确" : "已根据标准代码生成实际输出";
        }
        if (caseResult.status() != null && caseResult.status() != SubmissionStatus.ACCEPTED && !sandboxMessage.isBlank()) {
            return "标准代码运行结果：" + sandboxMessage;
        }
        return hasManualOutput
                ? "手动输出与标准代码运行结果不一致"
                : "标准代码未通过该测试点";
    }

    private AdminQuestionRequest normalizeQuestionRequest(AdminQuestionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目信息不能为空");
        }
        String type = clean(request.type(), "single", 32);
        if (!List.of("single", "multiple", "short", "vocabulary").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目类型不支持");
        }
        return new AdminQuestionRequest(
                required(request.setCode(), "题库编号", 64),
                type,
                required(request.stem(), "题干", 4000),
                request.options() == null ? List.of() : request.options(),
                clean(request.answer(), "", 4000),
                clean(request.explanation(), "", 4000),
                clean(request.difficultyLabel(), "", 64),
                clean(request.sourceUrl(), "", 512),
                request.sortOrder() == null ? 0 : request.sortOrder()
        );
    }

    /**
     * 标准化课程类型，验证是否为支持的类型。
     *
     * @param resourceType 课程类型
     * @return 标准化后的课程类型
     */
    private String normalizeResourceType(String resourceType) {
        String value = clean(resourceType, "online-open-courses", 64);
        if (!List.of("online-open-courses", "general-courses", "micro-major-courses").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程类型不支持");
        }
        return value;
    }

    /**
     * 验证必填字段，空值抛出异常。
     *
     * @param value 待验证的值
     * @param label 字段标签
     * @param maxLength 最大长度
     * @return 清理后的值
     */
    private String required(String value, String label, int maxLength) {
        String normalized = clean(value, "", maxLength);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不能为空");
        }
        return normalized;
    }

    /**
     * 清理字符串，空值返回默认值，超长截断。
     *
     * @param value 待清理的字符串
     * @param fallback 默认值
     * @param maxLength 最大长度
     * @return 清理后的字符串
     */
    private String clean(String value, String fallback, int maxLength) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        if (maxLength > 0 && normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }
}
