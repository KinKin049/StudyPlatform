package com.cupk.admin.service;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminUserUpdateRequest;
import com.cupk.admin.repository.AdminRepository;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_ROLE = "admin";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUserResponse> listUsers(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findUsers();
    }

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
        adminRepository.updateUser(
                userId,
                username,
                email,
                roleType,
                clean(request.learningGoal(), "", 255),
                clean(request.school(), "", 128),
                clean(request.teacherName(), "", 64),
                request.coinAdjustment(),
                clean(request.dataNote(), "", 512),
                passwordHash
        );
        return adminRepository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public void deleteUser(Long currentUserId, long userId) {
        ensureAdmin(currentUserId);
        AdminUserResponse user = adminRepository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (isAdminUser(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员账号不能删除");
        }
        adminRepository.deleteUser(userId);
    }

    public List<AdminCourseResponse> listCourses(Long currentUserId, String resourceType) {
        ensureAdmin(currentUserId);
        return adminRepository.findCourses(normalizeResourceType(resourceType));
    }

    public AdminCourseResponse saveCourse(Long currentUserId, AdminCourseRequest request) {
        ensureAdmin(currentUserId);
        AdminCourseRequest safeRequest = normalizeCourseRequest(request);
        adminRepository.upsertCourse(safeRequest);
        return adminRepository.findCourse(safeRequest.resourceType(), safeRequest.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程保存失败"));
    }

    public void deleteCourse(Long currentUserId, String resourceType, String courseId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteCourse(normalizeResourceType(resourceType), required(courseId, "课程编号", 128));
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
    }

    public List<AdminCourseReviewResponse> listReviews(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findReviews();
    }

    public void deleteReview(Long currentUserId, long reviewId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteReview(reviewId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
    }

    public List<CourseQuestionBankSetResponse> listQuestionBankSets(Long currentUserId) {
        ensureAdmin(currentUserId);
        return adminRepository.findQuestionBankSets();
    }

    public CourseQuestionBankSetResponse saveQuestionBankSet(Long currentUserId, AdminQuestionBankSetRequest request) {
        ensureAdmin(currentUserId);
        AdminQuestionBankSetRequest safeRequest = normalizeQuestionBankSetRequest(request);
        adminRepository.upsertQuestionBankSet(safeRequest);
        return adminRepository.findQuestionBankSets().stream()
                .filter(set -> set.code().equals(safeRequest.code()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题库保存失败"));
    }

    public void deleteQuestionBankSet(Long currentUserId, String setCode) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteQuestionBankSet(required(setCode, "题库编号", 64));
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "题库不存在");
        }
    }

    public List<CourseQuestionBankQuestionResponse> listQuestions(Long currentUserId, String setCode) {
        ensureAdmin(currentUserId);
        return adminRepository.findQuestions(required(setCode, "题库编号", 64));
    }

    public long saveQuestion(Long currentUserId, Long questionId, AdminQuestionRequest request) {
        ensureAdmin(currentUserId);
        AdminQuestionRequest safeRequest = normalizeQuestionRequest(request);
        return adminRepository.upsertQuestion(questionId, safeRequest);
    }

    public void deleteQuestion(Long currentUserId, long questionId) {
        ensureAdmin(currentUserId);
        int deleted = adminRepository.deleteQuestion(questionId);
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在");
        }
    }

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

    private boolean isAdminUser(AdminUserResponse user) {
        return ADMIN_EMAIL.equalsIgnoreCase(user.email()) || ADMIN_ROLE.equalsIgnoreCase(user.roleType());
    }

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

    private String normalizeResourceType(String resourceType) {
        String value = clean(resourceType, "online-open-courses", 64);
        if (!List.of("online-open-courses", "general-courses", "micro-major-courses").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程类型不支持");
        }
        return value;
    }

    private String required(String value, String label, int maxLength) {
        String normalized = clean(value, "", maxLength);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不能为空");
        }
        return normalized;
    }

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
