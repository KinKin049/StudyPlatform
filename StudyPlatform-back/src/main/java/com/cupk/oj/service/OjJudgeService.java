package com.cupk.oj.service;

import com.cupk.oj.dto.JudgeResult;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjTestCase;
import com.cupk.oj.model.SubmissionStatus;
import com.cupk.oj.repository.OjProblemRepository;
import com.cupk.oj.repository.OjSubmissionCaseRepository;
import com.cupk.oj.repository.OjSubmissionRepository;
import com.cupk.oj.repository.OjTestCaseRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OjJudgeService {
    private final OjProblemRepository problemRepository;
    private final OjSubmissionRepository submissionRepository;
    private final OjTestCaseRepository testCaseRepository;
    private final OjSubmissionCaseRepository submissionCaseRepository;
    private final JudgeSandboxClient judgeSandboxClient;

    public OjJudgeService(
            OjProblemRepository problemRepository,
            OjSubmissionRepository submissionRepository,
            OjTestCaseRepository testCaseRepository,
            OjSubmissionCaseRepository submissionCaseRepository,
            JudgeSandboxClient judgeSandboxClient
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionCaseRepository = submissionCaseRepository;
        this.judgeSandboxClient = judgeSandboxClient;
    }

    @Async
    @Transactional
    public void judgeSubmission(Long submissionId) {
        submissionRepository.updateStatus(submissionId, SubmissionStatus.JUDGING, null);
        try {
            OjSubmission submission = submissionRepository.findById(submissionId).orElseThrow();
            OjProblem problem = problemRepository.findById(submission.problemId()).orElseThrow();
            List<OjTestCase> testCases = testCaseRepository.findByProblemId(problem.id());
            if (testCases.isEmpty()) {
                submissionRepository.updateStatus(submissionId, SubmissionStatus.SYSTEM_ERROR, "Problem has no test cases");
                return;
            }
            JudgeResult result = judgeSandboxClient.judge(problem, submission, testCases);
            if (result == null) {
                submissionRepository.updateStatus(submissionId, SubmissionStatus.SYSTEM_ERROR, "Judge sandbox returned empty result");
                return;
            }
            submissionRepository.updateJudgeResult(submissionId, result);
            if (result.cases() != null && !result.cases().isEmpty()) {
                submissionCaseRepository.createAll(submissionId, result.cases());
            }
        } catch (Exception ex) {
            submissionRepository.updateStatus(submissionId, SubmissionStatus.SYSTEM_ERROR, ex.getMessage());
        }
    }
}
