package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyExamAnswerRequest;
import com.cupk.academy.dto.AcademyExamDetailResponse;
import com.cupk.academy.dto.AcademyExamQuestionResponse;
import com.cupk.academy.dto.AcademyExamQuestionResultResponse;
import com.cupk.academy.dto.AcademyExamSubmitResponse;
import com.cupk.academy.dto.AcademyExamSummaryResponse;
import com.cupk.academy.repository.AcademyExamRepository;
import com.cupk.academy.repository.AcademyExamRepository.ExamDetailRow;
import com.cupk.academy.repository.AcademyExamRepository.ExamQuestionRow;
import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.service.OjSubmissionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademyExamService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyExamRepository examRepository;
    private final OjSubmissionService ojSubmissionService;

    public AcademyExamService(
            AcademyExamRepository examRepository,
            OjSubmissionService ojSubmissionService
    ) {
        this.examRepository = examRepository;
        this.ojSubmissionService = ojSubmissionService;
    }

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

    private ExamDetailRow findExam(String examCode) {
        return examRepository.findExamByCode(examCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考试不存在"));
    }

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

    private Map<String, Object> safeAnswers(AcademyExamAnswerRequest request) {
        return request == null || request.answers() == null ? Map.of() : request.answers();
    }

    private void ensureExamCanStart(ExamDetailRow exam) {
        LocalDateTime now = LocalDateTime.now();
        if ("已结束".equals(exam.status()) || (exam.deadline() != null && exam.deadline().isBefore(now))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试已结束，不能进入考试");
        }
        if (exam.startsAt() != null && exam.startsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试尚未开始");
        }
    }

    private void ensureExamCanAcceptAnswers(ExamDetailRow exam) {
        ensureExamCanStart(exam);
    }

    private void ensureExamStarted(String status) {
        if (!"in_progress".equals(status) && !"draft".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先开始考试");
        }
    }

    private void ensureExamNotSubmitted(String status) {
        if ("graded".equals(status) || "pending_review".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试已提交，不能重复提交");
        }
    }

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

    private String defaultWrongMessage(ExamQuestionRow question) {
        if (question.explanation() == null || question.explanation().isBlank()) {
            return "自动批改未通过";
        }
        return question.explanation();
    }

    private String normalizeText(Object value) {
        return String.valueOf(value == null ? "" : value).trim().replaceAll("\\s+", " ").toLowerCase();
    }

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

    private String resolveLanguage(Object submittedAnswer) {
        if (submittedAnswer instanceof Map<?, ?> answerMap) {
            Object language = answerMap.get("language");
            if (language != null && !String.valueOf(language).isBlank()) {
                return String.valueOf(language);
            }
        }
        return "cpp";
    }

    private int scoreOf(ExamQuestionRow question) {
        return question.score() == null ? 0 : question.score();
    }

    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
