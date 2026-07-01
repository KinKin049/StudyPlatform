package com.cupk.oj.controller;

import com.cupk.oj.dto.CreateProblemRequest;
import com.cupk.oj.dto.CreateTestCaseRequest;
import com.cupk.oj.dto.ProblemSummary;
import com.cupk.oj.dto.UpdateProblemRequest;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.OjTestCase;
import com.cupk.oj.model.ProblemStatus;
import com.cupk.oj.service.OjProblemService;
import com.cupk.oj.service.OjTestCaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oj/problems")
public class OjProblemController {
    private final OjProblemService problemService;
    private final OjTestCaseService testCaseService;

    public OjProblemController(OjProblemService problemService, OjTestCaseService testCaseService) {
        this.problemService = problemService;
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public List<ProblemSummary> listProblems(@RequestParam(required = false) ProblemStatus status) {
        return problemService.listProblems(status);
    }

    @GetMapping("/{id}")
    public OjProblem getProblem(@PathVariable Long id) {
        return problemService.getProblem(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OjProblem createProblem(@Valid @RequestBody CreateProblemRequest request) {
        return problemService.createProblem(request);
    }

    @PutMapping("/{id}")
    public OjProblem updateProblem(@PathVariable Long id, @Valid @RequestBody UpdateProblemRequest request) {
        return problemService.updateProblem(id, request);
    }

    @GetMapping("/{problemId}/test-cases")
    public List<OjTestCase> listTestCases(@PathVariable Long problemId) {
        return testCaseService.listTestCases(problemId);
    }

    @PostMapping("/{problemId}/test-cases")
    @ResponseStatus(HttpStatus.CREATED)
    public OjTestCase createTestCase(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateTestCaseRequest request
    ) {
        return testCaseService.createTestCase(problemId, request);
    }

    @DeleteMapping("/{problemId}/test-cases/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTestCase(@PathVariable Long problemId, @PathVariable Long testCaseId) {
        testCaseService.deleteTestCase(problemId, testCaseId);
    }
}
