package com.cupk.academy.controller;

import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankDetailResponse;
import com.cupk.academy.dto.QuestionBankFavoritePageResponse;
import com.cupk.academy.dto.QuestionBankFavoriteRequest;
import com.cupk.academy.dto.QuestionBankFavoriteSummaryResponse;
import com.cupk.academy.dto.QuestionBankFavoriteToggleResponse;
import com.cupk.academy.dto.QuestionBankMistakeAnswerRequest;
import com.cupk.academy.dto.QuestionBankMistakeAnswerResponse;
import com.cupk.academy.dto.QuestionBankMistakePageResponse;
import com.cupk.academy.dto.QuestionBankMistakeSummaryResponse;
import com.cupk.academy.dto.QuestionBankImportResponse;
import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.dto.TypeWarriorWordPoolResponse;
import com.cupk.academy.service.QuestionBankService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库控制器
 * 提供科目列表、题目查询、错题管理、收藏管理、课程题库等相关接口
 */
@RestController
@RequestMapping("/api/academy/question-bank")
public class QuestionBankController {
    private static final long DEFAULT_USER_ID = 1L;

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    /**
     * 获取科目列表
     * @return 科目列表响应
     */
    @GetMapping("/subjects")
    public List<QuestionBankSubjectResponse> listSubjects() {
        return questionBankService.listSubjects();
    }

    /**
     * 分页查询题目列表
     * @param subject 科目编码，可选
     * @param keyword 关键词，可选
     * @param difficulty 难度等级，可选
     * @param page 页码，默认0
     * @param size 每页数量，默认12
     * @return 题目分页响应
     */
    @GetMapping("/problems")
    public QuestionBankProblemPageResponse listProblems(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return questionBankService.listProblems(subject, keyword, difficulty, page, size);
    }

    /**
     * 获取题目详情
     * @param id 题目ID
     * @return 题目详情响应
     */
    @GetMapping("/problems/{id}")
    public QuestionBankProblemResponse getProblem(@PathVariable long id) {
        return questionBankService.getProblem(id);
    }

    /**
     * 获取课程题库目录
     * @return 课程题库分类列表
     */
    @GetMapping("/course-catalog")
    public List<CourseQuestionBankCategoryResponse> listCourseCatalog() {
        return questionBankService.listCourseQuestionBankCatalog();
    }

    /**
     * 获取错题统计摘要
     * @param userId 用户ID，从请求头获取，可选
     * @return 错题统计摘要响应
     */
    @GetMapping("/mistakes/summary")
    public QuestionBankMistakeSummaryResponse getMistakeSummary(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getMistakeSummary(resolveUserId(userId));
    }

    /**
     * 分页查询错题列表
     * @param setCode 题库编码，可选
     * @param status 状态，默认active
     * @param keyword 关键词，可选
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @param userId 用户ID，从请求头获取，可选
     * @return 错题分页响应
     */
    @GetMapping("/mistakes")
    public QuestionBankMistakePageResponse listMistakes(
            @RequestParam(required = false) String setCode,
            @RequestParam(defaultValue = "active") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.listMistakes(resolveUserId(userId), setCode, status, keyword, page, size);
    }

    /**
     * 记录错题作答
     * @param request 错题作答请求
     * @param userId 用户ID，从请求头获取，可选
     * @return 错题作答响应
     */
    @PostMapping("/mistakes/answers")
    public QuestionBankMistakeAnswerResponse recordMistakeAnswer(
            @RequestBody QuestionBankMistakeAnswerRequest request,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.recordMistakeAnswer(resolveUserId(userId), request);
    }

    /**
     * 获取收藏统计摘要
     * @param userId 用户ID，从请求头获取，可选
     * @return 收藏统计摘要响应
     */
    @GetMapping("/favorites/summary")
    public QuestionBankFavoriteSummaryResponse getFavoriteSummary(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getFavoriteSummary(resolveUserId(userId));
    }

    /**
     * 分页查询收藏列表
     * @param setCode 题库编码，可选
     * @param keyword 关键词，可选
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @param userId 用户ID，从请求头获取，可选
     * @return 收藏分页响应
     */
    @GetMapping("/favorites")
    public QuestionBankFavoritePageResponse listFavorites(
            @RequestParam(required = false) String setCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.listFavorites(resolveUserId(userId), setCode, keyword, page, size);
    }

    /**
     * 添加收藏
     * @param request 收藏请求
     * @param userId 用户ID，从请求头获取，可选
     * @return 收藏状态响应
     */
    @PostMapping("/favorites")
    public QuestionBankFavoriteToggleResponse addFavorite(
            @RequestBody QuestionBankFavoriteRequest request,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.addFavorite(resolveUserId(userId), request);
    }

    /**
     * 取消收藏
     * @param questionId 题目ID
     * @param userId 用户ID，从请求头获取，可选
     * @return 收藏状态响应
     */
    @DeleteMapping("/favorites/{questionId}")
    public QuestionBankFavoriteToggleResponse removeFavorite(
            @PathVariable long questionId,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.removeFavorite(resolveUserId(userId), questionId);
    }

    /**
     * 获取课程题库详情
     * @param code 课程编码
     * @param page 页码，默认0
     * @param size 每页数量，默认30
     * @param keyword 关键词，可选
     * @param userId 用户ID，从请求头获取，可选
     * @return 课程题库详情响应
     */
    @GetMapping("/courses/{code}")
    public CourseQuestionBankDetailResponse getCourseQuestionBank(
            @PathVariable String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getCourseQuestionBank(code, page, size, keyword, resolveUserId(userId));
    }

    /**
     * 获取打字勇士词库
     * @param userId 用户ID，从请求头获取，可选
     * @return 词库响应
     */
    @GetMapping("/type-warrior/words")
    public TypeWarriorWordPoolResponse getTypeWarriorWordPool(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getTypeWarriorWordPool(resolveUserId(userId));
    }

    /**
     * 从洛谷导入题目
     * @param pages 导入页数，默认1
     * @param limit 每页数量，默认20
     * @return 导入结果响应
     */
    @PostMapping("/import/luogu")
    public QuestionBankImportResponse importLuoguProblems(
            @RequestParam(defaultValue = "1") int pages,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return questionBankService.importLuoguProblems(pages, limit);
    }

    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
