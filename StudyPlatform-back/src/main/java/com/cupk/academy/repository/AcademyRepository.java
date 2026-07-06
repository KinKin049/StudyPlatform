package com.cupk.academy.repository;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyTextbookCommentResponse;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyTextbookCartItemResponse;
import com.cupk.academy.dto.AcademyTextbookDetailResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class AcademyRepository {
    private final JdbcTemplate jdbcTemplate;

    public AcademyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AcademyCourseResponse> findOnlineOpenCourses() {
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       COALESCE(p.course_overview, course_comment) AS course_comment,
                       COALESCE(p.course_detail, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       source_url, c.certified
                FROM online_open_courses c
                LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                ORDER BY c.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAcademyCourse(rs));
    }

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
                getOptionalBoolean(rs, "certified")
        );
    }

    public Optional<AcademyCourseResponse> findOnlineOpenCourseById(String id) {
        String sql = """
                SELECT c.external_course_id, c.course_name, c.teacher_name, c.category, c.school_name,
                       c.cover_url, c.cover_file_path, c.start_time, c.participant_count,
                       COALESCE(p.course_overview, c.course_comment) AS course_comment,
                       COALESCE(p.course_detail, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       c.source_url, c.certified
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

    public List<AcademyCourseResponse> findPublishedOnlineOpenCourses(long publisherUserId) {
        String sql = """
                SELECT c.external_course_id, c.course_name, c.teacher_name, c.category, c.school_name,
                       c.cover_url, c.cover_file_path, c.start_time, c.participant_count,
                       COALESCE(p.course_overview, c.course_comment) AS course_comment,
                       COALESCE(p.course_detail, '') AS course_description,
                       p.semester_plan, p.course_overview, p.video_file_path,
                       c.source_url, c.certified
                FROM teacher_published_courses p
                JOIN online_open_courses c ON c.external_course_id = p.course_id
                WHERE p.publisher_user_id = ?
                ORDER BY p.updated_at DESC, p.id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAcademyCourse(rs), publisherUserId);
    }

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

    public List<AcademyCourseResponse> findGeneralCourses() {
        return findLearningCourses("general_courses");
    }

    public Optional<AcademyCourseResponse> findGeneralCourseById(String id) {
        return findLearningCourseById("general_courses", false, id);
    }

    public List<AcademyCourseResponse> findMicroMajorCourses() {
        return findLearningCourses("micro_major_courses");
    }

    public Optional<AcademyCourseResponse> findMicroMajorCourseById(String id) {
        return findLearningCourseById("micro_major_courses", false, id);
    }

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

    public List<AcademyTextbookCommentResponse> findTextbookReviews(String textbookId) {
        String sql = """
                SELECT user_name, rating, content
                FROM academy_textbook_reviews
                WHERE textbook_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyTextbookCommentResponse(
                rs.getString("user_name"),
                rs.getInt("rating"),
                rs.getString("content")
        ), textbookId);
    }

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
                SELECT user_name, rating, content
                FROM academy_textbook_reviews
                WHERE user_id = ? AND textbook_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new AcademyTextbookCommentResponse(
                        rs.getString("user_name"),
                        rs.getInt("rating"),
                        rs.getString("content")
                ),
                userId,
                textbookId
        );
    }

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

    public int deleteTextbookCartItem(Long userId, Long itemId) {
        return jdbcTemplate.update(
                "DELETE FROM academy_textbook_cart_items WHERE user_id = ? AND id = ?",
                userId,
                itemId
        );
    }

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

    public AcademyTextbookOrderResponseData createTextbookOrder(
            Long userId,
            AcademyTextbookDetailResponse textbook,
            Integer quantity
    ) {
        String orderNo = "TB" + System.currentTimeMillis() + userId;
        BigDecimal unitPrice = textbook.discountPrice() == null ? BigDecimal.ZERO : textbook.discountPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_orders (user_id, order_no, total_amount, order_status)
                VALUES (?, ?, ?, '待支付')
                """,
                userId,
                orderNo,
                totalAmount
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
        return new AcademyTextbookOrderResponseData(orderNo, totalAmount);
    }

    public AcademyTextbookOrderResponseData createTextbookOrderFromCart(
            Long userId,
            List<Long> cartItemIds
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
            return new AcademyTextbookOrderResponseData("", BigDecimal.ZERO);
        }

        BigDecimal totalAmount = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String orderNo = "TB" + System.currentTimeMillis() + userId;
        jdbcTemplate.update(
                """
                INSERT INTO academy_textbook_orders (user_id, order_no, total_amount, order_status)
                VALUES (?, ?, ?, '待支付')
                """,
                userId,
                orderNo,
                totalAmount
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

        Object[] deleteParams = new Object[cartItemIds.size() + 1];
        deleteParams[0] = userId;
        for (int index = 0; index < cartItemIds.size(); index += 1) {
            deleteParams[index + 1] = cartItemIds.get(index);
        }
        jdbcTemplate.update(
                "DELETE FROM academy_textbook_cart_items WHERE user_id = ? AND id IN (%s)".formatted(placeholders),
                deleteParams
        );
        return new AcademyTextbookOrderResponseData(orderNo, totalAmount);
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
                SELECT order_no, total_amount
                FROM academy_textbook_orders
                WHERE user_id = ? AND order_no = ?
                LIMIT 1
                """,
                (rs, rowNum) -> new AcademyTextbookOrderResponseData(
                        rs.getString("order_no"),
                        rs.getBigDecimal("total_amount")
                ),
                userId,
                orderNo
        ));
    }

    public List<AcademyCategoryResponse> findCategories(String tableName) {
        String sql = "SELECT DISTINCT category FROM " + tableName + " WHERE category IS NOT NULL AND category <> '' ORDER BY category";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyCategoryResponse(rs.getString("category")));
    }

    public void enrollCourse(String resourceType, String courseId, Long userId) {
        String sql = """
                INSERT INTO academy_course_enrollments (resource_type, course_id, user_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
                """;
        jdbcTemplate.update(sql, resourceType, courseId, userId);
    }

    public int unenrollCourse(String resourceType, String courseId, Long userId) {
        String sql = """
                DELETE FROM academy_course_enrollments
                WHERE resource_type = ? AND course_id = ? AND user_id = ?
                """;
        return jdbcTemplate.update(sql, resourceType, courseId, userId);
    }

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
                           COALESCE(p.course_detail, '') AS course_description,
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

    public List<AcademyCourseReviewResponse> findCourseReviews(String resourceType, String courseId) {
        String sql = """
                SELECT id, user_name, rating, content, created_at
                FROM academy_course_reviews
                WHERE resource_type = ? AND course_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AcademyCourseReviewResponse(
                rs.getLong("id"),
                rs.getString("user_name"),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), resourceType, courseId);
    }

    public AcademyCourseReviewResponse saveCourseReview(
            String resourceType,
            String courseId,
            String userName,
            int rating,
            String content
    ) {
        String insertSql = """
                INSERT INTO academy_course_reviews (resource_type, course_id, user_name, rating, content)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(insertSql, resourceType, courseId, userName, rating, content);

        String selectSql = """
                SELECT id, user_name, rating, content, created_at
                FROM academy_course_reviews
                WHERE resource_type = ? AND course_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> new AcademyCourseReviewResponse(
                rs.getLong("id"),
                rs.getString("user_name"),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), resourceType, courseId);
    }

    private List<AcademyCourseResponse> findLearningCourses(String tableName) {
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       course_comment, course_description, source_url, certified
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
                getOptionalBoolean(rs, "certified")
        ));
    }

    private Optional<AcademyCourseResponse> findLearningCourseById(
            String tableName,
            boolean emptyDescription,
            String id
    ) {
        String descriptionExpression = emptyDescription ? "'' AS course_description" : "course_description";
        String sql = """
                SELECT external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count,
                       course_comment, %s, source_url, certified
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
                    getOptionalBoolean(rs, "certified")
            ), id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String getOptionalString(java.sql.ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean getOptionalBoolean(java.sql.ResultSet rs, String columnLabel) {
        try {
            return rs.getBoolean(columnLabel);
        } catch (Exception ex) {
            return false;
        }
    }

    private BigDecimal getOptionalBigDecimal(java.sql.ResultSet rs, String columnLabel, BigDecimal fallback) {
        try {
            BigDecimal value = rs.getBigDecimal(columnLabel);
            return value == null ? fallback : value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer getOptionalInt(java.sql.ResultSet rs, String columnLabel, Integer fallback) {
        try {
            int value = rs.getInt(columnLabel);
            return rs.wasNull() ? fallback : value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private List<String> parseCatalog(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

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
                    return new AcademyTextbookCommentResponse(user, rating, content);
                })
                .toList();
    }

    public record AcademyTextbookOrderResponseData(
            String orderNo,
            BigDecimal totalAmount
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
