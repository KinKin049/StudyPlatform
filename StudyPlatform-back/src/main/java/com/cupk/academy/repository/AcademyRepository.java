package com.cupk.academy.repository;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
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

    public List<AcademyCourseResponse> findGeneralCourses() {
        return findLearningCourses("general_courses");
    }

    public List<AcademyCourseResponse> findMicroMajorCourses() {
        return findLearningCourses("micro_major_courses");
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
}
