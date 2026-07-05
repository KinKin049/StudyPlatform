package com.cupk.oj.service;

import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjSubmissionCase;
import com.cupk.oj.repository.OjProblemRepository;
import com.cupk.oj.repository.OjSubmissionCaseRepository;
import com.cupk.oj.repository.OjSubmissionRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OjSubmissionService {
    private static final long DEFAULT_USER_ID = 1L;

    private final OjProblemRepository problemRepository;
    private final OjSubmissionRepository submissionRepository;
    private final OjSubmissionCaseRepository submissionCaseRepository;
    private final OjJudgeService judgeService;
    private final JdbcTemplate jdbcTemplate;

    public OjSubmissionService(
            OjProblemRepository problemRepository,
            OjSubmissionRepository submissionRepository,
            OjSubmissionCaseRepository submissionCaseRepository,
            OjJudgeService judgeService,
            JdbcTemplate jdbcTemplate
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.submissionCaseRepository = submissionCaseRepository;
        this.judgeService = judgeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OjSubmission createSubmission(CreateSubmissionRequest request) {
        if (!problemRepository.existsById(request.problemId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        CreateSubmissionRequest normalizedRequest = new CreateSubmissionRequest(
                request.problemId(),
                normalizeUserId(request.userId()),
                request.language(),
                request.sourceCode()
        );
        Long id = submissionRepository.create(normalizedRequest);
        dispatchJudgeAfterCommit(id);
        return getSubmission(id);
    }

    public OjSubmission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    public List<OjSubmission> listSubmissions(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        return submissionRepository.findByProblemId(problemId);
    }

    public List<OjSubmissionCase> listSubmissionCases(Long submissionId) {
        getSubmission(submissionId);
        return submissionCaseRepository.findBySubmissionId(submissionId);
    }

    private void dispatchJudgeAfterCommit(Long submissionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            judgeService.judgeSubmission(submissionId);
            return;
        }
        // Judge only after commit so the async worker can read the saved submission.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                judgeService.judgeSubmission(submissionId);
            }
        });
    }

    private Long normalizeUserId(Long userId) {
        Long candidateUserId = userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
        ensurePlatformUserFromAuth(candidateUserId);
        if (userExists(candidateUserId)) {
            return candidateUserId;
        }
        ensureDefaultUser();
        if (userExists(DEFAULT_USER_ID)) {
            return DEFAULT_USER_ID;
        }
        return null;
    }

    private void ensurePlatformUserFromAuth(Long userId) {
        if (userId == null || userId <= 0 || userExists(userId)) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
                SELECT id, username, password_hash, username,
                       CASE WHEN role_type = 'teacher' THEN 'TEACHER' ELSE 'STUDENT' END,
                       1
                FROM auth_users
                WHERE id = ?
                """,
                userId
        );
    }

    private boolean userExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    private void ensureDefaultUser() {
        jdbcTemplate.update("""
                INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
                VALUES (?, 'local_default_student_1', 'local-default-password', '默认学生', 'STUDENT', 1)
                """, DEFAULT_USER_ID);
    }
}
