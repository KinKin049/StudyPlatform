package com.cupk.oj.service;

import com.cupk.oj.dto.CreateTestCaseRequest;
import com.cupk.oj.model.OjTestCase;
import com.cupk.oj.repository.OjProblemRepository;
import com.cupk.oj.repository.OjTestCaseRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OjTestCaseService {
    private final OjProblemRepository problemRepository;
    private final OjTestCaseRepository testCaseRepository;

    public OjTestCaseService(OjProblemRepository problemRepository, OjTestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<OjTestCase> listTestCases(Long problemId) {
        ensureProblemExists(problemId);
        return testCaseRepository.findByProblemId(problemId);
    }

    @Transactional
    public OjTestCase createTestCase(Long problemId, CreateTestCaseRequest request) {
        ensureProblemExists(problemId);
        Long id = testCaseRepository.create(problemId, request);
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test case was not saved"));
    }

    @Transactional
    public void deleteTestCase(Long problemId, Long testCaseId) {
        ensureProblemExists(problemId);
        OjTestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case not found"));
        if (!testCase.problemId().equals(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case not found");
        }
        testCaseRepository.delete(testCaseId);
    }

    private void ensureProblemExists(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
    }
}
