package com.cupk.admin.service;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminUserUpdateRequest;
import com.cupk.admin.dto.AdminVoucherItemRequest;
import com.cupk.admin.repository.AdminRepository;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.rewards.VoucherCatalog;
import com.cupk.rewards.dto.VoucherItemResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 管理员服务，提供用户管理、课程管理、题库管理和卡券管理等后台业务逻辑。
 */
@Service
public class AdminService {
    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_ROLE = "admin";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数，注入依赖组件。
     *
     * @param adminRepository 管理员数据访问层
     * @param passwordEncoder 密码编码器
     */
    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员账号为固定账号，不允许在后台修改");
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
    public void deleteReview(Long currentUserId, long reviewId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteReview(reviewId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
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
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录管理员账号");
        }
        AdminRepository.AdminAuthRow user = adminRepository.findAuthRow(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录管理员账号"));
        if (!ADMIN_EMAIL.equalsIgnoreCase(user.email()) || !ADMIN_ROLE.equalsIgnoreCase(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
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
        return new AdminCourseRequest(
                resourceType,
                required(request.id(), "课程编号", 128),
                required(request.name(), "课程名称", 120),
                clean(request.teacher(), "", 80),
                clean(request.category(), "", 80),
                clean(request.school(), "", 120),
                clean(request.coverUrl(), "", 512),
                clean(request.coverFilePath(), "", 255),
                clean(request.startTime(), "", 64),
                request.participants() == null ? 0 : Math.max(0, request.participants()),
                clean(request.comment(), "", 512),
                clean(request.description(), "", 4000),
                clean(request.semesterPlan(), "", 512),
                clean(request.overview(), "", 1200),
                clean(request.videoFilePath(), "", 512),
                clean(request.link(), "", 512),
                Boolean.TRUE.equals(request.certified())
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
        return new AdminQuestionBankSetRequest(
                required(request.categoryCode(), "分类编号", 64),
                required(request.categoryName(), "分类名称", 64),
                clean(request.categoryDescription(), "", 512),
                code,
                required(request.title(), "题库名称", 128),
                clean(request.subtitle(), "", 128),
                clean(request.description(), "", 512),
                clean(request.coverUrl(), "", 512),
                clean(request.coverFilePath(), "", 255),
                clean(request.difficultyLabel(), "", 64),
                clean(request.statusLabel(), "", 64),
                clean(request.sourceName(), "", 128),
                clean(request.sourceUrl(), "", 512),
                request.sourceRefs() == null ? List.of() : request.sourceRefs(),
                clean(request.routePath(), "/academy/question-bank/courses/" + code, 255),
                request.sortOrder() == null ? 0 : request.sortOrder()
        );
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
