package com.cupk.academy.repository;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyTextbookCommentResponse;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyTextbookCartItemResponse;
import com.cupk.academy.dto.AcademyTextbookDetailResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.dto.TeacherMailboxMessageResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class AcademyRepository {
    private static final Set<String> ALLOWED_CATEGORY_TABLES = Set.of(
            "excellent_textbooks",
            "online_open_courses",
            "general_courses",
            "micro_major_courses"
    );

    /**
     * 课程与教材数据访问层，提供在线公开课、普通课程、微专业课程和精品教材的查询、发布、报名、评论及购物车等功能。
     */

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     */
    public AcademyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询所有在线公开课列表
     *
     * @return 在线公开课列表
     */
    public List<AcademyCourseResponse> findOnlineOpenCourses() {
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       COALESCE(p.course_overview, course_comment) AS course_comment,
                       COALESCE(p.course_detail, c.course_description, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       source_url, c.certified, c.certification_label
                FROM online_open_courses c
                LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                ORDER BY c.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAcademyCourse(rs));
    }

    /**
     * 发布在线公开课
     *
     * @param publisherUserId 发布者用户ID
     * @param courseName 课程名称
     * @param teacherName 教师名称
     * @param schoolName 学校名称
     * @param category 课程分类
     * @param startTime 开课时间
     * @param semesterPlan 学期计划
     * @param courseDetail 课程详情
     * @param courseOverview 课程概述
     * @param coverFilePath 封面文件路径
     * @param videoFilePath 视频文件路径
     * @return 课程ID
     */
    public String publishOnlineOpenCourse(
            long publisherUserId,
            String courseName,
            String teacherName,
            String schoolName,
            String category,
            String startTime,
            String semesterPlan,
            String courseDetail,
            String courseOverview,
            String coverFilePath,
            String videoFilePath
    ) {
        String courseId = "teacher-" + publisherUserId + "-" + System.currentTimeMillis();
        String insertCourseSql = """
                INSERT INTO online_open_courses
                  (external_course_id, course_name, teacher_name, category, school_name,
                   cover_file_path, start_time, participant_count, course_comment, source_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """;
        jdbcTemplate.update(
                insertCourseSql,
                courseId,
                courseName,
                teacherName,
                category,
                schoolName,
                coverFilePath,
                startTime,
                courseOverview,
                "/academy/open-courses/" + courseId
        );

        String insertPublishSql = """
                INSERT INTO teacher_published_courses
                  (course_id, publisher_user_id, semester_plan, course_overview, course_detail, video_file_path)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                insertPublishSql,
                courseId,
                publisherUserId,
                semesterPlan,
                courseOverview,
                courseDetail,
                videoFilePath
        );
        return courseId;
    }

    /**
     * 将数据库结果集映射为课程响应对象
     *
     * @param rs 结果集
     * @return 课程响应对象
     * @throws java.sql.SQLException SQL异常
     */
    private AcademyCourseResponse mapAcademyCourse(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AcademyCourseResponse(
                rs.getString("external_course_id"),
                rs.getString("course_name"),
                rs.getString("teacher_name"),
                rs.getString("category"),
                rs.getString("school_name"),
                null,
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getString("start_time"),
                rs.getObject("participant_count", Integer.class),
                rs.getString("course_comment"),
                rs.getString("course_description"),
                getOptionalString(rs, "semester_plan"),
                getOptionalString(rs, "course_overview"),
                fileUrl(getOptionalString(rs, "video_file_path")),
                getOptionalString(rs, "video_file_path"),
                rs.getString("source_url"),
                getOptionalBoolean(rs, "certified"),
                getOptionalString(rs, "certification_label")
        );
    }

    /**
     * 根据ID查询在线公开课详情
     *
     * @param id 课程ID
     * @return 课程详情，不存在则返回空
     */
    public Optional<AcademyCourseResponse> findOnlineOpenCourseById(String id) {
        String sql = """
                SELECT c.external_course_id, c.course_name, c.teacher_name, c.category, c.school_name,
                       c.cover_url, c.cover_file_path, c.start_time, c.participant_count,
                       COALESCE(p.course_overview, c.course_comment) AS course_comment,
                       COALESCE(p.course_detail, c.course_description, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       c.source_url, c.certified, c.certification_label
                FROM online_open_courses c
                LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                WHERE c.external_course_id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapAcademyCourse(rs), id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int updateOnlineOpenCourseSourceContent(String courseId, String courseComment, String courseDescription) {
        return jdbcTemplate.update(
                """
                UPDATE online_open_courses
                SET course_comment = COALESCE(NULLIF(?, ''), course_comment),
                    course_description = COALESCE(NULLIF(?, ''), course_description),
                    source_synced_at = CURRENT_TIMESTAMP
                WHERE external_course_id = ?
                """,
                courseComment,
                courseDescription,
                courseId
        );
    }

    /**
     * 查询指定用户发布的在线公开课列表
     *
     * @param publisherUserId 发布者用户ID
     * @return 发布的课程列表
     */
    public List<AcademyCourseResponse> findPublishedOnlineOpenCourses(long publisherUserId) {
        String sql = """
                SELECT c.external_course_id, c.course_name, c.teacher_name, c.category, c.school_name,
                       c.cover_url, c.cover_file_path, c.start_time, c.participant_count,
                       COALESCE(p.course_overview, c.course_comment) AS course_comment,
                       COALESCE(p.course_detail, c.course_description, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       c.source_url, c.certified, c.certification_label
                FROM teacher_published_courses p
                JOIN online_open_courses c ON c.external_course_id = p.course_id
                WHERE p.publisher_user_id = ?
                ORDER BY p.updated_at DESC, p.id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAcademyCourse(rs), publisherUserId);
    }

    /**
     * 判断用户是否为课程所有者
     *
     * @param publisherUserId 用户ID
     * @param courseId 课程ID
     * @return 是否为所有者
     */
    public boolean isPublishedOnlineOpenCourseOwner(long publisherUserId, String courseId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM teacher_published_courses
                WHERE publisher_user_id = ? AND course_id = ?
                """,
                Long.class,
                publisherUserId,
                courseId
        );
        return count != null && count > 0;
    }

    public Optional<PublishedCourseOwnerRow> findPublishedCourseForTeacher(long publisherUserId, String courseId) {
        String sql = """
                SELECT c.external_course_id, c.course_name, c.teacher_name
                FROM teacher_published_courses p
                JOIN online_open_courses c ON c.external_course_id = p.course_id
                WHERE p.publisher_user_id = ? AND p.course_id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new PublishedCourseOwnerRow(
                            rs.getString("external_course_id"),
                            rs.getString("course_name"),
                            rs.getString("teacher_name")
                    ),
                    publisherUserId,
                    courseId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int updatePublishedOnlineOpenCourse(
            long publisherUserId,
            String courseId,
            String courseName,
            String teacherName,
            String schoolName,
            String category,
            String startTime,
            String semesterPlan,
            String courseDetail,
            String courseOverview,
            String coverFilePath,
            String videoFilePath
    ) {
        if (!isPublishedOnlineOpenCourseOwner(publisherUserId, courseId)) {
            return 0;
        }
        int courseRows = jdbcTemplate.update(
                """
                UPDATE online_open_courses
                SET course_name = ?,
                    teacher_name = ?,
                    school_name = ?,
                    category = ?,
                    start_time = ?,
                    course_comment = ?,
                    course_description = ?,
                    cover_file_path = COALESCE(?, cover_file_path),
                    source_url = ?
                WHERE external_course_id = ?
                """,
                courseName,
                teacherName,
                schoolName,
                category,
                startTime,
                courseOverview,
                courseDetail,
                coverFilePath,
                "/academy/open-courses/" + courseId,
                courseId
        );
        jdbcTemplate.update(
                """
                UPDATE teacher_published_courses
                SET semester_plan = ?,
                    course_overview = ?,
                    course_detail = ?,
                    video_file_path = COALESCE(?, video_file_path)
                WHERE publisher_user_id = ? AND course_id = ?
                """,
                semesterPlan,
                courseOverview,
                courseDetail,
                videoFilePath,
                publisherUserId,
                courseId
        );
        return courseRows;
    }

    /**
     * 删除发布的在线公开课（级联删除关联数据）
     *
     * @param publisherUserId 发布者用户ID
     * @param courseId 课程ID
     * @return 删除的行数
     */
    public int deletePublishedOnlineOpenCourse(long publisherUserId, String courseId) {
        if (!isPublishedOnlineOpenCourseOwner(publisherUserId, courseId)) {
            return 0;
        }
        jdbcTemplate.update(
                "DELETE FROM academy_course_reviews WHERE resource_type = 'online-open-courses' AND course_id = ?",
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM academy_course_enrollments WHERE resource_type = 'online-open-courses' AND course_id = ?",
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM teacher_published_courses WHERE publisher_user_id = ? AND course_id = ?",
                publisherUserId,
                courseId
        );
        return jdbcTemplate.update(
                "DELETE FROM online_open_courses WHERE external_course_id = ?",
                courseId
        );
    }

    /**
     * 查询普通课程列表
     *
     * @return 普通课程列表
     */
    public List<AcademyCourseResponse> findGeneralCourses() {
        return findLearningCourses("general_courses");
    }

    /**
     * 根据ID查询普通课程详情
     *
     * @param id 课程ID
     * @return 课程详情，不存在则返回空
     */
    public Optional<AcademyCourseResponse> findGeneralCourseById(String id) {
        return findLearningCourseById("general_courses", false, id);
    }

    /**
     * 查询微专业课程列表
     *
     * @return 微专业课程列表
     */
    public List<AcademyCourseResponse> findMicroMajorCourses() {
        return findLearningCourses("micro_major_courses");
    }

    /**
     * 根据ID查询微专业课程详情
     *
     * @param id 课程ID
     * @return 课程详情，不存在则返回空
     */
    public Optional<AcademyCourseResponse> findMicroMajorCourseById(String id) {
        return findLearningCourseById("micro_major_courses", false, id);
    }

    /**
     * 查询精品教材列表
     *
     * @return 教材列表
     */
    public List<AcademyTextbookResponse> findTextbooks() {
        String sql = """
                SELECT external_textbook_id, textbook_name, chief_editor, category, publisher,
                       publish_date, isbn, description, cover_url, cover_file_path, source_url
                FROM excellent_textbooks
                ORDER BY id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyTextbookResponse(
                rs.getString("external_textbook_id"),
                rs.getString("textbook_name"),
                rs.getString("chief_editor"),
                rs.getString("category"),
                rs.getString("publisher"),
                rs.getString("publish_date"),
                rs.getString("isbn"),
                rs.getString("description"),
                null,
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getString("source_url")
        ));
    }

    public Optional<AcademyTextbookOrderResponseData> findPendingTextbookOrder(Long userId, String orderNo) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT order_no, total_amount, original_amount, discount_amount, voucher_key, voucher_name, voucher_consumed
                    FROM academy_textbook_orders
                    WHERE user_id = ? AND order_no = ? AND order_status = '待支付'
                    LIMIT 1
                    """,
                    (rs, rowNum) -> new AcademyTextbookOrderResponseData(
                            rs.getString("order_no"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("original_amount"),
                            rs.getBigDecimal("discount_amount"),
                            rs.getString("voucher_key"),
                            rs.getString("voucher_name"),
                            rs.getBoolean("voucher_consumed")
                    ),
                    userId,
                    orderNo
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public TextbookPaymentSessionData createTextbookPaymentSession(
            Long userId,
            String orderNo,
            String gatewayOrderNo,
            String sessionId,
            String provider,
            BigDecimal amount,
            String qrPayload,
            LocalDateTime expiresAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_payments
                  (session_id, order_no, gateway_order_no, user_id, provider, amount, payment_status, qr_payload, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                """,
                sessionId,
                orderNo,
                gatewayOrderNo,
                userId,
                provider,
                amount,
                qrPayload,
                Timestamp.valueOf(expiresAt)
        );
        return findTextbookPaymentSession(sessionId).orElseThrow();
    }

    public Optional<TextbookPaymentSessionData> findTextbookPaymentSession(String sessionId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT session_id, order_no, COALESCE(NULLIF(gateway_order_no, ''), order_no) AS gateway_order_no,
                           user_id, provider, amount, payment_status, qr_payload, expires_at, paid_at
                    FROM academy_textbook_payments
                    WHERE session_id = ?
                    LIMIT 1
                    """,
                    (rs, rowNum) -> new TextbookPaymentSessionData(
                            rs.getString("session_id"),
                            rs.getString("order_no"),
                            rs.getString("gateway_order_no"),
                            rs.getLong("user_id"),
                            rs.getString("provider"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_status"),
                            rs.getString("qr_payload"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime()
                    ),
                    sessionId
            ));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public int markTextbookPaymentSessionPaid(String sessionId) {
        return jdbcTemplate.update(
                """
                UPDATE academy_textbook_payments
                SET payment_status = 'PAID', paid_at = CURRENT_TIMESTAMP
                WHERE session_id = ? AND payment_status = 'PENDING' AND expires_at > CURRENT_TIMESTAMP
                """,
                sessionId
        );
    }

    public int expireTextbookPaymentSession(String sessionId) {
        return jdbcTemplate.update(
                """
                UPDATE academy_textbook_payments
                SET payment_status = 'EXPIRED'
                WHERE session_id = ? AND payment_status = 'PENDING' AND expires_at <= CURRENT_TIMESTAMP
                """,
                sessionId
        );
    }

    /**
     * 根据ID查询教材详情
     *
     * @param id 教材ID
     * @return 教材详情，不存在则返回空
     */
    public Optional<AcademyTextbookDetailResponse> findTextbookById(String id) {
        String sql = """
                SELECT t.external_textbook_id, t.textbook_name, t.chief_editor, t.category, t.publisher,
                       t.publish_date, t.isbn, t.description, t.cover_url, t.cover_file_path, t.source_url,
                       d.recommendation, d.original_price, d.discount_price, d.reader_count,
                       d.overview, d.catalog_text, d.comments_text
                FROM excellent_textbooks t
                LEFT JOIN academy_textbook_details d ON d.textbook_id = t.external_textbook_id
                WHERE t.external_textbook_id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AcademyTextbookDetailResponse(
                    rs.getString("external_textbook_id"),
                    rs.getString("textbook_name"),
                    rs.getString("chief_editor"),
                    rs.getString("category"),
                    rs.getString("publisher"),
                    rs.getString("publish_date"),
                    rs.getString("isbn"),
                    rs.getString("description"),
                    null,
                    rs.getString("cover_url"),
                    rs.getString("cover_file_path"),
                    rs.getString("source_url"),
                    getOptionalString(rs, "recommendation"),
                    getOptionalBigDecimal(rs, "original_price", new BigDecimal("69.00")),
                    getOptionalBigDecimal(rs, "discount_price", new BigDecimal("49.00")),
                    getOptionalInt(rs, "reader_count", 0),
                    getOptionalString(rs, "overview"),
                    parseCatalog(getOptionalString(rs, "catalog_text")),
                    parseComments(getOptionalString(rs, "comments_text")),
                    false
            ), id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 判断用户是否已购买教材
     *
     * @param userId 用户ID
     * @param textbookId 教材ID
     * @return 是否已购买
     */
    public boolean hasPurchasedTextbook(Long userId, String textbookId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM academy_textbook_orders o
                JOIN academy_textbook_order_items i ON i.order_id = o.id
                WHERE o.user_id = ?
                  AND i.textbook_id = ?
                  AND o.order_status IN ('已支付', '已完成')
                """,
                Long.class,
                userId,
                textbookId
        );
        return count != null && count > 0;
    }

    /**
     * 查询教材评论列表
     *
     * @param textbookId 教材ID
     * @return 评论列表
     */
    public List<AcademyTextbookCommentResponse> findTextbookReviews(String textbookId) {
        String sql = """
                SELECT user_name, rating, content, reply_content, reply_user_name, reply_user_role_type
                FROM academy_textbook_reviews
                WHERE textbook_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyTextbookCommentResponse(
                rs.getString("user_name"),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getString("reply_content"),
                rs.getString("reply_user_name"),
                rs.getString("reply_user_role_type")
        ), textbookId);
    }

    /**
     * 保存教材评论
     *
     * @param userId 用户ID
     * @param textbookId 教材ID
     * @param userName 用户名
     * @param rating 评分
     * @param content 评论内容
     * @return 保存后的评论
     */
    public AcademyTextbookCommentResponse saveTextbookReview(
            Long userId,
            String textbookId,
            String userName,
            int rating,
            String content
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_reviews (user_id, textbook_id, user_name, rating, content)
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                textbookId,
                userName,
                rating,
                content
        );
        return jdbcTemplate.queryForObject(
                """
                SELECT user_name, rating, content, reply_content, reply_user_name, reply_user_role_type
                FROM academy_textbook_reviews
                WHERE user_id = ? AND textbook_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new AcademyTextbookCommentResponse(
                        rs.getString("user_name"),
                        rs.getInt("rating"),
                        rs.getString("content"),
                        rs.getString("reply_content"),
                        rs.getString("reply_user_name"),
                        rs.getString("reply_user_role_type")
                ),
                userId,
                textbookId
        );
    }

    /**
     * 查询用户教材购物车列表
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    public List<AcademyTextbookCartItemResponse> findTextbookCartItems(Long userId) {
        String sql = """
                SELECT c.id, c.textbook_id, t.textbook_name, t.chief_editor, t.publisher,
                       t.cover_url, t.cover_file_path,
                       COALESCE(d.discount_price, 49.00) AS unit_price,
                       c.quantity, c.created_at
                FROM academy_textbook_cart_items c
                JOIN excellent_textbooks t ON t.external_textbook_id = c.textbook_id
                LEFT JOIN academy_textbook_details d ON d.textbook_id = t.external_textbook_id
                WHERE c.user_id = ?
                ORDER BY c.updated_at DESC, c.id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyTextbookCartItemResponse(
                rs.getLong("id"),
                rs.getString("textbook_id"),
                rs.getString("textbook_name"),
                rs.getString("chief_editor"),
                rs.getString("publisher"),
                fileUrl(rs.getString("cover_file_path")),
                rs.getString("cover_url"),
                rs.getBigDecimal("unit_price"),
                rs.getInt("quantity"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId);
    }

    /**
     * 添加教材到购物车（已存在则更新数量）
     *
     * @param userId 用户ID
     * @param textbookId 教材ID
     * @param quantity 数量
     */
    public void addTextbookCartItem(Long userId, String textbookId, Integer quantity) {
        String sql = """
                INSERT INTO academy_textbook_cart_items (user_id, textbook_id, quantity)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  quantity = LEAST(quantity + VALUES(quantity), 99),
                  updated_at = CURRENT_TIMESTAMP
                """;
        jdbcTemplate.update(sql, userId, textbookId, quantity);
    }

    /**
     * 删除购物车中的教材商品
     *
     * @param userId 用户ID
     * @param itemId 商品ID
     * @return 删除的行数
     */
    public int deleteTextbookCartItem(Long userId, Long itemId) {
        return jdbcTemplate.update(
                "DELETE FROM academy_textbook_cart_items WHERE user_id = ? AND id = ?",
                userId,
                itemId
        );
    }

    /**
     * 更新购物车中教材商品的数量
     *
     * @param userId 用户ID
     * @param itemId 商品ID
     * @param quantity 数量
     * @return 更新的行数
     */
    public int updateTextbookCartItem(Long userId, Long itemId, Integer quantity) {
        return jdbcTemplate.update(
                """
                UPDATE academy_textbook_cart_items
                SET quantity = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND id = ?
                """,
                quantity,
                userId,
                itemId
        );
    }

    /**
     * 创建教材订单
     *
     * @param userId 用户ID
     * @param textbook 教材详情
     * @param quantity 数量
     * @param voucherKey 优惠券Key
     * @param voucherName 优惠券名称
     * @param voucherDiscountAmount 优惠券折扣金额
     * @return 订单信息
     */
    public AcademyTextbookOrderResponseData createTextbookOrder(
            Long userId,
            AcademyTextbookDetailResponse textbook,
            Integer quantity,
            String voucherKey,
            String voucherName,
            BigDecimal voucherDiscountAmount
    ) {
        String orderNo = "TB" + System.currentTimeMillis() + userId;
        BigDecimal unitPrice = textbook.discountPrice() == null ? BigDecimal.ZERO : textbook.discountPrice();
        BigDecimal originalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        TextbookVoucherDiscount voucherDiscount = resolveTextbookVoucherDiscount(voucherKey, voucherName, voucherDiscountAmount);
        BigDecimal totalAmount = originalAmount.subtract(voucherDiscount.discountAmount()).max(BigDecimal.ZERO);
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_orders
                  (user_id, order_no, total_amount, original_amount, discount_amount, voucher_key, voucher_name, order_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, '待支付')
                """,
                userId,
                orderNo,
                totalAmount,
                originalAmount,
                voucherDiscount.discountAmount(),
                voucherDiscount.voucherKey(),
                voucherDiscount.voucherName()
        );
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM academy_textbook_orders WHERE order_no = ?",
                Long.class,
                orderNo
        );
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_order_items (order_id, textbook_id, textbook_name, unit_price, quantity)
                VALUES (?, ?, ?, ?, ?)
                """,
                orderId,
                textbook.id(),
                textbook.name(),
                unitPrice,
                quantity
        );
        return new AcademyTextbookOrderResponseData(
                orderNo,
                totalAmount,
                originalAmount,
                voucherDiscount.discountAmount(),
                voucherDiscount.voucherKey(),
                voucherDiscount.voucherName(),
                false
        );
    }

    /**
     * 从购物车创建教材订单（创建后删除购物车商品）
     *
     * @param userId 用户ID
     * @param cartItemIds 购物车商品ID列表
     * @param voucherKey 优惠券Key
     * @param voucherName 优惠券名称
     * @param voucherDiscountAmount 优惠券折扣金额
     * @return 订单信息
     */
    public AcademyTextbookOrderResponseData createTextbookOrderFromCart(
            Long userId,
            List<Long> cartItemIds,
            String voucherKey,
            String voucherName,
            BigDecimal voucherDiscountAmount
    ) {
        String placeholders = String.join(", ", Collections.nCopies(cartItemIds.size(), "?"));
        Object[] queryParams = new Object[cartItemIds.size() + 1];
        queryParams[0] = userId;
        for (int index = 0; index < cartItemIds.size(); index += 1) {
            queryParams[index + 1] = cartItemIds.get(index);
        }

        String selectSql = """
                SELECT c.id, c.textbook_id, t.textbook_name,
                       COALESCE(d.discount_price, 49.00) AS unit_price,
                       c.quantity
                FROM academy_textbook_cart_items c
                JOIN excellent_textbooks t ON t.external_textbook_id = c.textbook_id
                LEFT JOIN academy_textbook_details d ON d.textbook_id = t.external_textbook_id
                WHERE c.user_id = ? AND c.id IN (%s)
                ORDER BY c.id ASC
                """.formatted(placeholders);
        List<TextbookCartOrderItem> items = jdbcTemplate.query(selectSql, (rs, rowNum) -> new TextbookCartOrderItem(
                rs.getLong("id"),
                rs.getString("textbook_id"),
                rs.getString("textbook_name"),
                rs.getBigDecimal("unit_price"),
                rs.getInt("quantity")
        ), queryParams);
        if (items.isEmpty()) {
            return new AcademyTextbookOrderResponseData("", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, false);
        }

        BigDecimal totalAmount = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal originalAmount = totalAmount;
        TextbookVoucherDiscount voucherDiscount = resolveTextbookVoucherDiscount(voucherKey, voucherName, voucherDiscountAmount);
        totalAmount = originalAmount.subtract(voucherDiscount.discountAmount()).max(BigDecimal.ZERO);
        String orderNo = "TB" + System.currentTimeMillis() + userId;
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_orders
                  (user_id, order_no, total_amount, original_amount, discount_amount, voucher_key, voucher_name, order_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, '待支付')
                """,
                userId,
                orderNo,
                totalAmount,
                originalAmount,
                voucherDiscount.discountAmount(),
                voucherDiscount.voucherKey(),
                voucherDiscount.voucherName()
        );
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM academy_textbook_orders WHERE order_no = ?",
                Long.class,
                orderNo
        );
        for (TextbookCartOrderItem item : items) {
            jdbcTemplate.update(
                    """
                    INSERT INTO academy_textbook_order_items (order_id, textbook_id, textbook_name, unit_price, quantity)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    orderId,
                    item.textbookId(),
                    item.textbookName(),
                    item.unitPrice(),
                    item.quantity()
            );
        }

        return new AcademyTextbookOrderResponseData(
                orderNo,
                totalAmount,
                originalAmount,
                voucherDiscount.discountAmount(),
                voucherDiscount.voucherKey(),
                voucherDiscount.voucherName(),
                false
        );
    }

    /**
     * 计算购物车商品总金额
     *
     * @param userId 用户ID
     * @param cartItemIds 购物车商品ID列表
     * @return 总金额
     */
    public BigDecimal sumTextbookCartItems(Long userId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String placeholders = String.join(", ", Collections.nCopies(cartItemIds.size(), "?"));
        Object[] queryParams = new Object[cartItemIds.size() + 1];
        queryParams[0] = userId;
        for (int index = 0; index < cartItemIds.size(); index += 1) {
            queryParams[index + 1] = cartItemIds.get(index);
        }
        BigDecimal value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(COALESCE(d.discount_price, 49.00) * c.quantity), 0)
                FROM academy_textbook_cart_items c
                JOIN excellent_textbooks t ON t.external_textbook_id = c.textbook_id
                LEFT JOIN academy_textbook_details d ON d.textbook_id = t.external_textbook_id
                WHERE c.user_id = ? AND c.id IN (%s)
                """.formatted(placeholders),
                BigDecimal.class,
                queryParams
        );
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 支付教材订单
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单信息，支付失败或订单不存在则返回空
     */
    public int deleteTextbookCartItemsForPaidOrder(Long userId, String orderNo) {
        return jdbcTemplate.update(
                """
                DELETE c
                FROM academy_textbook_cart_items c
                JOIN academy_textbook_orders o ON o.user_id = c.user_id AND o.order_no = ?
                JOIN academy_textbook_order_items i ON i.order_id = o.id AND i.textbook_id = c.textbook_id
                WHERE c.user_id = ?
                """,
                orderNo,
                userId
        );
    }

    public Optional<AcademyTextbookOrderResponseData> payTextbookOrder(Long userId, String orderNo) {
        int updated = jdbcTemplate.update(
                """
                UPDATE academy_textbook_orders
                SET order_status = '已支付'
                WHERE user_id = ? AND order_no = ? AND order_status = '待支付'
                """,
                userId,
                orderNo
        );
        if (updated <= 0) {
            Long exists = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM academy_textbook_orders
                    WHERE user_id = ? AND order_no = ? AND order_status IN ('已支付', '已完成')
                    """,
                    Long.class,
                    userId,
                    orderNo
            );
            if (exists == null || exists <= 0) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                SELECT order_no, total_amount, original_amount, discount_amount, voucher_key, voucher_name, voucher_consumed
                FROM academy_textbook_orders
                WHERE user_id = ? AND order_no = ?
                LIMIT 1
                """,
                (rs, rowNum) -> new AcademyTextbookOrderResponseData(
                        rs.getString("order_no"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("original_amount"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getString("voucher_key"),
                        rs.getString("voucher_name"),
                        rs.getBoolean("voucher_consumed")
                ),
                userId,
                orderNo
        ));
    }

    /**
     * 标记订单优惠券已使用
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 更新的行数
     */
    public int markTextbookOrderVoucherConsumed(Long userId, String orderNo) {
        return jdbcTemplate.update(
                """
                UPDATE academy_textbook_orders
                SET voucher_consumed = 1
                WHERE user_id = ? AND order_no = ? AND voucher_key IS NOT NULL
                """,
                userId,
                orderNo
        );
    }

    /**
     * 查询指定表的分类列表
     *
     * @param tableName 表名
     * @return 分类列表
     */
    public List<AcademyCategoryResponse> findCategories(String tableName) {
        if (!ALLOWED_CATEGORY_TABLES.contains(tableName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的分类来源");
        }
        String sql = "SELECT DISTINCT category FROM " + tableName + " WHERE category IS NOT NULL AND category <> '' ORDER BY category";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyCategoryResponse(rs.getString("category")));
    }

    public List<AcademyCategoryResponse> findManagedCourseCategories(String resourceType) {
        return jdbcTemplate.query(
                """
                SELECT name
                FROM admin_course_categories
                WHERE resource_type = ?
                ORDER BY sort_order ASC, name ASC
                """,
                (rs, rowNum) -> new AcademyCategoryResponse(rs.getString("name")),
                resourceType
        );
    }

    public boolean managedCourseCategoryExists(String resourceType, String name) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM admin_course_categories
                WHERE resource_type = ? AND name = ?
                """,
                Long.class,
                resourceType,
                name
        );
        return count != null && count > 0;
    }

    /**
     * 报名课程（已报名则更新报名时间）
     *
     * @param resourceType 资源类型
     * @param courseId 课程ID
     * @param userId 用户ID
     */
    public void enrollCourse(String resourceType, String courseId, Long userId) {
        String sql = """
                INSERT INTO academy_course_enrollments (resource_type, course_id, user_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
                """;
        jdbcTemplate.update(sql, resourceType, courseId, userId);
    }

    /**
     * 取消课程报名
     *
     * @param resourceType 资源类型
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 删除的行数
     */
    public int unenrollCourse(String resourceType, String courseId, Long userId) {
        String sql = """
                DELETE FROM academy_course_enrollments
                WHERE resource_type = ? AND course_id = ? AND user_id = ?
                """;
        return jdbcTemplate.update(sql, resourceType, courseId, userId);
    }

    /**
     * 查询用户已报名的课程列表
     *
     * @param userId 用户ID
     * @return 已报名课程列表
     */
    public List<AcademyEnrolledCourseResponse> findEnrolledCourses(Long userId) {
        String sql = """
                SELECT enrolled.resource_type, enrolled.external_course_id, enrolled.course_name,
                       enrolled.teacher_name, enrolled.category, enrolled.school_name,
                       enrolled.cover_url, enrolled.cover_file_path, enrolled.start_time,
                       enrolled.participant_count, enrolled.course_comment,
                       enrolled.course_description, enrolled.semester_plan, enrolled.course_overview,
                       enrolled.video_file_path, enrolled.source_url, enrolled.enrolled_at
                FROM (
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           COALESCE(p.course_detail, c.course_description, '') AS course_description,
                           p.semester_plan, p.course_overview, p.video_file_path,
                           c.source_url, e.created_at AS enrolled_at
                    FROM academy_course_enrollments e
                    JOIN online_open_courses c ON c.external_course_id = e.course_id
                    LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                    WHERE e.resource_type = 'online-open-courses' AND e.user_id = ?
                    UNION ALL
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           c.course_description, NULL AS semester_plan, NULL AS course_overview,
                           NULL AS video_file_path, c.source_url, e.created_at AS enrolled_at
                    FROM academy_course_enrollments e
                    JOIN general_courses c ON c.external_course_id = e.course_id
                    WHERE e.resource_type = 'general-courses' AND e.user_id = ?
                    UNION ALL
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           c.course_description, NULL AS semester_plan, NULL AS course_overview,
                           NULL AS video_file_path, c.source_url, e.created_at AS enrolled_at
                    FROM academy_course_enrollments e
                    JOIN micro_major_courses c ON c.external_course_id = e.course_id
                    WHERE e.resource_type = 'micro-major-courses' AND e.user_id = ?
                ) enrolled
                ORDER BY enrolled.enrolled_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyEnrolledCourseResponse(
                rs.getString("resource_type"),
                rs.getString("external_course_id"),
                rs.getString("course_name"),
                rs.getString("teacher_name"),
                rs.getString("category"),
                rs.getString("school_name"),
                null,
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getString("start_time"),
                rs.getObject("participant_count", Integer.class),
                rs.getString("course_comment"),
                rs.getString("course_description"),
                rs.getString("semester_plan"),
                rs.getString("course_overview"),
                fileUrl(rs.getString("video_file_path")),
                rs.getString("video_file_path"),
                rs.getString("source_url"),
                rs.getTimestamp("enrolled_at").toLocalDateTime()
        ), userId, userId, userId);
    }

    /**
     * 查询课程评论列表
     *
     * @param resourceType 资源类型
     * @param courseId 课程ID
     * @return 评论列表
     */
    public List<AcademyCourseReviewResponse> findCourseReviews(String resourceType, String courseId) {
        String sql = """
                SELECT r.id, r.parent_review_id, r.user_id, r.user_name, u.role_type AS user_role_type,
                       r.rating, r.content, r.created_at,
                       r.reply_content, r.reply_user_name, r.reply_user_role_type, r.replied_at
                FROM academy_course_reviews
                r LEFT JOIN users u ON u.id = r.user_id
                WHERE r.resource_type = ? AND r.course_id = ?
                ORDER BY r.created_at ASC, r.id ASC
                """;
        List<CourseReviewRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new CourseReviewRow(
                rs.getLong("id"),
                getNullableLong(rs, "parent_review_id"),
                getNullableLong(rs, "user_id"),
                rs.getString("user_name"),
                rs.getString("user_role_type"),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("reply_content"),
                rs.getString("reply_user_name"),
                rs.getString("reply_user_role_type"),
                toLocalDateTime(rs.getTimestamp("replied_at"))
        ), resourceType, courseId);
        return buildCourseReviewTree(rows);
    }

    /**
     * 保存课程评论
     *
     * @param resourceType 资源类型
     * @param courseId 课程ID
     * @param userName 用户名
     * @param rating 评分
     * @param content 评论内容
     * @return 保存后的评论
     */
    public AcademyCourseReviewResponse saveCourseReview(
            String resourceType,
            String courseId,
            Long userId,
            String userName,
            String userRoleType,
            int rating,
            String content,
            Long parentReviewId
    ) {
        String insertSql = """
                INSERT INTO academy_course_reviews (resource_type, course_id, user_id, user_name, parent_review_id, rating, content)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(insertSql, resourceType, courseId, userId, userName, parentReviewId, rating, content);

        String selectSql = """
                SELECT id, parent_review_id, user_id, user_name, rating, content, created_at,
                       reply_content, reply_user_name, reply_user_role_type, replied_at
                FROM academy_course_reviews
                WHERE resource_type = ? AND course_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> new AcademyCourseReviewResponse(
                rs.getLong("id"),
                getNullableLong(rs, "parent_review_id"),
                getNullableLong(rs, "user_id"),
                rs.getString("user_name"),
                userRoleType,
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("reply_content"),
                rs.getString("reply_user_name"),
                rs.getString("reply_user_role_type"),
                toLocalDateTime(rs.getTimestamp("replied_at")),
                List.of()
        ), resourceType, courseId);
    }

    public boolean courseReviewExists(String resourceType, String courseId, Long reviewId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM academy_course_reviews
                WHERE resource_type = ? AND course_id = ? AND id = ?
                """,
                Long.class,
                resourceType,
                courseId,
                reviewId
        );
        return count != null && count > 0;
    }

    public Optional<AcademyCourseReviewResponse> findCourseReviewById(Long reviewId) {
        String sql = """
                SELECT r.id, r.parent_review_id, r.user_id, r.user_name, u.role_type AS user_role_type,
                       r.rating, r.content, r.created_at,
                       r.reply_content, r.reply_user_name, r.reply_user_role_type, r.replied_at
                FROM academy_course_reviews r
                LEFT JOIN users u ON u.id = r.user_id
                WHERE r.id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AcademyCourseReviewResponse(
                    rs.getLong("id"),
                    getNullableLong(rs, "parent_review_id"),
                    getNullableLong(rs, "user_id"),
                    rs.getString("user_name"),
                    rs.getString("user_role_type"),
                    rs.getInt("rating"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getString("reply_content"),
                    rs.getString("reply_user_name"),
                    rs.getString("reply_user_role_type"),
                    toLocalDateTime(rs.getTimestamp("replied_at")),
                    List.of()
            ), reviewId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public AcademyCourseReviewResponse saveCourseReviewReply(Long parentReviewId, Long userId, String userName, String userRoleType, String content) {
        String insertSql = """
                INSERT INTO academy_course_reviews
                  (resource_type, course_id, user_id, user_name, parent_review_id, rating, content)
                SELECT resource_type, course_id, ?, ?, id, rating, ?
                FROM academy_course_reviews
                WHERE id = ?
                """;
        int inserted = jdbcTemplate.update(insertSql, userId, userName, content, parentReviewId);
        if (inserted <= 0) {
            throw new EmptyResultDataAccessException(1);
        }
        String selectSql = """
                SELECT id
                FROM academy_course_reviews
                WHERE parent_review_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        Long replyId = jdbcTemplate.queryForObject(selectSql, Long.class, parentReviewId);
        return findCourseReviewById(replyId).orElseThrow(() -> new EmptyResultDataAccessException(1));
    }

    public int countTeacherPendingAssignmentReviews(long teacherUserId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM academy_assignment_submissions s
                JOIN academy_assignments a ON a.id = s.assignment_id
                JOIN teacher_published_courses p
                  ON p.course_id = a.course_id AND a.course_resource_type = 'online-open-courses'
                WHERE p.publisher_user_id = ?
                  AND s.submission_status = 'pending_review'
                """,
                Integer.class,
                teacherUserId
        );
        return count == null ? 0 : count;
    }

    public int countTeacherPendingExamReviews(long teacherUserId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM academy_exam_submissions s
                JOIN academy_exams e ON e.id = s.exam_id
                JOIN teacher_published_courses p
                  ON p.course_id = e.course_id AND e.course_resource_type = 'online-open-courses'
                WHERE p.publisher_user_id = ?
                  AND s.submission_status = 'pending_review'
                """,
                Integer.class,
                teacherUserId
        );
        return count == null ? 0 : count;
    }

    public int countTeacherUnreadCourseReviews(long teacherUserId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM academy_course_reviews r
                JOIN teacher_published_courses p
                  ON p.course_id = r.course_id AND r.resource_type = 'online-open-courses'
                WHERE p.publisher_user_id = ?
                  AND r.teacher_read_at IS NULL
                  AND COALESCE(r.user_id, 0) <> ?
                """,
                Integer.class,
                teacherUserId,
                teacherUserId
        );
        return count == null ? 0 : count;
    }

    public List<TeacherMailboxMessageResponse> findTeacherMailboxMessages(long teacherUserId) {
        return jdbcTemplate.query(
                """
                SELECT r.id, r.parent_review_id, r.course_id,
                       COALESCE(c.course_name, r.course_id) AS course_title,
                       r.user_id, r.user_name, u.role_type AS user_role_type,
                       r.content, r.created_at, r.teacher_read_at
                FROM academy_course_reviews r
                JOIN teacher_published_courses p
                  ON p.course_id = r.course_id AND r.resource_type = 'online-open-courses'
                LEFT JOIN online_open_courses c ON c.external_course_id = r.course_id
                LEFT JOIN users u ON u.id = r.user_id
                WHERE p.publisher_user_id = ?
                  AND COALESCE(r.user_id, 0) <> ?
                ORDER BY r.teacher_read_at IS NULL DESC, r.created_at DESC, r.id DESC
                LIMIT 30
                """,
                (rs, rowNum) -> new TeacherMailboxMessageResponse(
                        rs.getLong("id"),
                        getNullableLong(rs, "parent_review_id"),
                        rs.getString("course_id"),
                        rs.getString("course_title"),
                        getNullableLong(rs, "user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_role_type"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("teacher_read_at") == null
                ),
                teacherUserId,
                teacherUserId
        );
    }

    public void markTeacherMailboxRead(long teacherUserId) {
        jdbcTemplate.update(
                """
                UPDATE academy_course_reviews r
                JOIN teacher_published_courses p
                  ON p.course_id = r.course_id AND r.resource_type = 'online-open-courses'
                SET r.teacher_read_at = COALESCE(r.teacher_read_at, CURRENT_TIMESTAMP)
                WHERE p.publisher_user_id = ?
                  AND COALESCE(r.user_id, 0) <> ?
                  AND r.teacher_read_at IS NULL
                """,
                teacherUserId,
                teacherUserId
        );
    }

    public record PublishedCourseOwnerRow(
            String courseId,
            String courseName,
            String teacherName
    ) {
    }

    /**
     * 查询学习课程列表（通用方法）
     *
     * @param tableName 表名
     * @return 课程列表
     */
    private List<AcademyCourseResponse> findLearningCourses(String tableName) {
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       course_comment, course_description, source_url, certified, certification_label
                FROM %s
                ORDER BY id ASC
                """.formatted(tableName);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyCourseResponse(
                rs.getString("external_course_id"),
                rs.getString("course_name"),
                rs.getString("teacher_name"),
                rs.getString("category"),
                rs.getString("school_name"),
                null,
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getString("start_time"),
                rs.getObject("participant_count", Integer.class),
                rs.getString("course_comment"),
                rs.getString("course_description"),
                null,
                null,
                "",
                null,
                rs.getString("source_url"),
                getOptionalBoolean(rs, "certified"),
                getOptionalString(rs, "certification_label")
        ));
    }

    /**
     * 根据ID查询学习课程详情（通用方法）
     *
     * @param tableName 表名
     * @param emptyDescription 是否清空描述
     * @param id 课程ID
     * @return 课程详情，不存在则返回空
     */
    private Optional<AcademyCourseResponse> findLearningCourseById(
            String tableName,
            boolean emptyDescription,
            String id
    ) {
        String descriptionExpression = emptyDescription ? "'' AS course_description" : "course_description";
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       course_comment, %s, source_url, certified, certification_label
                FROM %s
                WHERE external_course_id = ?
                LIMIT 1
                """.formatted(descriptionExpression, tableName);

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AcademyCourseResponse(
                    rs.getString("external_course_id"),
                    rs.getString("course_name"),
                    rs.getString("teacher_name"),
                    rs.getString("category"),
                    rs.getString("school_name"),
                    null,
                    rs.getString("cover_url"),
                    rs.getString("cover_file_path"),
                    rs.getString("start_time"),
                    rs.getObject("participant_count", Integer.class),
                    rs.getString("course_comment"),
                    rs.getString("course_description"),
                    null,
                    null,
                    "",
                    null,
                    rs.getString("source_url"),
                    getOptionalBoolean(rs, "certified"),
                    getOptionalString(rs, "certification_label")
            ), id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取字符串值（异常时返回null）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @return 字符串值，异常则返回null
     */
    private String getOptionalString(java.sql.ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 安全获取布尔值（异常时返回false）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @return 布尔值，异常则返回false
     */
    private boolean getOptionalBoolean(java.sql.ResultSet rs, String columnLabel) {
        try {
            return rs.getBoolean(columnLabel);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 安全获取BigDecimal值（异常时返回默认值）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @param fallback 默认值
     * @return BigDecimal值，异常则返回默认值
     */
    private BigDecimal getOptionalBigDecimal(java.sql.ResultSet rs, String columnLabel, BigDecimal fallback) {
        try {
            BigDecimal value = rs.getBigDecimal(columnLabel);
            return value == null ? fallback : value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    /**
     * 安全获取整数（异常时返回默认值）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @param fallback 默认值
     * @return 整数值，异常则返回默认值
     */
    private Integer getOptionalInt(java.sql.ResultSet rs, String columnLabel, Integer fallback) {
        try {
            int value = rs.getInt(columnLabel);
            return rs.wasNull() ? fallback : value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Long getNullableLong(java.sql.ResultSet rs, String columnLabel) throws java.sql.SQLException {
        long value = rs.getLong(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private List<AcademyCourseReviewResponse> buildCourseReviewTree(List<CourseReviewRow> rows) {
        Map<Long, List<CourseReviewRow>> childrenByParent = new LinkedHashMap<>();
        for (CourseReviewRow row : rows) {
            childrenByParent.computeIfAbsent(row.parentReviewId(), ignored -> new ArrayList<>()).add(row);
        }
        List<CourseReviewRow> roots = childrenByParent.getOrDefault(null, List.of());
        List<AcademyCourseReviewResponse> result = new ArrayList<>();
        for (int index = roots.size() - 1; index >= 0; index--) {
            result.add(toCourseReviewResponse(roots.get(index), childrenByParent));
        }
        return result;
    }

    private AcademyCourseReviewResponse toCourseReviewResponse(
            CourseReviewRow row,
            Map<Long, List<CourseReviewRow>> childrenByParent
    ) {
        List<CourseReviewRow> children = childrenByParent.getOrDefault(row.id(), List.of());
        List<AcademyCourseReviewResponse> replies = new ArrayList<>();
        for (CourseReviewRow child : children) {
            replies.add(toCourseReviewResponse(child, childrenByParent));
        }
        return new AcademyCourseReviewResponse(
                row.id(),
                row.parentReviewId(),
                row.userId(),
                row.userName(),
                row.userRoleType(),
                row.rating(),
                row.content(),
                row.createdAt(),
                row.replyContent(),
                row.replyUserName(),
                row.replyUserRoleType(),
                row.repliedAt(),
                replies
        );
    }

    /**
     * 解析目录文本为字符串列表
     *
     * @param value 目录文本
     * @return 目录列表
     */
    private List<String> parseCatalog(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * 解析评论文本为评论列表
     *
     * @param value 评论文本
     * @return 评论列表
     */
    private List<AcademyTextbookCommentResponse> parseComments(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    String[] parts = line.split("\\|", 3);
                    String user = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "平台用户";
                    int rating = 5;
                    if (parts.length > 1) {
                        try {
                            rating = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            rating = 5;
                        }
                    }
                    String content = parts.length > 2 ? parts[2] : line;
                    return new AcademyTextbookCommentResponse(user, rating, content, null, null, null);
                })
                .toList();
    }

    public record AcademyTextbookOrderResponseData(
            String orderNo,
            BigDecimal totalAmount,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String voucherKey,
            String voucherName,
            boolean voucherConsumed
    ) {
    }

    public record TextbookPaymentSessionData(
            String sessionId,
            String orderNo,
            String gatewayOrderNo,
            Long userId,
            String provider,
            BigDecimal amount,
            String status,
            String qrPayload,
            LocalDateTime expiresAt,
            LocalDateTime paidAt
    ) {
    }

    /**
     * 解析教材优惠券折扣
     *
     * @param voucherKey 优惠券Key
     * @param voucherName 优惠券名称
     * @param voucherDiscountAmount 折扣金额
     * @return 优惠券折扣信息
     */
    private TextbookVoucherDiscount resolveTextbookVoucherDiscount(
            String voucherKey,
            String voucherName,
            BigDecimal voucherDiscountAmount
    ) {
        if (voucherKey == null || voucherKey.isBlank()) {
            return new TextbookVoucherDiscount(BigDecimal.ZERO, null, null);
        }
        BigDecimal discountAmount = voucherDiscountAmount == null ? BigDecimal.ZERO : voucherDiscountAmount.max(BigDecimal.ZERO);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new TextbookVoucherDiscount(BigDecimal.ZERO, null, null);
        }
        return new TextbookVoucherDiscount(discountAmount, voucherKey, voucherName);
    }

    private record TextbookVoucherDiscount(
            BigDecimal discountAmount,
            String voucherKey,
            String voucherName
    ) {
    }

    private record TextbookCartOrderItem(
            Long id,
            String textbookId,
            String textbookName,
            BigDecimal unitPrice,
            Integer quantity
    ) {
    }

    private record CourseReviewRow(
            Long id,
            Long parentReviewId,
            Long userId,
            String userName,
            String userRoleType,
            int rating,
            String content,
            LocalDateTime createdAt,
            String replyContent,
            String replyUserName,
            String replyUserRoleType,
            LocalDateTime repliedAt
    ) {
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 将文件路径转换为访问URL
     *
     * @param filePath 文件路径
     * @return 访问URL
     */
    private String fileUrl(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        String normalizedPath = filePath.replace("\\", "/");
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
