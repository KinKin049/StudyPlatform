package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyAssignmentAnswerRequest;
import com.cupk.academy.dto.AcademyAssignmentDetailResponse;
import com.cupk.academy.dto.AcademyAssignmentQuestionResponse;
import com.cupk.academy.dto.AcademyAssignmentQuestionResultResponse;
import com.cupk.academy.dto.AcademyAssignmentSubmitResponse;
import com.cupk.academy.dto.AcademyAssignmentSummaryResponse;
import com.cupk.academy.repository.AcademyAssignmentRepository;
import com.cupk.academy.repository.AcademyAssignmentRepository.AssignmentDetailRow;
import com.cupk.academy.repository.AcademyAssignmentRepository.AssignmentQuestionRow;
import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.service.OjSubmissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademyAssignmentService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyAssignmentRepository assignmentRepository;
    private final OjSubmissionService ojSubmissionService;

    public AcademyAssignmentService(
            AcademyAssignmentRepository assignmentRepository,
            OjSubmissionService ojSubmissionService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.ojSubmissionService = ojSubmissionService;
    }

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

    private AssignmentDetailRow findAssignment(String assignmentCode) {
        return assignmentRepository.findAssignmentByCode(assignmentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作业不存在"));
    }

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

    private Map<String, Object> safeAnswers(AcademyAssignmentAnswerRequest request) {
        return request == null || request.answers() == null ? Map.of() : request.answers();
    }

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

    private String defaultWrongMessage(AssignmentQuestionRow question) {
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

    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
