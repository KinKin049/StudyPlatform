package com.cupk.oj.service;

import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjSubmissionCase;
import com.cupk.oj.repository.OjProblemRepository;
import com.cupk.oj.repository.OjSubmissionCaseRepository;
import com.cupk.oj.repository.OjSubmissionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OjSubmissionService {
    private final OjProblemRepository problemRepository;
    private final OjSubmissionRepository submissionRepository;
    private final OjSubmissionCaseRepository submissionCaseRepository;
    private final OjJudgeService judgeService;

    public OjSubmissionService(
            OjProblemRepository problemRepository,
            OjSubmissionRepository submissionRepository,
            OjSubmissionCaseRepository submissionCaseRepository,
            OjJudgeService judgeService
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.submissionCaseRepository = submissionCaseRepository;
        this.judgeService = judgeService;
    }

    @Transactional
    public OjSubmission createSubmission(CreateSubmissionRequest request) {
        if (!problemRepository.existsById(request.problemId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        Long id = submissionRepository.create(request);
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
}
