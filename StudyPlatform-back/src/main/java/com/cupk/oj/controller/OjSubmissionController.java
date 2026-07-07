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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OJ提交控制器
 * 提供代码提交、提交查询、测试用例结果查询等相关接口
 */
@RestController
@RequestMapping("/api/oj/submissions")
public class OjSubmissionController {
    private final OjSubmissionService submissionService;

    public OjSubmissionController(OjSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * 创建代码提交
     * @param userId 用户ID，从请求头获取，可选
     * @param request 提交请求，包含题目ID、用户ID、语言和源代码
     * @return 提交记录
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OjSubmission createSubmission(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateSubmissionRequest request
    ) {
        CreateSubmissionRequest scopedRequest = new CreateSubmissionRequest(
                request.problemId(),
                userId == null || userId <= 0 ? request.userId() : userId,
                request.language(),
                request.sourceCode()
        );
        return submissionService.createSubmission(scopedRequest);
    }

    /**
     * 获取提交详情
     * @param id 提交ID
     * @return 提交记录详情
     */
    @GetMapping("/{id}")
    public OjSubmission getSubmission(@PathVariable Long id) {
        return submissionService.getSubmission(id);
    }

    /**
     * 查询题目提交列表
     * @param problemId 题目ID
     * @return 提交记录列表
     */
    @GetMapping
    public List<OjSubmission> listSubmissions(@RequestParam Long problemId) {
        return submissionService.listSubmissions(problemId);
    }

    /**
     * 查询提交的测试用例结果
     * @param id 提交ID
     * @return 测试用例结果列表
     */
    @GetMapping("/{id}/cases")
    public List<OjSubmissionCase> listSubmissionCases(@PathVariable Long id) {
        return submissionService.listSubmissionCases(id);
    }
}
