package com.cupk.academy.repository;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
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
                       course_comment, '' AS course_description, source_url
                FROM online_open_courses
                ORDER BY id ASC
                """;
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
                rs.getString("source_url")
        ));
    }

    public Optional<AcademyCourseResponse> findOnlineOpenCourseById(String id) {
        return findLearningCourseById("online_open_courses", true, id);
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
                       enrolled.course_description, enrolled.source_url, enrolled.enrolled_at
                FROM (
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           '' AS course_description, c.source_url, e.created_at AS enrolled_at
                    FROM academy_course_enrollments e
                    JOIN online_open_courses c ON c.external_course_id = e.course_id
                    WHERE e.resource_type = 'online-open-courses' AND e.user_id = ?
                    UNION ALL
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           c.course_description, c.source_url, e.created_at AS enrolled_at
                    FROM academy_course_enrollments e
                    JOIN general_courses c ON c.external_course_id = e.course_id
                    WHERE e.resource_type = 'general-courses' AND e.user_id = ?
                    UNION ALL
                    SELECT e.resource_type, c.external_course_id, c.course_name, c.teacher_name,
                           c.category, c.school_name, c.cover_url, c.cover_file_path,
                           c.start_time, c.participant_count, c.course_comment,
                           c.course_description, c.source_url, e.created_at AS enrolled_at
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
                       course_comment, course_description, source_url
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
                rs.getString("source_url")
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
                       course_comment, %s, source_url
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
                    rs.getString("source_url")
            ), id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
