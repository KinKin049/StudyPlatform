package com.cupk.admin.repository;

import com.cupk.admin.dto.AdminCourseRequest;
import com.cupk.admin.dto.AdminCourseResponse;
import com.cupk.admin.dto.AdminCourseReviewResponse;
import com.cupk.admin.dto.AdminOjProblemRequest;
import com.cupk.admin.dto.AdminOjProblemResponse;
import com.cupk.admin.dto.AdminOjTestCaseRequest;
import com.cupk.admin.dto.AdminOjTestCaseResponse;
import com.cupk.admin.dto.AdminQuestionBankSetRequest;
import com.cupk.admin.dto.AdminQuestionRequest;
import com.cupk.admin.dto.AdminUserResponse;
import com.cupk.admin.dto.AdminVoucherItemRequest;
import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 管理员数据访问层，提供用户管理、课程管理、题库管理、OJ题目管理和卡券管理的数据操作。
 */
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

    public void updateUserPassword(long userId, String passwordHash) {
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                passwordHash,
                userId
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
                       source_url, certified, certification_label)
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
                      source_url = VALUES(source_url),
                      certified = VALUES(certified),
                      certification_label = VALUES(certification_label)
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
                    Boolean.TRUE.equals(request.certified()) ? 1 : 0,
                    request.certificationLabel()
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
                   course_description, source_url, certified, certification_label)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                  certified = VALUES(certified),
                  certification_label = VALUES(certification_label)
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
                Boolean.TRUE.equals(request.certified()) ? 1 : 0,
                request.certificationLabel()
        );
    }

    public List<AcademyCategoryResponse> findCourseCategories(String resourceType) {
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

    public boolean courseCategoryExists(String resourceType, String name) {
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

    public void upsertCourseCategory(String resourceType, String name) {
        Integer maxSort = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM admin_course_categories WHERE resource_type = ?",
                Integer.class,
                resourceType
        );
        jdbcTemplate.update(
                """
                INSERT INTO admin_course_categories (resource_type, name, sort_order)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """,
                resourceType,
                name,
                (maxSort == null ? 0 : maxSort) + 1
        );
    }

    public int deleteCourseCategory(String resourceType, String name) {
        return jdbcTemplate.update(
                "DELETE FROM admin_course_categories WHERE resource_type = ? AND name = ?",
                resourceType,
                name
        );
    }

    public List<AcademyCategoryResponse> findOjCategories() {
        return jdbcTemplate.query(
                """
                SELECT name
                FROM oj_categories
                ORDER BY sort_order ASC, name ASC
                """,
                (rs, rowNum) -> new AcademyCategoryResponse(rs.getString("name"))
        );
    }

    public boolean ojCategoryExists(String name) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oj_categories WHERE name = ?",
                Long.class,
                name
        );
        return count != null && count > 0;
    }

    public void upsertOjCategory(String name) {
        Integer maxSort = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM oj_categories",
                Integer.class
        );
        jdbcTemplate.update(
                """
                INSERT INTO oj_categories (name, sort_order)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """,
                name,
                (maxSort == null ? 0 : maxSort) + 1
        );
    }

    public int deleteOjCategory(String name) {
        return jdbcTemplate.update("DELETE FROM oj_categories WHERE name = ?", name);
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
        List<AdminCourseReviewResponse> reviews = new ArrayList<>();
        reviews.addAll(jdbcTemplate.query(
                """
                SELECT r.id, r.resource_type, r.course_id AS target_id,
                       r.parent_review_id,
                       COALESCE(NULLIF(pu.nickname, ''), NULLIF(pu.username, ''), pr.user_name) AS parent_user_name,
                       r.user_id,
                       COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), r.user_name) AS user_name,
                       u.email AS user_email,
                       COALESCE(NULLIF(u.role_type, ''), CASE WHEN u.role = 'TEACHER' THEN 'teacher' WHEN u.role = 'ADMIN' THEN 'admin' ELSE 'student' END, 'student') AS user_role_type,
                       r.rating, r.content, r.created_at,
                       r.reply_content, r.reply_user_id, r.reply_user_name, r.reply_user_role_type, r.replied_at
                FROM academy_course_reviews r
                LEFT JOIN users u ON u.id = r.user_id
                LEFT JOIN academy_course_reviews pr ON pr.id = r.parent_review_id
                LEFT JOIN users pu ON pu.id = pr.user_id
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT 300
                """,
                (rs, rowNum) -> mapAdminReview(rs, "course")
        ));
        reviews.addAll(jdbcTemplate.query(
                """
                SELECT r.id, 'textbook' AS resource_type, r.textbook_id AS target_id,
                       NULL AS parent_review_id, NULL AS parent_user_name, r.user_id,
                       COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), r.user_name) AS user_name,
                       u.email AS user_email,
                       COALESCE(NULLIF(u.role_type, ''), CASE WHEN u.role = 'TEACHER' THEN 'teacher' WHEN u.role = 'ADMIN' THEN 'admin' ELSE 'student' END, 'student') AS user_role_type,
                       r.rating, r.content, r.created_at,
                       r.reply_content, r.reply_user_id, r.reply_user_name, r.reply_user_role_type, r.replied_at
                FROM academy_textbook_reviews r
                LEFT JOIN users u ON u.id = r.user_id
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT 300
                """,
                (rs, rowNum) -> mapAdminReview(rs, "textbook")
        ));
        reviews.sort((left, right) -> {
            if (left.createdAt() == null && right.createdAt() == null) {
                return Long.compare(right.id(), left.id());
            }
            if (left.createdAt() == null) {
                return 1;
            }
            if (right.createdAt() == null) {
                return -1;
            }
            int compared = right.createdAt().compareTo(left.createdAt());
            return compared != 0 ? compared : Long.compare(right.id(), left.id());
        });
        return reviews.size() > 600 ? reviews.subList(0, 600) : reviews;
    }

    public int deleteReview(String reviewType, long reviewId) {
        return jdbcTemplate.update(reviewTable(reviewType).deleteSql(), reviewId);
    }

    public int replyReview(String reviewType, long reviewId, AdminAuthRow replier, String content) {
        return jdbcTemplate.update(
                reviewTable(reviewType).replySql(),
                content,
                replier.id(),
                replier.username(),
                replier.roleType(),
                reviewId
        );
    }

    public int clearReviewReply(String reviewType, long reviewId) {
        return jdbcTemplate.update(reviewTable(reviewType).clearReplySql(), reviewId);
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

    public List<AdminOjProblemResponse> findOjProblems() {
        return jdbcTemplate.query(
                ojProblemSelectSql("") + " ORDER BY p.id DESC LIMIT 500",
                (rs, rowNum) -> mapOjProblem(rs, List.of())
        );
    }

    public List<AdminOjProblemResponse> findOjProblemsByOwner(long ownerId) {
        return jdbcTemplate.query(
                ojProblemSelectSql("WHERE p.created_by = ?") + " ORDER BY p.id DESC LIMIT 500",
                (rs, rowNum) -> mapOjProblem(rs, List.of()),
                ownerId
        );
    }

    public Optional<AdminOjProblemResponse> findOjProblem(long problemId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    ojProblemSelectSql("WHERE p.id = ?") + " LIMIT 1",
                    (rs, rowNum) -> mapOjProblem(rs, findOjTestCases(problemId)),
                    problemId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public long upsertOjProblem(Long problemId, AdminOjProblemRequest request, long createdBy) {
        Long savedId = problemId;
        if (savedId == null) {
            String sql = """
                    INSERT INTO oj_problems
                      (title, slug, category, description, input_description, output_description, standard_code,
                       samples, difficulty, time_limit_ms, memory_limit_kb, tags, status, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, CAST(? AS JSON), ?, ?)
                    """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, request.title());
                ps.setString(2, request.slug());
                ps.setString(3, request.category());
                ps.setString(4, request.description());
                ps.setString(5, request.inputDescription());
                ps.setString(6, request.outputDescription());
                ps.setString(7, request.standardCode());
                ps.setString(8, request.difficulty());
                ps.setInt(9, request.timeLimitMs());
                ps.setInt(10, request.memoryLimitKb());
                ps.setString(11, tagsToJson(request.tags()));
                ps.setString(12, request.status());
                ps.setLong(13, createdBy);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            savedId = key == null ? 0L : key.longValue();
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE oj_problems
                    SET title = ?, slug = ?, description = ?, input_description = ?, output_description = ?,
                        category = ?, standard_code = ?, difficulty = ?, time_limit_ms = ?, memory_limit_kb = ?,
                        tags = CAST(? AS JSON), status = ?
                    WHERE id = ?
                    """,
                    request.title(),
                    request.slug(),
                    request.description(),
                    request.inputDescription(),
                    request.outputDescription(),
                    request.category(),
                    request.standardCode(),
                    request.difficulty(),
                    request.timeLimitMs(),
                    request.memoryLimitKb(),
                    tagsToJson(request.tags()),
                    request.status(),
                    savedId
            );
        }
        replaceOjTestCases(savedId, request.testCases());
        return savedId;
    }

    public int deleteOjProblem(long problemId) {
        return jdbcTemplate.update("DELETE FROM oj_problems WHERE id = ?", problemId);
    }

    public List<AdminOjTestCaseResponse> findOjTestCases(long problemId) {
        return jdbcTemplate.query(
                """
                SELECT id, problem_id, input_data, expected_output, sample, weight, sort_order
                FROM oj_test_cases
                WHERE problem_id = ?
                ORDER BY sort_order ASC, id ASC
                """,
                (rs, rowNum) -> new AdminOjTestCaseResponse(
                        rs.getLong("id"),
                        rs.getLong("problem_id"),
                        rs.getString("input_data"),
                        rs.getString("expected_output"),
                        rs.getBoolean("sample"),
                        rs.getInt("weight"),
                        rs.getInt("sort_order")
                ),
                problemId
        );
    }

    private void replaceOjTestCases(long problemId, List<AdminOjTestCaseRequest> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return;
        }
        List<Long> existingIds = jdbcTemplate.queryForList(
                "SELECT id FROM oj_test_cases WHERE problem_id = ?",
                Long.class,
                problemId
        );
        Set<Long> existingIdSet = new HashSet<>(existingIds);
        Set<Long> keptExistingIds = new HashSet<>();
        for (AdminOjTestCaseRequest testCase : testCases) {
            Long testCaseId = testCase.id();
            if (testCaseId != null && existingIdSet.contains(testCaseId)) {
                jdbcTemplate.update(
                        """
                        UPDATE oj_test_cases
                        SET input_data = ?, expected_output = ?, sample = ?, weight = ?, sort_order = ?
                        WHERE id = ? AND problem_id = ?
                        """,
                        testCase.inputData(),
                        testCase.expectedOutput(),
                        Boolean.TRUE.equals(testCase.sample()),
                        testCase.weight() == null ? 1 : testCase.weight(),
                        testCase.sortOrder() == null ? 0 : testCase.sortOrder(),
                        testCaseId,
                        problemId
                );
                keptExistingIds.add(testCaseId);
            } else {
                insertOjTestCase(problemId, testCase);
            }
        }
        for (Long existingId : existingIds) {
            if (!keptExistingIds.contains(existingId)) {
                jdbcTemplate.update(
                        """
                        DELETE FROM oj_test_cases
                        WHERE id = ? AND problem_id = ?
                          AND NOT EXISTS (
                            SELECT 1 FROM oj_submission_cases WHERE test_case_id = ?
                          )
                        """,
                        existingId,
                        problemId,
                        existingId
                );
            }
        }
    }

    private void insertOjTestCase(long problemId, AdminOjTestCaseRequest testCase) {
        jdbcTemplate.update(
                """
                INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                problemId,
                testCase.inputData(),
                testCase.expectedOutput(),
                Boolean.TRUE.equals(testCase.sample()),
                testCase.weight() == null ? 1 : testCase.weight(),
                testCase.sortOrder() == null ? 0 : testCase.sortOrder()
        );
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
                rs.getBoolean("certified"),
                getOptionalString(rs, "certification_label")
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

    private AdminOjProblemResponse mapOjProblem(ResultSet rs, List<AdminOjTestCaseResponse> testCases) throws SQLException {
        return new AdminOjProblemResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                getOptionalString(rs, "category"),
                rs.getString("description"),
                rs.getString("input_description"),
                rs.getString("output_description"),
                getOptionalString(rs, "standard_code"),
                rs.getString("difficulty"),
                rs.getInt("time_limit_ms"),
                rs.getInt("memory_limit_kb"),
                tagsFromJson(rs.getString("tags")),
                rs.getString("status"),
                getNullableLong(rs, "created_by"),
                getOptionalString(rs, "owner_name"),
                getOptionalString(rs, "owner_role_type"),
                testCases
        );
    }

    private AdminCourseReviewResponse mapAdminReview(ResultSet rs, String reviewType) throws SQLException {
        return new AdminCourseReviewResponse(
                rs.getLong("id"),
                reviewType,
                rs.getString("resource_type"),
                rs.getString("target_id"),
                getNullableLong(rs, "parent_review_id"),
                getOptionalString(rs, "parent_user_name"),
                getNullableLong(rs, "user_id"),
                rs.getString("user_name"),
                getOptionalString(rs, "user_email"),
                getOptionalString(rs, "user_role_type"),
                rs.getInt("rating"),
                rs.getString("content"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                getOptionalString(rs, "reply_content"),
                getNullableLong(rs, "reply_user_id"),
                getOptionalString(rs, "reply_user_name"),
                getOptionalString(rs, "reply_user_role_type"),
                toLocalDateTime(rs.getTimestamp("replied_at"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String courseSelectSql(String resourceType, boolean single) {
        String table = courseTable(resourceType);
        String detailJoin = "online-open-courses".equals(resourceType)
                ? """
                  LEFT JOIN teacher_published_courses p ON p.course_id = c.external_course_id
                  """
                : "";
        String detailColumns = "online-open-courses".equals(resourceType)
                ? "COALESCE(p.course_detail, c.course_description, '') AS course_description, p.semester_plan, p.course_overview, p.video_file_path"
                : "c.course_description, NULL AS semester_plan, NULL AS course_overview, NULL AS video_file_path";
        String where = single ? "WHERE c.external_course_id = ?" : "";
        return """
                SELECT '%s' AS resource_type, c.external_course_id, c.course_name, c.teacher_name,
                       c.category, c.school_name, c.cover_url, c.cover_file_path, c.start_time,
                       c.participant_count, c.course_comment, %s, c.source_url, c.certified,
                       c.certification_label
                FROM %s c
                %s
                %s
                ORDER BY c.id DESC
                """.formatted(resourceType, detailColumns, table, detailJoin, where);
    }

    private String ojProblemSelectSql(String where) {
        return """
                SELECT p.id, p.title, p.slug, p.category, p.description, p.input_description, p.output_description,
                       p.standard_code, p.difficulty, p.time_limit_ms, p.memory_limit_kb,
                       CAST(p.tags AS CHAR) AS tags, p.status, p.created_by,
                       COALESCE(NULLIF(owner.nickname, ''), NULLIF(owner.username, ''), '') AS owner_name,
                       COALESCE(NULLIF(owner.role_type, ''),
                         CASE WHEN owner.role = 'TEACHER' THEN 'teacher'
                              WHEN owner.role = 'ADMIN' THEN 'admin'
                              ELSE 'student' END
                       ) AS owner_role_type
                FROM oj_problems p
                LEFT JOIN users owner ON owner.id = p.created_by
                %s
                """.formatted(where == null ? "" : where);
    }

    private String courseTable(String resourceType) {
        return switch (resourceType) {
            case "online-open-courses" -> "online_open_courses";
            case "general-courses" -> "general_courses";
            case "micro-major-courses" -> "micro_major_courses";
            default -> throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        };
    }

    private ReviewTable reviewTable(String reviewType) {
        return switch (reviewType == null ? "" : reviewType) {
            case "course" -> new ReviewTable(
                    "DELETE FROM academy_course_reviews WHERE id = ?",
                    """
                    UPDATE academy_course_reviews
                    SET reply_content = ?, reply_user_id = ?, reply_user_name = ?, reply_user_role_type = ?, replied_at = NOW()
                    WHERE id = ?
                    """,
                    """
                    UPDATE academy_course_reviews
                    SET reply_content = NULL, reply_user_id = NULL, reply_user_name = NULL, reply_user_role_type = NULL, replied_at = NULL
                    WHERE id = ?
                    """
            );
            case "textbook" -> new ReviewTable(
                    "DELETE FROM academy_textbook_reviews WHERE id = ?",
                    """
                    UPDATE academy_textbook_reviews
                    SET reply_content = ?, reply_user_id = ?, reply_user_name = ?, reply_user_role_type = ?, replied_at = NOW()
                    WHERE id = ?
                    """,
                    """
                    UPDATE academy_textbook_reviews
                    SET reply_content = NULL, reply_user_id = NULL, reply_user_name = NULL, reply_user_role_type = NULL, replied_at = NULL
                    WHERE id = ?
                    """
            );
            default -> throw new IllegalArgumentException("Unsupported review type: " + reviewType);
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

    private String tagsToJson(String tags) {
        if (tags == null || tags.isBlank()) {
            return "[]";
        }
        List<String> items = java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
        return toJson(items);
    }

    private String tagsFromJson(String json) {
        return String.join(", ", parseStringList(json));
    }

    public record AdminAuthRow(long id, String username, String email, String roleType) {
    }

    private record ReviewTable(String deleteSql, String replySql, String clearReplySql) {
    }
}
