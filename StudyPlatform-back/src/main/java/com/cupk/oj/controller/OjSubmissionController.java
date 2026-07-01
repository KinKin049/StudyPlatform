package com.cupk.oj.controller;

import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjSubmissionCase;
import com.cupk.oj.service.OjSubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oj/submissions")
public class OjSubmissionController {
    private final OjSubmissionService submissionService;

    public OjSubmissionController(OjSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OjSubmission createSubmission(@Valid @RequestBody CreateSubmissionRequest request) {
        return submissionService.createSubmission(request);
    }

    @GetMapping("/{id}")
    public OjSubmission getSubmission(@PathVariable Long id) {
        return submissionService.getSubmission(id);
    }

    @GetMapping
    public List<OjSubmission> listSubmissions(@RequestParam Long problemId) {
        return submissionService.listSubmissions(problemId);
    }

    @GetMapping("/{id}/cases")
    public List<OjSubmissionCase> listSubmissionCases(@PathVariable Long id) {
        return submissionService.listSubmissionCases(id);
    }
}
