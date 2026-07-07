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

/**
 * OJ题目控制器
 * 提供题目列表查询、题目详情、题目创建与更新、测试用例管理等相关接口
 */
@RestController
@RequestMapping("/api/oj/problems")
public class OjProblemController {
    private final OjProblemService problemService;
    private final OjTestCaseService testCaseService;

    public OjProblemController(OjProblemService problemService, OjTestCaseService testCaseService) {
        this.problemService = problemService;
        this.testCaseService = testCaseService;
    }

    /**
     * 查询题目列表
     * @param status 题目状态，可选
     * @param keyword 关键词，可选
     * @param tags 标签，可选
     * @param difficulties 难度等级，可选
     * @param languages 支持语言，可选
     * @return 题目摘要列表
     */
    @GetMapping
    public List<ProblemSummary> listProblems(
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String difficulties,
            @RequestParam(required = false) String languages
    ) {
        return problemService.listProblems(status, keyword, tags, difficulties, languages);
    }

    /**
     * 获取题目详情
     * @param id 题目ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public OjProblem getProblem(@PathVariable Long id) {
        return problemService.getProblem(id);
    }

    /**
     * 创建新题目
     * @param request 题目创建请求
     * @return 创建后的题目详情
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OjProblem createProblem(@Valid @RequestBody CreateProblemRequest request) {
        return problemService.createProblem(request);
    }

    /**
     * 更新题目信息
     * @param id 题目ID
     * @param request 题目更新请求
     * @return 更新后的题目详情
     */
    @PutMapping("/{id}")
    public OjProblem updateProblem(@PathVariable Long id, @Valid @RequestBody UpdateProblemRequest request) {
        return problemService.updateProblem(id, request);
    }

    /**
     * 查询题目的测试用例列表
     * @param problemId 题目ID
     * @return 测试用例列表
     */
    @GetMapping("/{problemId}/test-cases")
    public List<OjTestCase> listTestCases(@PathVariable Long problemId) {
        return testCaseService.listTestCases(problemId);
    }

    /**
     * 为题目添加测试用例
     * @param problemId 题目ID
     * @param request 测试用例创建请求
     * @return 创建后的测试用例
     */
    @PostMapping("/{problemId}/test-cases")
    @ResponseStatus(HttpStatus.CREATED)
    public OjTestCase createTestCase(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateTestCaseRequest request
    ) {
        return testCaseService.createTestCase(problemId, request);
    }

    /**
     * 删除题目测试用例
     * @param problemId 题目ID
     * @param testCaseId 测试用例ID
     */
    @DeleteMapping("/{problemId}/test-cases/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTestCase(@PathVariable Long problemId, @PathVariable Long testCaseId) {
        testCaseService.deleteTestCase(problemId, testCaseId);
    }
}
