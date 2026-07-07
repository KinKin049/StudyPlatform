package com.cupk.academy.repository;

import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionPageResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.academy.dto.QuestionBankFavoritePageResponse;
import com.cupk.academy.dto.QuestionBankFavoriteResponse;
import com.cupk.academy.dto.QuestionBankFavoriteSetSummaryResponse;
import com.cupk.academy.dto.QuestionBankFavoriteSummaryResponse;
import com.cupk.academy.dto.QuestionBankMistakePageResponse;
import com.cupk.academy.dto.QuestionBankMistakeResponse;
import com.cupk.academy.dto.QuestionBankMistakeSetSummaryResponse;
import com.cupk.academy.dto.QuestionBankMistakeSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionBankRepository {

    /**
     * 题库数据访问层，提供题目查询、错题管理、收藏管理和词汇学习等功能。
     */

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     * @param objectMapper JSON序列化器
     */
    public QuestionBankRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询题库学科列表
     *
     * @return 学科列表
     */
    public List<QuestionBankSubjectResponse> findSubjects() {
        String sql = """
                SELECT s.subject_code, s.subject_name, s.description, COUNT(ps.problem_id) AS problem_count
                FROM question_bank_subjects s
                LEFT JOIN question_bank_problem_subjects ps ON ps.subject_id = s.id
                GROUP BY s.id, s.subject_code, s.subject_name, s.description, s.sort_order
                ORDER BY s.sort_order ASC, s.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new QuestionBankSubjectResponse(
                rs.getString("subject_code"),
                rs.getString("subject_name"),
                rs.getString("description"),
                rs.getInt("problem_count")
        ));
    }

    /**
     * 分页查询题库题目列表
     *
     * @param subjectCode 学科代码
     * @param keyword 关键词
     * @param difficulty 难度
     * @param page 页码
     * @param size 每页大小
     * @return 题目分页结果
     */
    public QuestionBankProblemPageResponse findProblems(
            String subjectCode,
            String keyword,
            Integer difficulty,
            int page,
            int size
    ) {
        List<Object> params = new ArrayList<>();
        String where = buildProblemWhere(subjectCode, keyword, difficulty, params);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question_bank_problems p " + where, Long.class, params.toArray());
        int offset = Math.max(0, page) * size;
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(size);
        listParams.add(offset);
        String sql = """
                SELECT p.id, p.source, p.external_problem_id, p.title, p.difficulty, p.difficulty_label,
                       CAST(p.tag_names AS CHAR) AS tag_names, p.description, p.input_description,
                       p.output_description, p.hint, p.total_submit, p.total_accepted, p.source_url, p.imported_at
                FROM question_bank_problems p
                %s
                ORDER BY COALESCE(p.difficulty, 99) ASC, p.external_problem_id ASC
                LIMIT ? OFFSET ?
                """.formatted(where);
        List<QuestionBankProblemResponse> items = jdbcTemplate.query(sql, this::mapProblem, listParams.toArray());
        return new QuestionBankProblemPageResponse(items, Math.max(0, page), size, total == null ? 0 : total);
    }

    /**
     * 根据ID查询题目详情
     *
     * @param id 题目ID
     * @return 题目详情，不存在则返回空
     */
    public Optional<QuestionBankProblemResponse> findProblemById(long id) {
        String sql = """
                SELECT p.id, p.source, p.external_problem_id, p.title, p.difficulty, p.difficulty_label,
                       CAST(p.tag_names AS CHAR) AS tag_names, p.description, p.input_description,
                       p.output_description, p.hint, p.total_submit, p.total_accepted, p.source_url, p.imported_at
                FROM question_bank_problems p
                WHERE p.id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapProblem, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 插入或更新标签
     *
     * @param source 来源
     * @param externalTagId 外部标签ID
     * @param tagName 标签名称
     * @param tagType 标签类型
     * @param parentExternalTagId 父标签ID
     * @return 更新的行数
     */
    public int upsertTag(String source, int externalTagId, String tagName, Integer tagType, Integer parentExternalTagId) {
        String sql = """
                INSERT INTO question_bank_tags (source, external_tag_id, tag_name, tag_type, parent_external_tag_id)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  tag_name = VALUES(tag_name),
                  tag_type = VALUES(tag_type),
                  parent_external_tag_id = VALUES(parent_external_tag_id)
                """;
        return jdbcTemplate.update(sql, source, externalTagId, tagName, tagType, parentExternalTagId);
    }

    /**
     * 插入或更新题目
     *
     * @param source 来源
     * @param externalProblemId 外部题目ID
     * @param title 题目标题
     * @param difficulty 难度
     * @param difficultyLabel 难度标签
     * @param tagIds 标签ID列表
     * @param tagNames 标签名称列表
     * @param description 题目描述
     * @param inputDescription 输入描述
     * @param outputDescription 输出描述
     * @param hint 提示
     * @param totalSubmit 提交次数
     * @param totalAccepted 通过次数
     * @param sourceUrl 来源URL
     * @return 题目ID
     */
    public long upsertProblem(
            String source,
            String externalProblemId,
            String title,
            Integer difficulty,
            String difficultyLabel,
            List<Integer> tagIds,
            List<String> tagNames,
            String description,
            String inputDescription,
            String outputDescription,
            String hint,
            Integer totalSubmit,
            Integer totalAccepted,
            String sourceUrl
    ) {
        String sql = """
                INSERT INTO question_bank_problems
                  (source, external_problem_id, title, difficulty, difficulty_label, tag_ids, tag_names,
                   description, input_description, output_description, hint, total_submit, total_accepted, source_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  title = VALUES(title),
                  difficulty = VALUES(difficulty),
                  difficulty_label = VALUES(difficulty_label),
                  tag_ids = VALUES(tag_ids),
                  tag_names = VALUES(tag_names),
                  description = COALESCE(NULLIF(VALUES(description), ''), description),
                  input_description = COALESCE(NULLIF(VALUES(input_description), ''), input_description),
                  output_description = COALESCE(NULLIF(VALUES(output_description), ''), output_description),
                  hint = COALESCE(NULLIF(VALUES(hint), ''), hint),
                  total_submit = VALUES(total_submit),
                  total_accepted = VALUES(total_accepted),
                  source_url = VALUES(source_url)
                """;
        jdbcTemplate.update(sql, source, externalProblemId, title, difficulty, difficultyLabel,
                toJson(tagIds), toJson(tagNames), description, inputDescription, outputDescription, hint,
                totalSubmit, totalAccepted, sourceUrl);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM question_bank_problems WHERE source = ? AND external_problem_id = ?",
                Long.class,
                source,
                externalProblemId
        );
    }

    /**
     * 将题目关联到所有学科
     *
     * @param problemId 题目ID
     */
    public void attachProblemToAllSubjects(long problemId) {
        String sql = """
                INSERT IGNORE INTO question_bank_problem_subjects (problem_id, subject_id)
                SELECT ?, id FROM question_bank_subjects
                WHERE subject_code IN ('c-language', 'java', 'python')
                """;
        jdbcTemplate.update(sql, problemId);
    }

    /**
     * 查询标签名称列表
     *
     * @param source 来源
     * @param tagIds 标签ID列表
     * @return 标签名称列表
     */
    public List<String> findTagNames(String source, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", tagIds.stream().map(id -> "?").toList());
        List<Object> params = new ArrayList<>();
        params.add(source);
        params.addAll(tagIds);
        String sql = "SELECT tag_name FROM question_bank_tags WHERE source = ? AND external_tag_id IN (" + placeholders + ") ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("tag_name"), params.toArray());
    }

    /**
     * 查询课程题库目录（分类和题目集）
     *
     * @return 题库目录列表
     */
    public List<CourseQuestionBankCategoryResponse> findCourseQuestionBankCatalog() {
        String sql = """
                SELECT c.category_code, c.category_name, c.description AS category_description,
                       s.id, s.set_code, s.title, s.subtitle, s.description, s.cover_url, s.cover_file_path,
                       s.difficulty_label, s.status_label, s.source_name, s.source_url,
                       CAST(s.source_refs AS CHAR) AS source_refs, s.route_path,
                       (
                         SELECT COUNT(*)
                         FROM course_question_bank_questions q
                         WHERE q.set_id = s.id
                       ) AS question_count
                FROM course_question_bank_categories c
                LEFT JOIN course_question_bank_sets s ON s.category_id = c.id
                ORDER BY c.sort_order ASC, c.id ASC, s.sort_order ASC, s.id ASC
                """;
        Map<String, CourseQuestionBankCategoryBuilder> categories = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String categoryCode = rs.getString("category_code");
            CourseQuestionBankCategoryBuilder builder = categories.computeIfAbsent(
                    categoryCode,
                    code -> new CourseQuestionBankCategoryBuilder(
                            code,
                            getString(rs, "category_name"),
                            getString(rs, "category_description")
                    )
            );
            long setId = rs.getLong("id");
            if (!rs.wasNull()) {
                builder.sets().add(mapCourseQuestionBankSet(rs));
            }
        });
        return categories.values().stream()
                .map(builder -> new CourseQuestionBankCategoryResponse(
                        builder.code(),
                        builder.name(),
                        builder.description(),
                        builder.sets()
                ))
                .toList();
    }

    /**
     * 根据代码查询课程题目集详情
     *
     * @param code 题目集代码
     * @return 题目集详情，不存在则返回空
     */
    public Optional<CourseQuestionBankSetResponse> findCourseQuestionBankSet(String code) {
        String sql = """
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
                WHERE s.set_code = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapCourseQuestionBankSet, code));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 分页查询课程题目集下的题目列表
     *
     * @param code 题目集代码
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID（用于判断收藏状态）
     * @return 题目分页结果
     */
    public CourseQuestionBankQuestionPageResponse findCourseQuestionBankQuestions(
            String code,
            String keyword,
            int page,
            int size,
            long userId
    ) {
        List<Object> params = new ArrayList<>();
        params.add(code);
        String keywordCondition = "";
        if (keyword != null && !keyword.isBlank()) {
            keywordCondition = " AND (q.stem LIKE ? OR q.answer LIKE ? OR q.explanation LIKE ?)";
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE s.set_code = ?
                %s
                """.formatted(keywordCondition),
                Long.class,
                params.toArray()
        );
        int offset = Math.max(0, page) * size;
        List<Object> listParams = new ArrayList<>();
        listParams.add(userId);
        listParams.addAll(params);
        listParams.add(size);
        listParams.add(offset);
        String sql = """
                SELECT q.id, q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url,
                       CASE WHEN f.question_id IS NULL THEN 0 ELSE 1 END AS favorite
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                LEFT JOIN course_question_bank_favorites f ON f.question_id = q.id AND f.user_id = ?
                WHERE s.set_code = ?
                %s
                ORDER BY q.sort_order ASC, q.id ASC
                LIMIT ? OFFSET ?
                """.formatted(keywordCondition);
        List<CourseQuestionBankQuestionResponse> items =
                jdbcTemplate.query(sql, this::mapCourseQuestionBankQuestion, listParams.toArray());
        long safeTotal = total == null ? 0 : total;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) safeTotal / size);
        return new CourseQuestionBankQuestionPageResponse(items, Math.max(0, page), size, safeTotal, totalPages);
    }

    /**
     * 查询用户错题汇总信息
     *
     * @param userId 用户ID
     * @return 错题汇总
     */
    public QuestionBankMistakeSummaryResponse findMistakeSummary(long userId) {
        String totalsSql = """
                SELECT
                  COUNT(*) AS total_count,
                  COALESCE(SUM(CASE WHEN mastered = 0 THEN 1 ELSE 0 END), 0) AS active_count,
                  COALESCE(SUM(CASE WHEN mastered = 1 THEN 1 ELSE 0 END), 0) AS mastered_count
                FROM course_question_bank_mistakes
                WHERE user_id = ?
                """;
        QuestionBankMistakeTotals totals = jdbcTemplate.queryForObject(
                totalsSql,
                (rs, rowNum) -> new QuestionBankMistakeTotals(
                        rs.getLong("total_count"),
                        rs.getLong("active_count"),
                        rs.getLong("mastered_count")
                ),
                userId
        );

        String setsSql = """
                SELECT s.set_code, s.title AS set_title, c.category_name,
                       COUNT(*) AS total_count,
                       COALESCE(SUM(CASE WHEN m.mastered = 0 THEN 1 ELSE 0 END), 0) AS active_count,
                       COALESCE(SUM(CASE WHEN m.mastered = 1 THEN 1 ELSE 0 END), 0) AS mastered_count,
                       MAX(COALESCE(m.last_reviewed_at, m.last_wrong_at, m.updated_at)) AS latest_at
                FROM course_question_bank_mistakes m
                JOIN course_question_bank_questions q ON q.id = m.question_id
                JOIN course_question_bank_sets s ON s.id = q.set_id
                JOIN course_question_bank_categories c ON c.id = s.category_id
                WHERE m.user_id = ?
                GROUP BY s.id, s.set_code, s.title, c.category_name
                ORDER BY active_count DESC, latest_at DESC, s.sort_order ASC
                """;
        List<QuestionBankMistakeSetSummaryResponse> sets = jdbcTemplate.query(
                setsSql,
                (rs, rowNum) -> new QuestionBankMistakeSetSummaryResponse(
                        rs.getString("set_code"),
                        rs.getString("set_title"),
                        rs.getString("category_name"),
                        rs.getLong("total_count"),
                        rs.getLong("active_count"),
                        rs.getLong("mastered_count"),
                        rs.getObject("latest_at", LocalDateTime.class)
                ),
                userId
        );

        QuestionBankMistakeTotals safeTotals =
                totals == null ? new QuestionBankMistakeTotals(0, 0, 0) : totals;
        return new QuestionBankMistakeSummaryResponse(
                safeTotals.total(),
                safeTotals.active(),
                safeTotals.mastered(),
                sets
        );
    }

    /**
     * 分页查询用户错题列表
     *
     * @param userId 用户ID
     * @param setCode 题目集代码
     * @param status 状态（mastered/all/默认active）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 错题分页结果
     */
    public QuestionBankMistakePageResponse findMistakes(
            long userId,
            String setCode,
            String status,
            String keyword,
            int page,
            int size
    ) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("m.user_id = ?");
        params.add(userId);

        if (setCode != null && !setCode.isBlank()) {
            conditions.add("s.set_code = ?");
            params.add(setCode.trim());
        }

        if ("mastered".equalsIgnoreCase(status)) {
            conditions.add("m.mastered = 1");
        } else if (!"all".equalsIgnoreCase(status)) {
            conditions.add("m.mastered = 0");
        }

        if (keyword != null && !keyword.isBlank()) {
            conditions.add("""
                    (q.stem LIKE ? OR q.answer LIKE ? OR q.explanation LIKE ? OR m.selected_answer LIKE ? OR s.title LIKE ?)
                    """);
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String where = "WHERE " + String.join(" AND ", conditions);
        String from = """
                FROM course_question_bank_mistakes m
                JOIN course_question_bank_questions q ON q.id = m.question_id
                JOIN course_question_bank_sets s ON s.id = q.set_id
                JOIN course_question_bank_categories c ON c.id = s.category_id
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + from + " " + where,
                Long.class,
                params.toArray()
        );

        int offset = Math.max(0, page) * size;
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(size);
        listParams.add(offset);
        String sql = """
                SELECT m.id AS mistake_id, m.question_id, m.selected_answer, m.correct_answer,
                       m.wrong_count, m.correct_streak, m.mastered, m.first_wrong_at,
                       m.last_wrong_at, m.last_reviewed_at,
                       s.set_code, s.title AS set_title, c.category_code, c.category_name,
                       q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url
                %s
                %s
                ORDER BY m.mastered ASC, COALESCE(m.last_wrong_at, m.updated_at) DESC, m.id DESC
                LIMIT ? OFFSET ?
                """.formatted(from, where);
        List<QuestionBankMistakeResponse> items =
                jdbcTemplate.query(sql, this::mapMistake, listParams.toArray());
        long safeTotal = total == null ? 0 : total;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) safeTotal / size);
        return new QuestionBankMistakePageResponse(items, Math.max(0, page), size, safeTotal, totalPages);
    }

    /**
     * 查询用户未掌握的单选题错题列表
     *
     * @param userId 用户ID
     * @param setCode 题目集代码
     * @return 单选题错题列表
     */
    public List<CourseQuestionBankQuestionResponse> findActiveSingleChoiceMistakeQuestions(long userId, String setCode) {
        List<Object> params = new ArrayList<>();
        params.add(userId);
        StringBuilder sql = new StringBuilder("""
                SELECT q.id, q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url,
                       0 AS favorite
                FROM course_question_bank_mistakes m
                JOIN course_question_bank_questions q ON q.id = m.question_id
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE m.user_id = ?
                  AND m.mastered = 0
                  AND q.question_type = 'single'
                """);
        if (setCode != null && !setCode.isBlank()) {
            sql.append(" AND s.set_code = ? ");
            params.add(setCode.trim());
        }
        sql.append(" ORDER BY COALESCE(m.last_wrong_at, m.updated_at) DESC, s.sort_order ASC, q.sort_order ASC, q.id ASC ");
        return jdbcTemplate.query(sql.toString(), this::mapCourseQuestionBankQuestion, params.toArray());
    }

    /**
     * 查询所有单选题列表
     *
     * @param setCode 题目集代码
     * @return 单选题列表
     */
    public List<CourseQuestionBankQuestionResponse> findAllSingleChoiceQuestions(String setCode) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT q.id, q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url,
                       0 AS favorite
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE q.question_type = 'single'
                """);
        if (setCode != null && !setCode.isBlank()) {
            sql.append(" AND s.set_code = ? ");
            params.add(setCode.trim());
        }
        sql.append(" ORDER BY s.sort_order ASC, q.sort_order ASC, q.id ASC ");
        return jdbcTemplate.query(sql.toString(), this::mapCourseQuestionBankQuestion, params.toArray());
    }

    /**
     * 查询包含单选题的题目集列表
     *
     * @return 题目集列表
     */
    public List<SingleChoiceQuestionBankRow> findSingleChoiceQuestionBanks() {
        String sql = """
                SELECT s.set_code, s.title, c.category_name, COUNT(*) AS question_count
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                JOIN course_question_bank_categories c ON c.id = s.category_id
                WHERE q.question_type = 'single'
                GROUP BY s.id, s.set_code, s.title, c.category_name, s.sort_order
                HAVING COUNT(*) > 0
                ORDER BY c.sort_order ASC, s.sort_order ASC, s.id ASC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SingleChoiceQuestionBankRow(
                        rs.getString("set_code"),
                        rs.getString("title"),
                        rs.getString("category_name"),
                        rs.getInt("question_count")
                )
        );
    }

    /**
     * 查询用户收藏汇总信息
     *
     * @param userId 用户ID
     * @return 收藏汇总
     */
    public QuestionBankFavoriteSummaryResponse findFavoriteSummary(long userId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_question_bank_favorites WHERE user_id = ?",
                Long.class,
                userId
        );
        String setsSql = """
                SELECT s.set_code, s.title AS set_title, c.category_name,
                       COUNT(*) AS total_count,
                       MAX(f.created_at) AS latest_at
                FROM course_question_bank_favorites f
                JOIN course_question_bank_questions q ON q.id = f.question_id
                JOIN course_question_bank_sets s ON s.id = q.set_id
                JOIN course_question_bank_categories c ON c.id = s.category_id
                WHERE f.user_id = ?
                GROUP BY s.id, s.set_code, s.title, c.category_name
                ORDER BY total_count DESC, latest_at DESC, s.sort_order ASC
                """;
        List<QuestionBankFavoriteSetSummaryResponse> sets = jdbcTemplate.query(
                setsSql,
                (rs, rowNum) -> new QuestionBankFavoriteSetSummaryResponse(
                        rs.getString("set_code"),
                        rs.getString("set_title"),
                        rs.getString("category_name"),
                        rs.getLong("total_count"),
                        rs.getObject("latest_at", LocalDateTime.class)
                ),
                userId
        );
        return new QuestionBankFavoriteSummaryResponse(total == null ? 0 : total, sets);
    }

    /**
     * 分页查询用户收藏列表
     *
     * @param userId 用户ID
     * @param setCode 题目集代码
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 收藏分页结果
     */
    public QuestionBankFavoritePageResponse findFavorites(
            long userId,
            String setCode,
            String keyword,
            int page,
            int size
    ) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("f.user_id = ?");
        params.add(userId);

        if (setCode != null && !setCode.isBlank()) {
            conditions.add("s.set_code = ?");
            params.add(setCode.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            conditions.add("(q.stem LIKE ? OR q.answer LIKE ? OR q.explanation LIKE ? OR s.title LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String where = "WHERE " + String.join(" AND ", conditions);
        String from = """
                FROM course_question_bank_favorites f
                JOIN course_question_bank_questions q ON q.id = f.question_id
                JOIN course_question_bank_sets s ON s.id = q.set_id
                JOIN course_question_bank_categories c ON c.id = s.category_id
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + from + " " + where,
                Long.class,
                params.toArray()
        );

        int offset = Math.max(0, page) * size;
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(size);
        listParams.add(offset);
        String sql = """
                SELECT f.id AS favorite_id, f.question_id, f.created_at,
                       s.set_code, s.title AS set_title, c.category_code, c.category_name,
                       q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url
                %s
                %s
                ORDER BY f.created_at DESC, f.id DESC
                LIMIT ? OFFSET ?
                """.formatted(from, where);
        List<QuestionBankFavoriteResponse> items =
                jdbcTemplate.query(sql, this::mapFavorite, listParams.toArray());
        long safeTotal = total == null ? 0 : total;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) safeTotal / size);
        return new QuestionBankFavoritePageResponse(items, Math.max(0, page), size, safeTotal, totalPages);
    }

    /**
     * 添加题目收藏（已存在则忽略）
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     */
    public void addFavorite(long userId, long questionId) {
        String sql = """
                INSERT IGNORE INTO course_question_bank_favorites (user_id, question_id)
                VALUES (?, ?)
                """;
        jdbcTemplate.update(sql, userId, questionId);
    }

    /**
     * 移除题目收藏
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     */
    public void removeFavorite(long userId, long questionId) {
        jdbcTemplate.update(
                "DELETE FROM course_question_bank_favorites WHERE user_id = ? AND question_id = ?",
                userId,
                questionId
        );
    }

    /**
     * 统计用户收藏数量
     *
     * @param userId 用户ID
     * @return 收藏数量
     */
    public long countFavorites(long userId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_question_bank_favorites WHERE user_id = ?",
                Long.class,
                userId
        );
        return total == null ? 0 : total;
    }

    /**
     * 查询课程题目答案参考
     *
     * @param questionId 题目ID
     * @return 答案参考，不存在则返回空
     */
    public Optional<CourseQuestionAnswerReference> findCourseQuestionAnswerReference(long questionId) {
        String sql = """
                SELECT q.id, s.set_code, q.question_type, q.answer
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE q.id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new CourseQuestionAnswerReference(
                            rs.getLong("id"),
                            rs.getString("set_code"),
                            rs.getString("question_type"),
                            rs.getString("answer")
                    ),
                    questionId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 插入或更新错题记录（答错时）
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param selectedAnswer 选中答案
     * @param correctAnswer 正确答案
     */
    public void upsertWrongMistake(long userId, long questionId, String selectedAnswer, String correctAnswer) {
        String sql = """
                INSERT INTO course_question_bank_mistakes
                  (user_id, question_id, selected_answer, correct_answer, wrong_count, correct_streak,
                   mastered, first_wrong_at, last_wrong_at, last_reviewed_at)
                VALUES (?, ?, ?, ?, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  selected_answer = VALUES(selected_answer),
                  correct_answer = VALUES(correct_answer),
                  wrong_count = wrong_count + 1,
                  correct_streak = 0,
                  mastered = 0,
                  last_wrong_at = CURRENT_TIMESTAMP,
                  last_reviewed_at = CURRENT_TIMESTAMP
                """;
        jdbcTemplate.update(sql, userId, questionId, selectedAnswer, correctAnswer);
    }

    /**
     * 应用错题正确复习（答对时）
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param selectedAnswer 选中答案
     * @param correctAnswer 正确答案
     * @param masteredThreshold 掌握阈值（连续答对次数）
     * @return 更新的行数
     */
    public int applyCorrectMistakeReview(
            long userId,
            long questionId,
            String selectedAnswer,
            String correctAnswer,
            int masteredThreshold
    ) {
        String sql = """
                UPDATE course_question_bank_mistakes
                SET selected_answer = ?,
                    correct_answer = ?,
                    correct_streak = correct_streak + 1,
                    mastered = CASE WHEN correct_streak + 1 >= ? THEN 1 ELSE mastered END,
                    last_reviewed_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND question_id = ?
                """;
        return jdbcTemplate.update(sql, selectedAnswer, correctAnswer, masteredThreshold, userId, questionId);
    }

    /**
     * 查询用户错题状态
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 错题状态，不存在则返回空
     */
    public Optional<QuestionBankMistakeState> findMistakeState(long userId, long questionId) {
        String sql = """
                SELECT wrong_count, correct_streak, mastered
                FROM course_question_bank_mistakes
                WHERE user_id = ? AND question_id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new QuestionBankMistakeState(
                            rs.getInt("wrong_count"),
                            rs.getInt("correct_streak"),
                            rs.getBoolean("mastered")
                    ),
                    userId,
                    questionId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 统计课程题目集下指定类型的题目数量
     *
     * @param code 题目集代码
     * @param questionType 题目类型
     * @return 题目数量
     */
    public long countCourseQuestionBankQuestions(String code, String questionType) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE s.set_code = ? AND q.question_type = ?
                """,
                Long.class,
                code,
                questionType
        );
        return total == null ? 0 : total;
    }

    /**
     * 删除课程题目集下的所有题目
     *
     * @param code 题目集代码
     */
    public void deleteCourseQuestionBankQuestions(String code) {
        jdbcTemplate.update(
                """
                DELETE q
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE s.set_code = ?
                """,
                code
        );
    }

    /**
     * 批量插入课程题目集题目
     *
     * @param code 题目集代码
     * @param questions 题目列表
     */
    public void batchInsertCourseQuestionBankQuestions(String code, List<CourseQuestionBankQuestionSeed> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        Long setId = jdbcTemplate.queryForObject(
                "SELECT id FROM course_question_bank_sets WHERE set_code = ?",
                Long.class,
                code
        );
        if (setId == null) {
            return;
        }
        String sql = """
                INSERT INTO course_question_bank_questions
                  (set_id, question_type, stem, options_json, answer, explanation, difficulty_label, source_url, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.batchUpdate(sql, questions, 500, (PreparedStatement ps, CourseQuestionBankQuestionSeed question) -> {
            ps.setLong(1, setId);
            ps.setString(2, question.type());
            ps.setString(3, question.stem());
            ps.setString(4, toJson(question.options()));
            ps.setString(5, question.answer());
            ps.setString(6, question.explanation());
            ps.setString(7, question.difficultyLabel());
            ps.setString(8, question.sourceUrl());
            ps.setInt(9, question.sortOrder());
        });
    }

    /**
     * 查询打字勇士词汇列表（含熟悉度状态）
     *
     * @param userId 用户ID
     * @return 词汇列表
     */
    public List<TypeWarriorVocabularyRow> findTypeWarriorVocabularyRows(long userId) {
        String sql = """
                SELECT q.id,
                       s.set_code,
                       q.stem,
                       q.answer,
                       COALESCE(latest_status.vocabulary_status, 'unmarked') AS familiarity
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                LEFT JOIN (
                  SELECT e.question_id, e.vocabulary_status
                  FROM profile_learning_events e
                  JOIN (
                    SELECT question_id, MAX(id) AS latest_id
                    FROM profile_learning_events
                    WHERE user_id = ? AND event_type = 'vocabulary' AND question_id IS NOT NULL
                    GROUP BY question_id
                  ) latest ON latest.latest_id = e.id
                ) latest_status ON latest_status.question_id = q.id
                WHERE q.question_type = 'vocabulary'
                  AND s.set_code IN ('cet4', 'cet6')
                ORDER BY s.set_code ASC, q.sort_order ASC, q.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TypeWarriorVocabularyRow(
                rs.getLong("id"),
                rs.getString("set_code"),
                rs.getString("stem"),
                rs.getString("answer"),
                rs.getString("familiarity")
        ), userId);
    }

    /**
     * 构建题目查询条件
     *
     * @param subjectCode 学科代码
     * @param keyword 关键词
     * @param difficulty 难度
     * @param params 参数列表（用于收集动态参数）
     * @return WHERE子句
     */
    private String buildProblemWhere(String subjectCode, String keyword, Integer difficulty, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (subjectCode != null && !subjectCode.isBlank()) {
            conditions.add("""
                    EXISTS (
                      SELECT 1
                      FROM question_bank_problem_subjects ps
                      JOIN question_bank_subjects s ON s.id = ps.subject_id
                      WHERE ps.problem_id = p.id AND s.subject_code = ?
                    )
                    """);
            params.add(subjectCode);
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.add("(p.title LIKE ? OR p.external_problem_id LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (difficulty != null) {
            conditions.add("p.difficulty = ?");
            params.add(difficulty);
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    /**
     * 将结果集映射为题目标题响应对象
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 题目标题响应对象
     * @throws SQLException SQL异常
     */
    private QuestionBankProblemResponse mapProblem(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionBankProblemResponse(
                rs.getLong("id"),
                rs.getString("source"),
                rs.getString("external_problem_id"),
                rs.getString("title"),
                rs.getObject("difficulty", Integer.class),
                rs.getString("difficulty_label"),
                parseStringList(rs.getString("tag_names")),
                rs.getString("description"),
                rs.getString("input_description"),
                rs.getString("output_description"),
                rs.getString("hint"),
                rs.getObject("total_submit", Integer.class),
                rs.getObject("total_accepted", Integer.class),
                rs.getString("source_url"),
                rs.getObject("imported_at", LocalDateTime.class)
        );
    }

    /**
     * 将结果集映射为课程题目集响应对象（兼容RowMapper接口）
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 课程题目集响应对象
     * @throws SQLException SQL异常
     */
    private CourseQuestionBankSetResponse mapCourseQuestionBankSet(ResultSet rs, int rowNum) throws SQLException {
        return mapCourseQuestionBankSet(rs);
    }

    /**
     * 将结果集映射为课程题目集响应对象
     *
     * @param rs 结果集
     * @return 课程题目集响应对象
     * @throws SQLException SQL异常
     */
    private CourseQuestionBankSetResponse mapCourseQuestionBankSet(ResultSet rs) throws SQLException {
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

    /**
     * 将结果集映射为课程题目响应对象
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 课程题目响应对象
     * @throws SQLException SQL异常
     */
    private CourseQuestionBankQuestionResponse mapCourseQuestionBankQuestion(ResultSet rs, int rowNum) throws SQLException {
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

    /**
     * 将结果集映射为收藏响应对象
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 收藏响应对象
     * @throws SQLException SQL异常
     */
    private QuestionBankFavoriteResponse mapFavorite(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionBankFavoriteResponse(
                rs.getLong("favorite_id"),
                rs.getLong("question_id"),
                rs.getString("set_code"),
                rs.getString("set_title"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("question_type"),
                rs.getString("stem"),
                parseStringList(rs.getString("options_json")),
                rs.getString("answer"),
                rs.getString("explanation"),
                rs.getString("difficulty_label"),
                rs.getString("source_url"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    /**
     * 将结果集映射为错题响应对象
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 错题响应对象
     * @throws SQLException SQL异常
     */
    private QuestionBankMistakeResponse mapMistake(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionBankMistakeResponse(
                rs.getLong("mistake_id"),
                rs.getLong("question_id"),
                rs.getString("set_code"),
                rs.getString("set_title"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("question_type"),
                rs.getString("stem"),
                parseStringList(rs.getString("options_json")),
                rs.getString("answer"),
                rs.getString("explanation"),
                rs.getString("difficulty_label"),
                rs.getString("source_url"),
                rs.getString("selected_answer"),
                rs.getString("correct_answer"),
                rs.getInt("wrong_count"),
                rs.getInt("correct_streak"),
                rs.getBoolean("mastered"),
                rs.getObject("first_wrong_at", LocalDateTime.class),
                rs.getObject("last_wrong_at", LocalDateTime.class),
                rs.getObject("last_reviewed_at", LocalDateTime.class)
        );
    }

    /**
     * 安全获取字符串值（异常时返回空字符串）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @return 字符串值，异常则返回空字符串
     */
    private String getString(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException ex) {
            return "";
        }
    }

    /**
     * 将JSON字符串解析为字符串列表
     *
     * @param json JSON字符串
     * @return 字符串列表
     */
    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param value 对象
     * @return JSON字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public record CourseQuestionBankQuestionSeed(
            String type,
            String stem,
            List<String> options,
            String answer,
            String explanation,
            String difficultyLabel,
            String sourceUrl,
            int sortOrder
    ) {
    }

    public record TypeWarriorVocabularyRow(
            long questionId,
            String setCode,
            String stem,
            String answer,
            String familiarity
    ) {
    }

    public record SingleChoiceQuestionBankRow(
            String setCode,
            String title,
            String categoryName,
            int questionCount
    ) {
    }

    public record CourseQuestionAnswerReference(
            long questionId,
            String setCode,
            String questionType,
            String answer
    ) {
    }

    public record QuestionBankMistakeState(
            int wrongCount,
            int correctStreak,
            boolean mastered
    ) {
    }

    private record QuestionBankMistakeTotals(
            long total,
            long active,
            long mastered
    ) {
    }

    private record CourseQuestionBankCategoryBuilder(
            String code,
            String name,
            String description,
            List<CourseQuestionBankSetResponse> sets
    ) {
        private CourseQuestionBankCategoryBuilder(String code, String name, String description) {
            this(code, name, description, new ArrayList<>());
        }
    }
}
