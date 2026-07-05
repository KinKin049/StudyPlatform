package com.cupk.academy.repository;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
