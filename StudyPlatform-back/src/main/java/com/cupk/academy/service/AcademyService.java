package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyHomeItemResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookCartItemResponse;
import com.cupk.academy.dto.AcademyTextbookCartRequest;
import com.cupk.academy.dto.AcademyTextbookDetailResponse;
import com.cupk.academy.dto.AcademyTextbookOrderRequest;
import com.cupk.academy.dto.AcademyTextbookOrderResponse;
import com.cupk.academy.dto.AcademyTextbookCommentResponse;
import com.cupk.academy.dto.AcademyTextbookReviewRequest;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.repository.AcademyRepository.AcademyTextbookOrderResponseData;
import com.cupk.academy.repository.AcademyRepository;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.repository.AuthUserRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademyService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyRepository academyRepository;
    private final AuthUserRepository authUserRepository;

    public AcademyService(AcademyRepository academyRepository, AuthUserRepository authUserRepository) {
        this.academyRepository = academyRepository;
        this.authUserRepository = authUserRepository;
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

    public List<AcademyCourseResponse> listMyPublishedOnlineOpenCourses(Long userId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        return withCourseCovers(academyRepository.findPublishedOnlineOpenCourses(teacherUserId));
    }

    public AcademyCourseEnrollmentResponse deletePublishedOnlineOpenCourse(Long userId, String courseId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        if (courseId == null || courseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程编号不能为空");
        }
        int deleted = academyRepository.deletePublishedOnlineOpenCourse(teacherUserId, courseId.trim());
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在或不属于当前教师");
        }
        return new AcademyCourseEnrollmentResponse(false, "课程已删除");
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

    public AcademyTextbookDetailResponse getTextbook(String id) {
        return getTextbook(id, DEFAULT_USER_ID);
    }

    public AcademyTextbookDetailResponse getTextbook(String id, Long userId) {
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        long normalizedUserId = normalizeUserId(userId);
        AcademyTextbookDetailResponse textbook = academyRepository.findTextbookById(id.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教材不存在"));
        return withTextbookCover(textbook, normalizedUserId);
    }

    public List<AcademyTextbookCartItemResponse> listTextbookCart(Long userId) {
        return academyRepository.findTextbookCartItems(normalizeUserId(userId));
    }

    public List<AcademyTextbookCartItemResponse> addTextbookCartItem(AcademyTextbookCartRequest request) {
        if (request == null || request.textbookId() == null || request.textbookId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        getTextbook(request.textbookId());
        long userId = normalizeUserId(request.userId());
        academyRepository.addTextbookCartItem(userId, request.textbookId().trim(), normalizeQuantity(request.quantity()));
        return listTextbookCart(userId);
    }

    public List<AcademyTextbookCartItemResponse> deleteTextbookCartItem(Long userId, Long itemId) {
        long normalizedUserId = normalizeUserId(userId);
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购物车条目不能为空");
        }
        academyRepository.deleteTextbookCartItem(normalizedUserId, itemId);
        return listTextbookCart(normalizedUserId);
    }

    public List<AcademyTextbookCartItemResponse> updateTextbookCartItem(Long userId, Long itemId, Integer quantity) {
        long normalizedUserId = normalizeUserId(userId);
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购物车条目不能为空");
        }
        academyRepository.updateTextbookCartItem(normalizedUserId, itemId, normalizeQuantity(quantity));
        return listTextbookCart(normalizedUserId);
    }

    public AcademyTextbookOrderResponse createTextbookOrder(AcademyTextbookOrderRequest request) {
        long userId = normalizeUserId(request == null ? null : request.userId());
        List<Long> cartItemIds = request == null || request.cartItemIds() == null
                ? List.of()
                : request.cartItemIds().stream()
                        .filter(itemId -> itemId != null && itemId > 0)
                        .distinct()
                        .toList();
        if (!cartItemIds.isEmpty()) {
            AcademyTextbookOrderResponseData order = academyRepository.createTextbookOrderFromCart(userId, cartItemIds);
            if (order.orderNo() == null || order.orderNo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的购物车教材");
            }
            return new AcademyTextbookOrderResponse(order.orderNo(), order.totalAmount(), "待支付", "订单已创建，请完成结算", false);
        }
        if (request == null || request.textbookId() == null || request.textbookId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        AcademyTextbookDetailResponse textbook = getTextbook(request.textbookId());
        AcademyTextbookOrderResponseData order = academyRepository.createTextbookOrder(
                userId,
                textbook,
                normalizeQuantity(request.quantity())
        );
        return new AcademyTextbookOrderResponse(order.orderNo(), order.totalAmount(), "待支付", "订单已创建", false);
    }

    public AcademyTextbookOrderResponse payTextbookOrder(String orderNo, Long userId) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单编号不能为空");
        }
        AcademyTextbookOrderResponseData order = academyRepository.payTextbookOrder(normalizeUserId(userId), orderNo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在或不可支付"));
        return new AcademyTextbookOrderResponse(order.orderNo(), order.totalAmount(), "已支付", "支付成功，教材已购买", true);
    }

    public AcademyTextbookCommentResponse saveTextbookReview(String textbookId, AcademyTextbookReviewRequest request) {
        if (textbookId == null || textbookId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        getTextbook(textbookId);
        long userId = normalizeUserId(request == null ? null : request.userId());
        if (!academyRepository.hasPurchasedTextbook(userId, textbookId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "购买教材后才能评论");
        }
        String content = clean(request == null ? null : request.content(), 800);
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        String userName = clean(request == null ? null : request.userName(), 80);
        if (userName.isBlank()) {
            userName = "默认用户";
        }
        int rating = normalizeRating(request == null ? null : request.rating());
        return academyRepository.saveTextbookReview(userId, textbookId.trim(), userName, rating, content);
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

    public AcademyCourseResponse publishOnlineOpenCourse(
            Long userId,
            String courseName,
            String startTime,
            String semesterPlan,
            String courseDetail,
            String courseOverview,
            MultipartFile cover,
            MultipartFile video
    ) {
        long publisherUserId = normalizeUserId(userId);
        AuthUserResponse user = ensureTeacher(publisherUserId);
        String normalizedName = clean(courseName, 120);
        String normalizedStartTime = clean(startTime, 64);
        String normalizedSemesterPlan = clean(semesterPlan, 512);
        String normalizedDetail = clean(courseDetail, 4000);
        String normalizedOverview = clean(courseOverview, 1200);
        if (normalizedName.isBlank()
                || normalizedStartTime.isBlank()
                || normalizedSemesterPlan.isBlank()
                || normalizedDetail.isBlank()
                || normalizedOverview.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程信息不能为空");
        }

        String coverPath = saveCourseFile(publisherUserId, cover, "cover", List.of("jpg", "jpeg", "png", "webp"), true);
        String videoPath = saveCourseFile(publisherUserId, video, "video", List.of("mp4", "webm", "ogg", "mov"), true);
        String courseId = academyRepository.publishOnlineOpenCourse(
                publisherUserId,
                normalizedName,
                clean(user.teacherName(), 80),
                clean(user.school(), 120),
                "教师发布",
                normalizedStartTime,
                normalizedSemesterPlan,
                normalizedDetail,
                normalizedOverview,
                coverPath,
                videoPath
        );
        return getOnlineOpenCourse(courseId);
    }

    public AcademyCourseEnrollmentResponse enrollCourse(String resourceType, String courseId, Long userId) {
        ensureCourseExists(resourceType, courseId);
        academyRepository.enrollCourse(resourceType, courseId, normalizeUserId(userId));
        return new AcademyCourseEnrollmentResponse(true, "已参加课程");
    }

    public AcademyCourseEnrollmentResponse unenrollCourse(String resourceType, String courseId, Long userId) {
        ensureCourseExists(resourceType, courseId);
        academyRepository.unenrollCourse(resourceType, courseId, normalizeUserId(userId));
        return new AcademyCourseEnrollmentResponse(false, "已退出课程");
    }

    public List<AcademyEnrolledCourseResponse> listMyCourses(Long userId) {
        return academyRepository.findEnrolledCourses(normalizeUserId(userId)).stream()
                .map(this::withEnrolledCourseCover)
                .toList();
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
                course.semesterPlan(),
                course.overview(),
                fileUrl(course.videoFilePath()),
                course.videoFilePath(),
                course.link()
        );
    }

    private AcademyTextbookDetailResponse withTextbookCover(AcademyTextbookDetailResponse textbook, long userId) {
        List<AcademyTextbookCommentResponse> reviews = academyRepository.findTextbookReviews(textbook.id());
        List<AcademyTextbookCommentResponse> comments = reviews.isEmpty() ? textbook.comments() : reviews;
        boolean purchased = academyRepository.hasPurchasedTextbook(userId, textbook.id());
        return new AcademyTextbookDetailResponse(
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
                textbook.link(),
                textbook.recommendation(),
                textbook.originalPrice(),
                textbook.discountPrice(),
                textbook.readerCount(),
                textbook.overview(),
                textbook.catalog(),
                comments,
                purchased
        );
    }

    private AuthUserResponse ensureTeacher(long userId) {
        AuthUserResponse user = authUserRepository.findResponseById(userId);
        if (!"teacher".equals(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教师可以使用该功能");
        }
        return user;
    }

    private AcademyEnrolledCourseResponse withEnrolledCourseCover(AcademyEnrolledCourseResponse course) {
        return new AcademyEnrolledCourseResponse(
                course.resourceType(),
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
                course.semesterPlan(),
                course.overview(),
                fileUrl(course.videoFilePath()),
                course.videoFilePath(),
                course.link(),
                course.enrolledAt()
        );
    }

    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return Math.min(quantity, 99);
    }

    private int normalizeRating(Integer rating) {
        if (rating == null) {
            return 5;
        }
        return Math.max(1, Math.min(rating, 5));
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

    private String saveCourseFile(
            long userId,
            MultipartFile file,
            String type,
            List<String> allowedExtensions,
            boolean required
    ) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传课程" + ("cover".equals(type) ? "封面" : "视频"));
            }
            return null;
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程" + ("cover".equals(type) ? "封面" : "视频") + "格式不支持");
        }
        Path storageDirectory = resolveStoragePath()
                .resolve("teacher_courses")
                .resolve(String.valueOf(userId))
                .normalize();
        String fileName = type + "-" + UUID.randomUUID() + "." + extension;
        Path targetPath = storageDirectory.resolve(fileName).normalize();
        if (!targetPath.startsWith(storageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名不合法");
        }
        try {
            Files.createDirectories(storageDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "课程文件保存失败", ex);
        }
        return "teacher_courses/" + userId + "/" + fileName;
    }

    private String resolveExtension(String originalFilename) {
        String fileName = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    private Path resolveStoragePath() {
        Path currentDirectory = Path.of("").toAbsolutePath();
        Path directStorage = currentDirectory.resolve("storage").normalize();
        if (Files.isDirectory(directStorage)) {
            return directStorage;
        }
        Path backendStorage = currentDirectory.resolve("StudyPlatform-back").resolve("storage").normalize();
        if (Files.isDirectory(backendStorage)) {
            return backendStorage;
        }
        return directStorage;
    }

    private String clean(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (maxLength > 0 && normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }
}
