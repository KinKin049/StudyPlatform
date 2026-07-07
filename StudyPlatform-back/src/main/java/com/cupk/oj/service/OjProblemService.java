package com.cupk.oj.service;

import com.cupk.oj.dto.CreateProblemRequest;
import com.cupk.oj.dto.ProblemSummary;
import com.cupk.oj.dto.UpdateProblemRequest;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.ProblemStatus;
import com.cupk.oj.repository.OjProblemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * OJ题目服务，提供题目列表查询、详情查看、创建和更新等功能。
 */
@Service
public class OjProblemService {
    private final OjProblemRepository problemRepository;

    /**
     * 构造函数，注入题目数据访问层。
     *
     * @param problemRepository 题目数据访问层
     */
    public OjProblemService(OjProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /**
     * 查询题目列表，支持多条件筛选。
     *
     * @param status      题目状态
     * @param keyword     关键词
     * @param tags        标签
     * @param difficulties 难度
     * @param languages   编程语言
     * @return 题目摘要列表
     */
    public List<ProblemSummary> listProblems(
            ProblemStatus status,
            String keyword,
            String tags,
            String difficulties,
            String languages
    ) {
        return problemRepository.findAll(status, keyword, tags, difficulties, languages);
    }

    /**
     * 根据ID获取题目详情。
     *
     * @param id 题目ID
     * @return 题目对象
     */
    public OjProblem getProblem(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
    }

    /**
     * 创建新题目。
     *
     * @param request 创建请求对象
     * @return 创建后的题目对象
     */
    @Transactional
    public OjProblem createProblem(CreateProblemRequest request) {
        Long id = problemRepository.create(request);
        return getProblem(id);
    }

    /**
     * 更新题目信息。
     *
     * @param id      题目ID
     * @param request 更新请求对象
     * @return 更新后的题目对象
     */
    @Transactional
    public OjProblem updateProblem(Long id, UpdateProblemRequest request) {
        int updated = problemRepository.update(id, request);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        return getProblem(id);
    }
}
