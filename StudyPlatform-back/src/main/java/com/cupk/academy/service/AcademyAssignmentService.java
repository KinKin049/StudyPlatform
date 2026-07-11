package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyAssignmentAnswerRequest;
import com.cupk.academy.dto.AcademyAssignmentDetailResponse;
import com.cupk.academy.dto.AcademyAssignmentQuestionResponse;
import com.cupk.academy.dto.AcademyAssignmentQuestionResultResponse;
import com.cupk.academy.dto.AcademyAssignmentSubmitResponse;
import com.cupk.academy.dto.AcademyAssignmentSummaryResponse;
import com.cupk.academy.dto.TeacherAssignmentCreateRequest;
import com.cupk.academy.dto.TeacherAssignmentQuestionRequest;
import com.cupk.academy.repository.AcademyAssignmentRepository;
import com.cupk.academy.repository.AcademyAssignmentRepository.AssignmentDetailRow;
import com.cupk.academy.repository.AcademyAssignmentRepository.AssignmentQuestionRow;
import com.cupk.academy.repository.AcademyRepository;
import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.service.OjSubmissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 作业服务，提供作业列表查询、详情查看、保存草稿、提交批改等功能。
 */
@Service
public class AcademyAssignmentService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyAssignmentRepository assignmentRepository;
    private final AcademyRepository academyRepository;
    private final AuthUserRepository authUserRepository;
    private final OjSubmissionService ojSubmissionService;

    /**
     * 构造函数，注入依赖的仓库和服务。
     *
     * @param assignmentRepository 作业数据访问层
     * @param ojSubmissionService  OJ提交服务
     */
    public AcademyAssignmentService(
            AcademyAssignmentRepository assignmentRepository,
            AcademyRepository academyRepository,
            AuthUserRepository authUserRepository,
            OjSubmissionService ojSubmissionService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.academyRepository = academyRepository;
        this.authUserRepository = authUserRepository;
        this.ojSubmissionService = ojSubmissionService;
    }

    public AcademyAssignmentDetailResponse createTeacherAssignment(Long userId, TeacherAssignmentCreateRequest request) {
        long teacherUserId = requireTeacher(userId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "作业内容不能为空");
        }
        String courseId = clean(request.courseId(), 120);
        String title = clean(request.title(), 255);
        if (courseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程不能为空");
        }
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "作业标题不能为空");
        }

        var course = academyRepository.findPublishedCourseForTeacher(teacherUserId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在或不属于当前教师"));
        List<TeacherAssignmentQuestionRequest> questions = normalizeTeacherQuestions(request.questions());
        int totalScore = questions.stream()
                .mapToInt(question -> normalizeScore(question.score()))
                .sum();
        String assignmentCode = "ta-" + teacherUserId + "-" + System.currentTimeMillis();
        long assignmentId = assignmentRepository.createAssignment(
                assignmentCode,
                "online-open-courses",
                course.courseId(),
                course.courseName(),
                title,
                clean(course.teacherName(), 120),
                request.deadline(),
                request.attemptsLimit(),
                request.durationMinutes(),
                totalScore,
                cleanNullable(request.description(), 4000)
        );

        for (int index = 0; index < questions.size(); index += 1) {
            TeacherAssignmentQuestionRequest question = questions.get(index);
            String type = normalizeQuestionType(question.type());
            Object correctAnswer = normalizeCorrectAnswer(type, question.correctAnswer());
            boolean autoGradable = isAutoGradableType(type) && correctAnswer != null;
            assignmentRepository.createQuestion(
                    assignmentId,
                    index + 1,
                    type,
                    cleanNullable(question.label(), 64),
                    clean(question.title(), 4000),
                    normalizeOptions(type, question.options()),
                    cleanNullable(question.placeholder(), 1000),
                    normalizeScore(question.score()),
                    correctAnswer,
                    cleanNullable(question.explanation(), 2000),
                    autoGradable,
                    !autoGradable
            );
        }

        return getAssignment(assignmentCode, teacherUserId);
    }

    /**
     * 查询用户作业列表。
     *
     * @param userId 用户ID
     * @return 作业摘要响应列表
     */
    public List<AcademyAssignmentSummaryResponse> listAssignments(Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        return assignmentRepository.findAssignments(normalizedUserId).stream()
                .map(row -> new AcademyAssignmentSummaryResponse(
                        row.code(),
                        row.title(),
                        row.courseTitle(),
                        row.teacher(),
                        row.status(),
                        row.deadline(),
                        row.attemptsLimit(),
                        row.durationMinutes(),
                        row.totalScore(),
                        row.description(),
                        row.questionCount(),
                        row.submissionStatus(),
                        row.score(),
                        row.pendingTeacherReview()
                ))
                .toList();
    }

    /**
     * 获取作业详情，包含题目列表和用户答案。
     *
     * @param assignmentCode 作业代码
     * @param userId         用户ID
     * @return 作业详情响应对象
     */
    public AcademyAssignmentDetailResponse getAssignment(String assignmentCode, Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        AssignmentDetailRow assignment = findAssignment(assignmentCode);
        List<AssignmentQuestionRow> questions = assignmentRepository.findQuestions(assignment.id());
        Map<String, Object> draftAnswers = assignmentRepository.findLatestDraftAnswers(assignment.id(), normalizedUserId);
        var submissionStatus = assignmentRepository.findLatestSubmissionStatus(assignment.id(), normalizedUserId);

        return new AcademyAssignmentDetailResponse(
                assignment.code(),
                assignment.title(),
                assignment.courseTitle(),
                assignment.teacher(),
                assignment.status(),
                assignment.deadline(),
                assignment.attemptsLimit(),
                assignment.durationMinutes(),
                assignment.totalScore(),
                assignment.description(),
                questions.stream().map(this::toQuestionResponse).toList(),
                draftAnswers,
                submissionStatus.map(AcademyAssignmentRepository.SubmissionStatusRow::status).orElse(null),
                submissionStatus.map(AcademyAssignmentRepository.SubmissionStatusRow::score).orElse(null),
                questions.stream().anyMatch(question -> Boolean.TRUE.equals(question.requiresTeacherReview()))
        );
    }

    /**
     * 保存作业草稿。
     *
     * @param assignmentCode 作业代码
     * @param request        答题请求对象
     * @return 提交响应对象
     */
    public AcademyAssignmentSubmitResponse saveDraft(String assignmentCode, AcademyAssignmentAnswerRequest request) {
        AssignmentDetailRow assignment = findAssignment(assignmentCode);
        assignmentRepository.saveDraft(assignment.id(), normalizeUserId(request == null ? null : request.userId()), safeAnswers(request));
        return new AcademyAssignmentSubmitResponse(
                "draft",
                null,
                null,
                assignment.totalScore(),
                true,
                "作业草稿保存成功",
                List.of()
        );
    }

    /**
     * 提交作业并进行自动批改。
     *
     * @param assignmentCode 作业代码
     * @param request        答题请求对象
     * @return 提交响应对象，包含批改结果
     */
    public AcademyAssignmentSubmitResponse submitAssignment(String assignmentCode, AcademyAssignmentAnswerRequest request) {
        AssignmentDetailRow assignment = findAssignment(assignmentCode);
        List<AssignmentQuestionRow> questions = assignmentRepository.findQuestions(assignment.id());
        Map<String, Object> answers = safeAnswers(request);
        List<AcademyAssignmentQuestionResultResponse> results = new ArrayList<>();
        int autoScore = 0;
        int pendingScore = 0;
        boolean pendingTeacherReview = false;
        Long normalizedUserId = normalizeUserId(request == null ? null : request.userId());

        for (AssignmentQuestionRow question : questions) {
            Object submittedAnswer = answers.get(String.valueOf(question.id()));
            if (Boolean.TRUE.equals(question.requiresTeacherReview())) {
                pendingTeacherReview = true;
                pendingScore += question.score();
                AcademyAssignmentQuestionResultResponse pendingResult = createPendingReviewResult(
                        question,
                        submittedAnswer,
                        normalizedUserId
                );
                results.add(new AcademyAssignmentQuestionResultResponse(
                        pendingResult.questionId(),
                        pendingResult.status(),
                        pendingResult.score(),
                        pendingResult.maxScore(),
                        pendingResult.pendingTeacherReview(),
                        pendingResult.message()
                ));
                continue;
            }

            boolean accepted = Boolean.TRUE.equals(question.autoGradable()) && isAnswerAccepted(question, submittedAnswer);
            int questionScore = accepted ? question.score() : 0;
            autoScore += questionScore;
            results.add(new AcademyAssignmentQuestionResultResponse(
                    question.id(),
                    accepted ? "accepted" : "wrong_answer",
                    questionScore,
                    question.score(),
                    false,
                    accepted ? "自动批改正确" : defaultWrongMessage(question)
            ));
        }

        int currentScore = autoScore;
        assignmentRepository.saveSubmission(
                assignment.id(),
                normalizedUserId,
                answers,
                currentScore,
                pendingTeacherReview
        );

        return new AcademyAssignmentSubmitResponse(
                pendingTeacherReview ? "pending_review" : "graded",
                currentScore,
                autoScore,
                pendingScore,
                pendingTeacherReview,
                pendingTeacherReview ? "客观题已自动批改，主观题或编程题待教师审核" : "作业提交并批改完成",
                results
        );
    }

    /**
     * 创建待教师审核的题目结果。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @param userId         用户ID
     * @return 题目结果响应对象
     */
    private AcademyAssignmentQuestionResultResponse createPendingReviewResult(
            AssignmentQuestionRow question,
            Object submittedAnswer,
            Long userId
    ) {
        if (!"code".equals(question.type()) || question.ojProblemId() == null) {
            return new AcademyAssignmentQuestionResultResponse(
                    question.id(),
                    "pending_review",
                    0,
                    question.score(),
                    true,
                    "待教师批改"
            );
        }

        String sourceCode = buildOjSource(question, submittedAnswer);
        if (sourceCode.isBlank()) {
            return new AcademyAssignmentQuestionResultResponse(
                    question.id(),
                    "pending_oj_and_review",
                    0,
                    question.score(),
                    true,
                    "编程题未填写代码，待补交或教师审核"
            );
        }

        try {
            OjSubmission submission = ojSubmissionService.createSubmission(new CreateSubmissionRequest(
                    question.ojProblemId(),
                    userId,
                    resolveLanguage(submittedAnswer),
                    sourceCode
            ));
            return new AcademyAssignmentQuestionResultResponse(
                    question.id(),
                    "pending_oj_and_review",
                    0,
                    question.score(),
                    true,
                    "已提交至 OJ 判题，提交编号 #" + submission.id() + "，结果待教师审核"
            );
        } catch (Exception ex) {
            return new AcademyAssignmentQuestionResultResponse(
                    question.id(),
                    "oj_submit_failed",
                    0,
                    question.score(),
                    true,
                    "OJ 判题提交失败：" + ex.getMessage()
            );
        }
    }

    /**
     * 根据作业代码查找作业详情。
     *
     * @param assignmentCode 作业代码
     * @return 作业详情行数据
     */
    private AssignmentDetailRow findAssignment(String assignmentCode) {
        return assignmentRepository.findAssignmentByCode(assignmentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作业不存在"));
    }

    /**
     * 将题目行数据转换为响应对象。
     *
     * @param question 题目行数据
     * @return 题目响应对象
     */
    private AcademyAssignmentQuestionResponse toQuestionResponse(AssignmentQuestionRow question) {
        return new AcademyAssignmentQuestionResponse(
                question.id(),
                question.type(),
                question.label(),
                question.title(),
                question.options(),
                question.placeholder(),
                question.score(),
                question.autoGradable(),
                question.ojProblemId(),
                question.requiresTeacherReview(),
                question.explanation()
        );
    }

    /**
     * 获取安全的答案映射，处理空值情况。
     *
     * @param request 答题请求对象
     * @return 答案映射
     */
    private Map<String, Object> safeAnswers(AcademyAssignmentAnswerRequest request) {
        return request == null || request.answers() == null ? Map.of() : request.answers();
    }

    /**
     * 判断答案是否正确。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @return 是否正确
     */
    private boolean isAnswerAccepted(AssignmentQuestionRow question, Object submittedAnswer) {
        if (submittedAnswer == null || question.correctAnswer() == null) {
            return false;
        }
        return switch (question.type()) {
            case "single" -> normalizeText(submittedAnswer).equals(normalizeText(question.correctAnswer()));
            case "multiple" -> normalizeList(submittedAnswer).equals(normalizeList(question.correctAnswer()));
            case "blank" -> normalizeList(question.correctAnswer()).contains(normalizeText(submittedAnswer));
            default -> false;
        };
    }

    /**
     * 获取默认的答错提示信息。
     *
     * @param question 题目信息
     * @return 提示信息
     */
    private String defaultWrongMessage(AssignmentQuestionRow question) {
        if (question.explanation() == null || question.explanation().isBlank()) {
            return "自动批改未通过";
        }
        return question.explanation();
    }

    /**
     * 规范化文本，去除多余空格并转为小写。
     *
     * @param value 原始值
     * @return 规范化后的文本
     */
    private String normalizeText(Object value) {
        return String.valueOf(value == null ? "" : value).trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * 规范化列表，去除空值并排序。
     *
     * @param value 原始值
     * @return 规范化后的列表
     */
    private List<String> normalizeList(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(this::normalizeText)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
        return List.of(normalizeText(value));
    }

    private List<TeacherAssignmentQuestionRequest> normalizeTeacherQuestions(List<TeacherAssignmentQuestionRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少添加一道题目");
        }
        List<TeacherAssignmentQuestionRequest> normalized = questions.stream()
                .filter(Objects::nonNull)
                .toList();
        if (normalized.isEmpty() || normalized.size() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目数量需在 1-50 道之间");
        }
        for (TeacherAssignmentQuestionRequest question : normalized) {
            String type = normalizeQuestionType(question.type());
            if (clean(question.title(), 4000).isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目标题不能为空");
            }
            if (("single".equals(type) || "multiple".equals(type)) && normalizeOptions(type, question.options()).size() < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选择题至少需要两个选项");
            }
        }
        return normalized;
    }

    private String normalizeQuestionType(String type) {
        return switch (type == null ? "" : type.trim().toLowerCase(Locale.ROOT)) {
            case "single", "multiple", "blank", "short" -> type.trim().toLowerCase(Locale.ROOT);
            default -> "short";
        };
    }

    private boolean isAutoGradableType(String type) {
        return "single".equals(type) || "multiple".equals(type) || "blank".equals(type);
    }

    private List<String> normalizeOptions(String type, List<String> options) {
        if (!"single".equals(type) && !"multiple".equals(type)) {
            return List.of();
        }
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> clean(option, 255))
                .filter(option -> !option.isBlank())
                .limit(8)
                .toList();
    }

    private Object normalizeCorrectAnswer(String type, Object answer) {
        if (answer == null) {
            return null;
        }
        if ("multiple".equals(type)) {
            List<String> values = normalizeList(answer).stream()
                    .filter(value -> !value.isBlank())
                    .toList();
            return values.isEmpty() ? null : values;
        }
        String value = normalizeText(answer);
        return value.isBlank() ? null : value;
    }

    private int normalizeScore(Integer score) {
        if (score == null || score <= 0) {
            return 1;
        }
        return Math.min(score, 100);
    }

    private long requireTeacher(Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        var user = authUserRepository.findById(normalizedUserId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (!"teacher".equals(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教师可以布置作业");
        }
        return normalizedUserId;
    }

    private String clean(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }

    private String cleanNullable(String value, int maxLength) {
        String cleaned = clean(value, maxLength);
        return cleaned.isBlank() ? null : cleaned;
    }

    /**
     * 构建 OJ 提交的源代码。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @return 源代码
     */
    private String buildOjSource(AssignmentQuestionRow question, Object submittedAnswer) {
        String sourceCode = extractSourceCode(submittedAnswer).trim();
        if (sourceCode.isBlank() || sourceCode.contains(" main(") || sourceCode.contains("\nmain(")) {
            return sourceCode;
        }
        if (question.title() != null && question.title().contains("maxOfThree")) {
            return """
                    #include <bits/stdc++.h>
                    using namespace std;

                    %s

                    int main() {
                        int a, b, c;
                        if (!(cin >> a >> b >> c)) {
                            return 0;
                        }
                        cout << maxOfThree(a, b, c);
                        return 0;
                    }
                    """.formatted(sourceCode);
        }
        return sourceCode;
    }

    /**
     * 从答案中提取源代码。
     *
     * @param submittedAnswer 用户答案
     * @return 源代码
     */
    private String extractSourceCode(Object submittedAnswer) {
        if (submittedAnswer instanceof Map<?, ?> answerMap) {
            Object sourceCode = answerMap.get("sourceCode");
            if (sourceCode == null) {
                sourceCode = answerMap.get("code");
            }
            return sourceCode == null ? "" : String.valueOf(sourceCode);
        }
        return submittedAnswer == null ? "" : String.valueOf(submittedAnswer);
    }

    /**
     * 解析编程语言。
     *
     * @param submittedAnswer 用户答案
     * @return 编程语言
     */
    private String resolveLanguage(Object submittedAnswer) {
        if (submittedAnswer instanceof Map<?, ?> answerMap) {
            Object language = answerMap.get("language");
            if (language != null && !String.valueOf(language).isBlank()) {
                return String.valueOf(language);
            }
        }
        return "cpp";
    }

    /**
     * 规范化用户ID，空值或无效值时返回默认用户ID。
     *
     * @param userId 用户ID
     * @return 规范化后的用户ID
     */
    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
