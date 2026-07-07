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

/**
 * OJ提交服务，提供代码提交、查询提交记录和测试用例结果等功能。
 */
@Service
public class OjSubmissionService {
    private static final long DEFAULT_USER_ID = 1L;

    private final OjProblemRepository problemRepository;
    private final OjSubmissionRepository submissionRepository;
    private final OjSubmissionCaseRepository submissionCaseRepository;
    private final OjJudgeService judgeService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数，注入依赖的仓库、服务和模板。
     *
     * @param problemRepository      题目数据访问层
     * @param submissionRepository   提交记录数据访问层
     * @param submissionCaseRepository 提交测试用例数据访问层
     * @param judgeService           判题服务
     * @param jdbcTemplate           JDBC模板
     */
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

    /**
     * 创建代码提交，提交后触发异步判题。
     *
     * @param request 提交请求对象
     * @return 提交记录对象
     */
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

    /**
     * 根据ID获取提交记录。
     *
     * @param id 提交记录ID
     * @return 提交记录对象
     */
    public OjSubmission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    /**
     * 根据题目ID查询提交记录列表。
     *
     * @param problemId 题目ID
     * @return 提交记录列表
     */
    public List<OjSubmission> listSubmissions(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        return submissionRepository.findByProblemId(problemId);
    }

    /**
     * 根据提交记录ID查询测试用例结果列表。
     *
     * @param submissionId 提交记录ID
     * @return 测试用例结果列表
     */
    public List<OjSubmissionCase> listSubmissionCases(Long submissionId) {
        getSubmission(submissionId);
        return submissionCaseRepository.findBySubmissionId(submissionId);
    }

    /**
     * 在事务提交后分发判题任务，确保异步工作线程能读取到已保存的提交记录。
     *
     * @param submissionId 提交记录ID
     */
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

    /**
     * 规范化用户ID，确保用户存在，不存在时创建默认用户。
     *
     * @param userId 用户ID
     * @return 规范化后的用户ID
     */
    private Long normalizeUserId(Long userId) {
        Long candidateUserId = userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
        if (userExists(candidateUserId)) {
            return candidateUserId;
        }
        ensureDefaultUser();
        if (userExists(DEFAULT_USER_ID)) {
            return DEFAULT_USER_ID;
        }
        return null;
    }

    /**
     * 检查用户是否存在。
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    private boolean userExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    /**
     * 确保默认用户存在，不存在时创建。
     */
    private void ensureDefaultUser() {
        jdbcTemplate.update("""
                INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
                VALUES (?, 'local_default_student_1', 'local-default-password', '默认学生', 'STUDENT', 1)
                """, DEFAULT_USER_ID);
    }
}
