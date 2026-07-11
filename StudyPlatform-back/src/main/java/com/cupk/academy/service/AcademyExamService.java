package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyExamAnswerRequest;
import com.cupk.academy.dto.AcademyExamDetailResponse;
import com.cupk.academy.dto.AcademyExamQuestionResponse;
import com.cupk.academy.dto.AcademyExamQuestionResultResponse;
import com.cupk.academy.dto.AcademyRandomExamRequest;
import com.cupk.academy.dto.AcademyExamSubmitResponse;
import com.cupk.academy.dto.AcademyExamSummaryResponse;
import com.cupk.academy.repository.AcademyExamRepository;
import com.cupk.academy.repository.AcademyExamRepository.ExamDetailRow;
import com.cupk.academy.repository.AcademyExamRepository.ExamQuestionRow;
import com.cupk.academy.repository.AcademyExamRepository.RandomExamQuestionRow;
import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.service.OjSubmissionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 考试服务，提供考试列表查询、详情查看、开始考试、保存草稿、提交批改等功能。
 */
@Service
public class AcademyExamService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyExamRepository examRepository;
    private final OjSubmissionService ojSubmissionService;

    /**
     * 构造函数，注入依赖的仓库和服务。
     *
     * @param examRepository       考试数据访问层
     * @param ojSubmissionService  OJ提交服务
     */
    public AcademyExamService(
            AcademyExamRepository examRepository,
            OjSubmissionService ojSubmissionService
    ) {
        this.examRepository = examRepository;
        this.ojSubmissionService = ojSubmissionService;
    }

    /**
     * 查询用户考试列表。
     *
     * @param userId 用户ID
     * @return 考试摘要响应列表
     */
    public List<AcademyExamSummaryResponse> listExams(Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        return examRepository.findExams(normalizedUserId).stream()
                .map(row -> new AcademyExamSummaryResponse(
                        row.code(),
                        row.title(),
                        row.courseTitle(),
                        row.teacher(),
                        row.status(),
                        row.startsAt(),
                        row.deadline(),
                        row.attemptsLimit(),
                        row.durationMinutes(),
                        row.totalScore(),
                        row.description(),
                        row.questionCount(),
                        row.submissionStatus(),
                        row.score(),
                        row.pendingTeacherReview(),
                        row.startedAt(),
                        row.submittedAt()
                ))
                .toList();
    }

    /**
     * 获取考试详情，包含题目列表和用户答案。
     *
     * @param examCode 考试代码
     * @param userId   用户ID
     * @return 考试详情响应对象
     */
    public AcademyExamDetailResponse getExam(String examCode, Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        ExamDetailRow exam = findExam(examCode);
        List<ExamQuestionRow> questions = examRepository.findQuestions(exam.id());
        Map<String, Object> latestAnswers = examRepository.findLatestAnswers(exam.id(), normalizedUserId);
        var submissionStatus = examRepository.findLatestSubmissionStatus(exam.id(), normalizedUserId);

        return new AcademyExamDetailResponse(
                exam.code(),
                exam.title(),
                exam.courseTitle(),
                exam.teacher(),
                exam.status(),
                exam.startsAt(),
                exam.deadline(),
                exam.attemptsLimit(),
                exam.durationMinutes(),
                exam.totalScore(),
                exam.description(),
                questions.stream().map(this::toQuestionResponse).toList(),
                latestAnswers,
                submissionStatus.map(AcademyExamRepository.SubmissionStatusRow::status).orElse(null),
                submissionStatus.map(AcademyExamRepository.SubmissionStatusRow::score).orElse(null),
                questions.stream().anyMatch(question -> Boolean.TRUE.equals(question.requiresTeacherReview())),
                submissionStatus.map(AcademyExamRepository.SubmissionStatusRow::startedAt).orElse(null),
                submissionStatus.map(AcademyExamRepository.SubmissionStatusRow::submittedAt).orElse(null)
        );
    }

    /**
     * 开始考试，记录开始时间。
     *
     * @param examCode 考试代码
     * @param userId   用户ID
     * @return 考试详情响应对象
     */
    public AcademyExamDetailResponse startExam(String examCode, Long userId) {
        Long normalizedUserId = normalizeUserId(userId);
        ExamDetailRow exam = findExam(examCode);
        ensureExamCanStart(exam);
        var latestStatus = examRepository.findLatestSubmissionStatus(exam.id(), normalizedUserId);

        if (latestStatus.isEmpty()) {
            examRepository.startExam(exam.id(), normalizedUserId);
        }

        return getExam(examCode, normalizedUserId);
    }

    /**
     * 保存考试草稿。
     *
     * @param examCode 考试代码
     * @param request  答题请求对象
     * @return 提交响应对象
     */
    public AcademyExamSubmitResponse saveDraft(String examCode, AcademyExamAnswerRequest request) {
        ExamDetailRow exam = findExam(examCode);
        Long normalizedUserId = normalizeUserId(request == null ? null : request.userId());
        ensureExamCanAcceptAnswers(exam);
        var latestStatus = examRepository.findLatestSubmissionStatus(exam.id(), normalizedUserId);
        ensureExamNotSubmitted(latestStatus.map(AcademyExamRepository.SubmissionStatusRow::status).orElse(null));
        ensureExamStarted(latestStatus.map(AcademyExamRepository.SubmissionStatusRow::status).orElse(null));
        examRepository.saveDraft(
                exam.id(),
                normalizedUserId,
                safeAnswers(request),
                latestStatus.map(AcademyExamRepository.SubmissionStatusRow::startedAt).orElse(LocalDateTime.now())
        );
        return new AcademyExamSubmitResponse(
                "draft",
                null,
                null,
                exam.totalScore(),
                true,
                "考试草稿保存成功",
                List.of()
        );
    }

    /**
     * 提交考试并进行自动批改。
     *
     * @param examCode 考试代码
     * @param request  答题请求对象
     * @return 提交响应对象，包含批改结果
     */
    public AcademyExamSubmitResponse submitExam(String examCode, AcademyExamAnswerRequest request) {
        ExamDetailRow exam = findExam(examCode);
        Long normalizedUserId = normalizeUserId(request == null ? null : request.userId());
        ensureExamCanAcceptAnswers(exam);
        var latestStatus = examRepository.findLatestSubmissionStatus(exam.id(), normalizedUserId);
        ensureExamNotSubmitted(latestStatus.map(AcademyExamRepository.SubmissionStatusRow::status).orElse(null));
        ensureExamStarted(latestStatus.map(AcademyExamRepository.SubmissionStatusRow::status).orElse(null));

        List<ExamQuestionRow> questions = examRepository.findQuestions(exam.id());
        Map<String, Object> answers = safeAnswers(request);
        List<AcademyExamQuestionResultResponse> results = new ArrayList<>();
        int autoScore = 0;
        int pendingScore = 0;
        boolean pendingTeacherReview = false;

        for (ExamQuestionRow question : questions) {
            Object submittedAnswer = answers.get(String.valueOf(question.id()));
            if (Boolean.TRUE.equals(question.requiresTeacherReview())) {
                pendingTeacherReview = true;
                pendingScore += scoreOf(question);
                results.add(createPendingReviewResult(question, submittedAnswer, normalizedUserId));
                continue;
            }

            boolean accepted = Boolean.TRUE.equals(question.autoGradable()) && isAnswerAccepted(question, submittedAnswer);
            int questionScore = accepted ? scoreOf(question) : 0;
            autoScore += questionScore;
            results.add(new AcademyExamQuestionResultResponse(
                    question.id(),
                    accepted ? "accepted" : "wrong_answer",
                    questionScore,
                    scoreOf(question),
                    false,
                    accepted ? "自动批改正确" : defaultWrongMessage(question)
            ));
        }

        examRepository.saveSubmission(
                exam.id(),
                normalizedUserId,
                answers,
                autoScore,
                pendingTeacherReview,
                latestStatus.map(AcademyExamRepository.SubmissionStatusRow::startedAt).orElse(LocalDateTime.now())
        );

        return new AcademyExamSubmitResponse(
                pendingTeacherReview ? "pending_review" : "graded",
                autoScore,
                autoScore,
                pendingScore,
                pendingTeacherReview,
                pendingTeacherReview ? "客观题已自动批改，主观题或编程题待教师审核" : "考试提交并批改完成",
                results
        );
    }

    public AcademyExamDetailResponse createRandomExam(AcademyRandomExamRequest request) {
        Long normalizedUserId = normalizeUserId(request == null ? null : request.userId());
        int questionCount = clamp(request == null ? null : request.questionCount(), 5, 30, 10);
        int durationMinutes = clamp(request == null ? null : request.durationMinutes(), 10, 180, 45);
        List<RandomExamQuestionRow> questions =
                examRepository.findRandomQuestionsFromEnrolledCourses(normalizedUserId, questionCount);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前已选课程还没有可用于随机组卷的题库题目");
        }
        String courseTitle = randomExamCourseTitle(questions);
        String examCode = examRepository.createRandomExam(
                normalizedUserId,
                courseTitle,
                "随机组卷 · " + courseTitle,
                durationMinutes,
                questions
        );
        return getExam(examCode, normalizedUserId);
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        int safeValue = value == null ? fallback : value;
        return Math.min(Math.max(safeValue, min), max);
    }

    private String randomExamCourseTitle(List<RandomExamQuestionRow> questions) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (RandomExamQuestionRow question : questions) {
            if (question.courseTitle() != null && !question.courseTitle().isBlank()) {
                names.add(question.courseTitle());
            }
            if (names.size() >= 2) {
                break;
            }
        }
        if (names.isEmpty()) {
            return "已选课程";
        }
        if (names.size() == 1) {
            return names.iterator().next();
        }
        return String.join(" / ", names);
    }

    /**
     * 创建待教师审核的题目结果。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @param userId         用户ID
     * @return 题目结果响应对象
     */
    private AcademyExamQuestionResultResponse createPendingReviewResult(
            ExamQuestionRow question,
            Object submittedAnswer,
            Long userId
    ) {
        if (!"code".equals(question.type()) || question.ojProblemId() == null) {
            return new AcademyExamQuestionResultResponse(
                    question.id(),
                    "pending_review",
                    0,
                    scoreOf(question),
                    true,
                    "待教师批改"
            );
        }

        String sourceCode = buildOjSource(question, submittedAnswer);
        if (sourceCode.isBlank()) {
            return new AcademyExamQuestionResultResponse(
                    question.id(),
                    "pending_oj_and_review",
                    0,
                    scoreOf(question),
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
            return new AcademyExamQuestionResultResponse(
                    question.id(),
                    "pending_oj_and_review",
                    0,
                    scoreOf(question),
                    true,
                    "已提交至 OJ 判题，提交编号 #" + submission.id() + "，结果待教师审核"
            );
        } catch (Exception ex) {
            return new AcademyExamQuestionResultResponse(
                    question.id(),
                    "oj_submit_failed",
                    0,
                    scoreOf(question),
                    true,
                    "OJ 判题提交失败：" + ex.getMessage()
            );
        }
    }

    /**
     * 根据考试代码查找考试详情。
     *
     * @param examCode 考试代码
     * @return 考试详情行数据
     */
    private ExamDetailRow findExam(String examCode) {
        return examRepository.findExamByCode(examCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试不存在"));
    }

    /**
     * 将题目行数据转换为响应对象。
     *
     * @param question 题目行数据
     * @return 题目响应对象
     */
    private AcademyExamQuestionResponse toQuestionResponse(ExamQuestionRow question) {
        return new AcademyExamQuestionResponse(
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
    private Map<String, Object> safeAnswers(AcademyExamAnswerRequest request) {
        return request == null || request.answers() == null ? Map.of() : request.answers();
    }

    /**
     * 确保考试可以开始，检查考试状态和时间。
     *
     * @param exam 考试详情
     */
    private void ensureExamCanStart(ExamDetailRow exam) {
        LocalDateTime now = LocalDateTime.now();
        if ("已结束".equals(exam.status()) || (exam.deadline() != null && exam.deadline().isBefore(now))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试已结束，不能进入考试");
        }
        if (exam.startsAt() != null && exam.startsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试尚未开始");
        }
    }

    /**
     * 确保考试可以接收答案。
     *
     * @param exam 考试详情
     */
    private void ensureExamCanAcceptAnswers(ExamDetailRow exam) {
        ensureExamCanStart(exam);
    }

    /**
     * 确保考试已开始。
     *
     * @param status 当前状态
     */
    private void ensureExamStarted(String status) {
        if (!"in_progress".equals(status) && !"draft".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先开始考试");
        }
    }

    /**
     * 确保考试未提交。
     *
     * @param status 当前状态
     */
    private void ensureExamNotSubmitted(String status) {
        if ("graded".equals(status) || "pending_review".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试已提交，不能重复提交");
        }
    }

    /**
     * 判断答案是否正确。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @return 是否正确
     */
    private boolean isAnswerAccepted(ExamQuestionRow question, Object submittedAnswer) {
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
    private String defaultWrongMessage(ExamQuestionRow question) {
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

    /**
     * 构建 OJ 提交的源代码。
     *
     * @param question       题目信息
     * @param submittedAnswer 用户答案
     * @return 源代码
     */
    private String buildOjSource(ExamQuestionRow question, Object submittedAnswer) {
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
     * 获取题目的分数。
     *
     * @param question 题目信息
     * @return 分数
     */
    private int scoreOf(ExamQuestionRow question) {
        return question.score() == null ? 0 : question.score();
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
