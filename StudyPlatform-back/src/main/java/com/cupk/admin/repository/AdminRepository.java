package com.cupk.admin.repository;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminVoucherItemRequest;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<AdminAuthRow> findAuthRow(long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id,
                           COALESCE(NULLIF(nickname, ''), username) AS username,
                           email,
                           COALESCE(NULLIF(role_type, ''),
                             CASE WHEN role = 'TEACHER' THEN 'teacher'
                                  WHEN role = 'ADMIN' THEN 'admin'
                                  ELSE 'student' END
                           ) AS role_type
                    FROM users
                    WHERE id = ?
                    LIMIT 1
                    """,
                    (rs, rowNum) -> new AdminAuthRow(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role_type")
                    ),
                    userId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<AdminUserResponse> findUsers() {
        return jdbcTemplate.query(
                """
                SELECT u.id,
                       COALESCE(NULLIF(u.nickname, ''), u.username) AS username,
                       u.email,
                       COALESCE(NULLIF(u.role_type, ''),
                         CASE WHEN u.role = 'TEACHER' THEN 'teacher'
                              WHEN u.role = 'ADMIN' THEN 'admin'
                              ELSE 'student' END
                       ) AS role_type,
                       u.learning_goal, u.school, u.teacher_name, u.onboarding_completed,
                       COALESCE(r.reward_total, 0) - COALESCE(s.spend_total, 0) + COALESCE(p.admin_coin_adjustment, 0) AS coin_total,
                       COALESCE(p.admin_coin_adjustment, 0) AS admin_coin_adjustment,
                       COALESCE(p.admin_data_note, '') AS admin_data_note
                FROM users u
                LEFT JOIN profile_user_profiles p ON p.user_id = u.id
                LEFT JOIN (
                  SELECT user_id, SUM(amount) AS reward_total
                  FROM coin_reward_records
                  GROUP BY user_id
                ) r ON r.user_id = u.id
                LEFT JOIN (
                  SELECT user_id, SUM(amount) AS spend_total
                  FROM coin_spend_records
                  GROUP BY user_id
                ) s ON s.user_id = u.id
                ORDER BY CASE WHEN u.email = 'admin@admin.com' THEN 0 ELSE 1 END, u.id ASC
                """,
                this::mapUser
        );
    }

    public Optional<AdminUserResponse> findUser(long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT u.id,
                           COALESCE(NULLIF(u.nickname, ''), u.username) AS username,
                           u.email,
                           COALESCE(NULLIF(u.role_type, ''),
                             CASE WHEN u.role = 'TEACHER' THEN 'teacher'
                                  WHEN u.role = 'ADMIN' THEN 'admin'
                                  ELSE 'student' END
                           ) AS role_type,
                           u.learning_goal, u.school, u.teacher_name, u.onboarding_completed,
                           COALESCE(r.reward_total, 0) - COALESCE(s.spend_total, 0) + COALESCE(p.admin_coin_adjustment, 0) AS coin_total,
                           COALESCE(p.admin_coin_adjustment, 0) AS admin_coin_adjustment,
                           COALESCE(p.admin_data_note, '') AS admin_data_note
                    FROM users u
                    LEFT JOIN profile_user_profiles p ON p.user_id = u.id
                    LEFT JOIN (
                      SELECT user_id, SUM(amount) AS reward_total
                      FROM coin_reward_records
                      GROUP BY user_id
                    ) r ON r.user_id = u.id
                    LEFT JOIN (
                      SELECT user_id, SUM(amount) AS spend_total
                      FROM coin_spend_records
                      GROUP BY user_id
                    ) s ON s.user_id = u.id
                    WHERE u.id = ?
                    LIMIT 1
                    """,
                    this::mapUser,
                    userId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean emailBelongsToOtherUser(String email, long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?",
                Long.class,
                email,
                userId
        );
        return count != null && count > 0;
    }

    public void updateUser(
            long userId,
            String username,
            String email,
            String roleType,
            String learningGoal,
            String school,
            String teacherName,
            Long coinAdjustment,
            String dataNote,
            String passwordHash
    ) {
        String displayName = displayName(username, roleType, teacherName);
        String platformUsername = platformUsername(username, userId);
        String platformRole = platformRole(roleType);
        if (passwordHash == null || passwordHash.isBlank()) {
            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET username = ?, nickname = ?, email = ?, role_type = ?, role = ?,
                        learning_goal = ?, school = ?, teacher_name = ?, onboarding_completed = 1
                    WHERE id = ?
                    """,
                    platformUsername,
                    displayName,
                    email,
                    roleType,
                    platformRole,
                    learningGoal,
                    school,
                    teacherName,
                    userId
            );
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET username = ?, nickname = ?, email = ?, password_hash = ?, role_type = ?, role = ?,
                        learning_goal = ?, school = ?, teacher_name = ?, onboarding_completed = 1
                    WHERE id = ?
                    """,
                    platformUsername,
                    displayName,
                    email,
                    passwordHash,
                    roleType,
                    platformRole,
                    learningGoal,
                    school,
                    teacherName,
                    userId
            );
        }

        jdbcTemplate.update(
                """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, school,
                   admin_coin_adjustment, admin_data_note)
                VALUES (?, ?, ?, ?, ?, 'China', ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  display_name = VALUES(display_name),
                  handle = VALUES(handle),
                  role_label = VALUES(role_label),
                  bio = VALUES(bio),
                  school = VALUES(school),
                  admin_coin_adjustment = VALUES(admin_coin_adjustment),
                  admin_data_note = VALUES(admin_data_note)
                """,
                userId,
                displayName,
                "@" + username,
                "teacher".equals(roleType) ? "教师" : "学生",
                "teacher".equals(roleType) ? "教师：" + teacherName : "目标：" + learningGoal,
                school,
                coinAdjustment == null ? 0L : coinAdjustment,
                dataNote
        );
    }

    public int deleteUser(long userId) {
        jdbcTemplate.update("DELETE FROM academy_course_enrollments WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM course_question_bank_mistakes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM course_question_bank_favorites WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_learning_events WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_learning_time_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM game_ladder_jump_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM game_type_warrior_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_vouchers WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM coin_spend_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM coin_reward_records WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM teacher_published_courses WHERE publisher_user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_user_profiles WHERE user_id = ?", userId);
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    public List<AdminCourseResponse> findCourses(String resourceType) {
        return jdbcTemplate.query(courseSelectSql(resourceType, false), this::mapCourse);
    }

    public Optional<AdminCourseResponse> findCourse(String resourceType, String courseId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    courseSelectSql(resourceType, true),
                    this::mapCourse,
                    courseId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void upsertCourse(AdminCourseRequest request) {
        String resourceType = request.resourceType();
        if ("online-open-courses".equals(resourceType)) {
            jdbcTemplate.update(
                    """
                    INSERT INTO online_open_courses
                      (external_course_id, course_name, teacher_name, category, school_name,
                       cover_url, cover_file_path, start_time, participant_count, course_comment,
                       source_url, certified)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      course_name = VALUES(course_name),
                      teacher_name = VALUES(teacher_name),
                      category = VALUES(category),
                      school_name = VALUES(school_name),
                      cover_url = VALUES(cover_url),
                      cover_file_path = VALUES(cover_file_path),
                      start_time = VALUES(start_time),
                      participant_count = VALUES(participant_count),
                      course_comment = VALUES(course_comment),
                      source_url = VALUES(source_url),
                      certified = VALUES(certified)
                    """,
                    request.id(),
                    request.name(),
                    request.teacher(),
                    request.category(),
                    request.school(),
                    request.coverUrl(),
                    request.coverFilePath(),
                    request.startTime(),
                    request.participants() == null ? 0 : request.participants(),
                    request.comment(),
                    request.link(),
                    Boolean.TRUE.equals(request.certified()) ? 1 : 0
            );
            jdbcTemplate.update(
                    """
                    INSERT INTO teacher_published_courses
                      (course_id, publisher_user_id, semester_plan, course_overview, course_detail, video_file_path)
                    VALUES (?, 0, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      semester_plan = VALUES(semester_plan),
                      course_overview = VALUES(course_overview),
                      course_detail = VALUES(course_detail),
                      video_file_path = VALUES(video_file_path)
                    """,
                    request.id(),
                    request.semesterPlan(),
                    request.overview(),
                    request.description(),
                    request.videoFilePath()
            );
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO %s
                  (external_course_id, course_name, teacher_name, category, school_name,
                   cover_url, cover_file_path, start_time, participant_count, course_comment,
                   course_description, source_url, certified)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  course_name = VALUES(course_name),
                  teacher_name = VALUES(teacher_name),
                  category = VALUES(category),
                  school_name = VALUES(school_name),
                  cover_url = VALUES(cover_url),
                  cover_file_path = VALUES(cover_file_path),
                  start_time = VALUES(start_time),
                  participant_count = VALUES(participant_count),
                  course_comment = VALUES(course_comment),
                  course_description = VALUES(course_description),
                  source_url = VALUES(source_url),
                  certified = VALUES(certified)
                """.formatted(courseTable(resourceType)),
                request.id(),
                request.name(),
                request.teacher(),
                request.category(),
                request.school(),
                request.coverUrl(),
                request.coverFilePath(),
                request.startTime(),
                request.participants() == null ? 0 : request.participants(),
                request.comment(),
                request.description(),
                request.link(),
                Boolean.TRUE.equals(request.certified()) ? 1 : 0
        );
    }

    public int deleteCourse(String resourceType, String courseId) {
        String tableName = courseTable(resourceType);
        jdbcTemplate.update("DELETE FROM academy_course_reviews WHERE resource_type = ? AND course_id = ?", resourceType, courseId);
        jdbcTemplate.update("DELETE FROM academy_course_enrollments WHERE resource_type = ? AND course_id = ?", resourceType, courseId);
        if ("online-open-courses".equals(resourceType)) {
            jdbcTemplate.update("DELETE FROM teacher_published_courses WHERE course_id = ?", courseId);
        }
        return jdbcTemplate.update("DELETE FROM " + tableName + " WHERE external_course_id = ?", courseId);
    }

    public List<AdminCourseReviewResponse> findReviews() {
        return jdbcTemplate.query(
                """
                SELECT id, resource_type, course_id, user_name, rating, content, created_at
                FROM academy_course_reviews
                ORDER BY created_at DESC, id DESC
                LIMIT 300
                """,
                (rs, rowNum) -> new AdminCourseReviewResponse(
                        rs.getLong("id"),
                        rs.getString("resource_type"),
                        rs.getString("course_id"),
                        rs.getString("user_name"),
                        rs.getInt("rating"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }

    public int deleteReview(long reviewId) {
        return jdbcTemplate.update("DELETE FROM academy_course_reviews WHERE id = ?", reviewId);
    }

    public List<CourseQuestionBankSetResponse> findQuestionBankSets() {
        return jdbcTemplate.query(
                """
                SELECT c.category_code, c.category_name, c.description AS category_description,
                       s.id, s.set_code, s.title, s.subtitle, s.description, s.cover_url, s.cover_file_path,
                       s.difficulty_label, s.status_label, s.source_name, s.source_url,
                       CAST(s.source_refs AS CHAR) AS source_refs, s.route_path,
                       (
                         SELECT COUNT(*)
                         FROM course_question_bank_questions q
                         WHERE q.set_id = s.id
                       ) AS question_count
                FROM course_question_bank_sets s
                JOIN course_question_bank_categories c ON c.id = s.category_id
                ORDER BY c.sort_order ASC, s.sort_order ASC, s.id ASC
                """,
                this::mapQuestionBankSet
        );
    }

    public void upsertQuestionBankSet(AdminQuestionBankSetRequest request) {
        jdbcTemplate.update(
                """
                INSERT INTO course_question_bank_categories
                  (category_code, category_name, description, sort_order)
                VALUES (?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE
                  category_name = VALUES(category_name),
                  description = VALUES(description)
                """,
                request.categoryCode(),
                request.categoryName(),
                request.categoryDescription()
        );
        jdbcTemplate.update(
                """
                INSERT INTO course_question_bank_sets
                  (category_id, set_code, title, subtitle, description, cover_url, cover_file_path,
                   difficulty_label, status_label, source_name, source_url, source_refs, route_path, sort_order)
                SELECT id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?
                FROM course_question_bank_categories
                WHERE category_code = ?
                ON DUPLICATE KEY UPDATE
                  category_id = VALUES(category_id),
                  title = VALUES(title),
                  subtitle = VALUES(subtitle),
                  description = VALUES(description),
                  cover_url = VALUES(cover_url),
                  cover_file_path = VALUES(cover_file_path),
                  difficulty_label = VALUES(difficulty_label),
                  status_label = VALUES(status_label),
                  source_name = VALUES(source_name),
                  source_url = VALUES(source_url),
                  source_refs = VALUES(source_refs),
                  route_path = VALUES(route_path),
                  sort_order = VALUES(sort_order)
                """,
                request.code(),
                request.title(),
                request.subtitle(),
                request.description(),
                request.coverUrl(),
                request.coverFilePath(),
                request.difficultyLabel(),
                request.statusLabel(),
                request.sourceName(),
                request.sourceUrl(),
                toJson(request.sourceRefs()),
                request.routePath(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.categoryCode()
        );
    }

    public int deleteQuestionBankSet(String setCode) {
        return jdbcTemplate.update("DELETE FROM course_question_bank_sets WHERE set_code = ?", setCode);
    }

    public List<CourseQuestionBankQuestionResponse> findQuestions(String setCode) {
        return jdbcTemplate.query(
                """
                SELECT q.id, q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url, 0 AS favorite
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE s.set_code = ?
                ORDER BY q.sort_order ASC, q.id ASC
                LIMIT 500
                """,
                this::mapQuestion,
                setCode
        );
    }

    public long upsertQuestion(Long questionId, AdminQuestionRequest request) {
        if (questionId == null) {
            jdbcTemplate.update(
                    """
                    INSERT INTO course_question_bank_questions
                      (set_id, question_type, stem, options_json, answer, explanation,
                       difficulty_label, source_url, sort_order)
                    SELECT id, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?
                    FROM course_question_bank_sets
                    WHERE set_code = ?
                    """,
                    request.type(),
                    request.stem(),
                    toJson(request.options()),
                    request.answer(),
                    request.explanation(),
                    request.difficultyLabel(),
                    request.sourceUrl(),
                    request.sortOrder() == null ? 0 : request.sortOrder(),
                    request.setCode()
            );
            Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return id == null ? 0L : id;
        }

        jdbcTemplate.update(
                """
                UPDATE course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.set_code = ?
                SET q.set_id = s.id,
                    q.question_type = ?,
                    q.stem = ?,
                    q.options_json = CAST(? AS JSON),
                    q.answer = ?,
                    q.explanation = ?,
                    q.difficulty_label = ?,
                    q.source_url = ?,
                    q.sort_order = ?
                WHERE q.id = ?
                """,
                request.setCode(),
                request.type(),
                request.stem(),
                toJson(request.options()),
                request.answer(),
                request.explanation(),
                request.difficultyLabel(),
                request.sourceUrl(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                questionId
        );
        return questionId;
    }

    public int deleteQuestion(long questionId) {
        return jdbcTemplate.update("DELETE FROM course_question_bank_questions WHERE id = ?", questionId);
    }

    public List<VoucherItemResponse> findVoucherItems() {
        return jdbcTemplate.query(
                """
                SELECT id, voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                       discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                       valid_from, valid_until, enabled, sort_order
                FROM voucher_items
                ORDER BY enabled DESC, voucher_type ASC, sort_order ASC, id ASC
                """,
                this::mapVoucherItem
        );
    }

    public Optional<VoucherItemResponse> findVoucherItem(String voucherKey) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                           discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                           valid_from, valid_until, enabled, sort_order
                    FROM voucher_items
                    WHERE voucher_key = ?
                    LIMIT 1
                    """,
                    this::mapVoucherItem,
                    voucherKey
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void upsertVoucherItem(AdminVoucherItemRequest request) {
        jdbcTemplate.update(
                """
                INSERT INTO voucher_items
                  (voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                   discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                   valid_from, valid_until, enabled, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  voucher_type = VALUES(voucher_type),
                  name = VALUES(name),
                  description = VALUES(description),
                  price = VALUES(price),
                  stock_quantity = VALUES(stock_quantity),
                  unlimited_stock = VALUES(unlimited_stock),
                  discount_type = VALUES(discount_type),
                  threshold_amount = VALUES(threshold_amount),
                  discount_amount = VALUES(discount_amount),
                  discount_rate = VALUES(discount_rate),
                  max_discount_amount = VALUES(max_discount_amount),
                  valid_from = VALUES(valid_from),
                  valid_until = VALUES(valid_until),
                  enabled = VALUES(enabled),
                  sort_order = VALUES(sort_order)
                """,
                request.voucherKey(),
                request.voucherType(),
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                Boolean.TRUE.equals(request.unlimitedStock()) ? 1 : 0,
                request.discountType(),
                request.thresholdAmount(),
                request.discountAmount(),
                request.discountRate(),
                request.maxDiscountAmount(),
                request.validFrom(),
                request.validUntil(),
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0,
                request.sortOrder()
        );
    }

    public int disableVoucherItem(String voucherKey) {
        return jdbcTemplate.update("UPDATE voucher_items SET enabled = 0 WHERE voucher_key = ?", voucherKey);
    }

    private AdminUserResponse mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new AdminUserResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("role_type"),
                rs.getString("learning_goal"),
                rs.getString("school"),
                rs.getString("teacher_name"),
                rs.getLong("coin_total"),
                rs.getLong("admin_coin_adjustment"),
                rs.getString("admin_data_note"),
                rs.getBoolean("onboarding_completed")
        );
    }

    private AdminCourseResponse mapCourse(ResultSet rs, int rowNum) throws SQLException {
        return new AdminCourseResponse(
                rs.getString("resource_type"),
                rs.getString("external_course_id"),
                rs.getString("course_name"),
                rs.getString("teacher_name"),
                rs.getString("category"),
                rs.getString("school_name"),
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getString("start_time"),
                rs.getObject("participant_count", Integer.class),
                rs.getString("course_comment"),
                rs.getString("course_description"),
                getOptionalString(rs, "semester_plan"),
                getOptionalString(rs, "course_overview"),
                getOptionalString(rs, "video_file_path"),
                rs.getString("source_url"),
                rs.getBoolean("certified")
        );
    }

    private CourseQuestionBankSetResponse mapQuestionBankSet(ResultSet rs, int rowNum) throws SQLException {
        return new CourseQuestionBankSetResponse(
                rs.getLong("id"),
                rs.getString("set_code"),
                rs.getString("title"),
                rs.getString("subtitle"),
                rs.getString("description"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("cover_url"),
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getInt("question_count"),
                rs.getString("difficulty_label"),
                rs.getString("status_label"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                parseStringList(rs.getString("source_refs")),
                rs.getString("route_path")
        );
    }

    private CourseQuestionBankQuestionResponse mapQuestion(ResultSet rs, int rowNum) throws SQLException {
        return new CourseQuestionBankQuestionResponse(
                rs.getLong("id"),
                rs.getString("question_type"),
                rs.getString("stem"),
                parseStringList(rs.getString("options_json")),
                rs.getString("answer"),
                rs.getString("explanation"),
                rs.getString("difficulty_label"),
                rs.getString("source_url"),
                rs.getBoolean("favorite")
        );
    }

    private VoucherItemResponse mapVoucherItem(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherItemResponse(
                rs.getLong("id"),
                rs.getString("voucher_key"),
                rs.getString("voucher_type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("price"),
                (Integer) rs.getObject("stock_quantity"),
                rs.getBoolean("unlimited_stock"),
                rs.getString("discount_type"),
                rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("discount_rate"),
                rs.getBigDecimal("max_discount_amount"),
                toLocalDateTime(rs.getTimestamp("valid_from")),
                toLocalDateTime(rs.getTimestamp("valid_until")),
                rs.getBoolean("enabled"),
                rs.getInt("sort_order")
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String courseSelectSql(String resourceType, boolean single) {
        String table = courseTable(resourceType);
        String detailJoin = "online-open-courses".equals(resourceType)
                ? """
                  LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                  """
                : "";
        String detailColumns = "online-open-courses".equals(resourceType)
                ? "COALESCE(p.course_detail, '') AS course_description, p.semester_plan, p.course_overview, p.video_file_path"
                : "c.course_description, NULL AS semester_plan, NULL AS course_overview, NULL AS video_file_path";
        String where = single ? "WHERE c.external_course_id = ?" : "";
        return """
                SELECT '%s' AS resource_type, c.external_course_id, c.course_name, c.teacher_name,
                       c.category, c.school_name, c.cover_url, c.cover_file_path, c.start_time,
                       c.participant_count, c.course_comment, %s, c.source_url, c.certified
                FROM %s c
                %s
                %s
                ORDER BY c.id DESC
                """.formatted(resourceType, detailColumns, table, detailJoin, where);
    }

    private String courseTable(String resourceType) {
        return switch (resourceType) {
            case "online-open-courses" -> "online_open_courses";
            case "general-courses" -> "general_courses";
            case "micro-major-courses" -> "micro_major_courses";
            default -> throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        };
    }

    private String getOptionalString(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (Exception ex) {
            return null;
        }
    }

    private String displayName(String username, String roleType, String teacherName) {
        if ("teacher".equals(roleType) && teacherName != null && !teacherName.isBlank()) {
            return teacherName;
        }
        return username;
    }

    private String platformUsername(String username, long userId) {
        return username + "_" + userId;
    }

    private String platformRole(String roleType) {
        if ("teacher".equals(roleType)) {
            return "TEACHER";
        }
        if ("admin".equals(roleType)) {
            return "ADMIN";
        }
        return "STUDENT";
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public record AdminAuthRow(long id, String username, String email, String roleType) {
    }
}
