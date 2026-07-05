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

@RestController
@RequestMapping("/api/academy/question-bank")
public class QuestionBankController {
    private static final long DEFAULT_USER_ID = 1L;

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping("/subjects")
    public List<QuestionBankSubjectResponse> listSubjects() {
        return questionBankService.listSubjects();
    }

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

    @GetMapping("/problems/{id}")
    public QuestionBankProblemResponse getProblem(@PathVariable long id) {
        return questionBankService.getProblem(id);
    }

    @GetMapping("/course-catalog")
    public List<CourseQuestionBankCategoryResponse> listCourseCatalog() {
        return questionBankService.listCourseQuestionBankCatalog();
    }

    @GetMapping("/mistakes/summary")
    public QuestionBankMistakeSummaryResponse getMistakeSummary(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getMistakeSummary(resolveUserId(userId));
    }

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

    @PostMapping("/mistakes/answers")
    public QuestionBankMistakeAnswerResponse recordMistakeAnswer(
            @RequestBody QuestionBankMistakeAnswerRequest request,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.recordMistakeAnswer(resolveUserId(userId), request);
    }

    @GetMapping("/favorites/summary")
    public QuestionBankFavoriteSummaryResponse getFavoriteSummary(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getFavoriteSummary(resolveUserId(userId));
    }

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

    @PostMapping("/favorites")
    public QuestionBankFavoriteToggleResponse addFavorite(
            @RequestBody QuestionBankFavoriteRequest request,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.addFavorite(resolveUserId(userId), request);
    }

    @DeleteMapping("/favorites/{questionId}")
    public QuestionBankFavoriteToggleResponse removeFavorite(
            @PathVariable long questionId,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.removeFavorite(resolveUserId(userId), questionId);
    }

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

    @GetMapping("/type-warrior/words")
    public TypeWarriorWordPoolResponse getTypeWarriorWordPool(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionBankService.getTypeWarriorWordPool(resolveUserId(userId));
    }

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
