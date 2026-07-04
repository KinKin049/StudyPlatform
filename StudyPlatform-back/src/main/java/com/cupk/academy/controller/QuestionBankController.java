package com.cupk.academy.controller;

import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankDetailResponse;
import com.cupk.academy.dto.QuestionBankImportResponse;
import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.service.QuestionBankService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academy/question-bank")
public class QuestionBankController {
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

    @GetMapping("/courses/{code}")
    public CourseQuestionBankDetailResponse getCourseQuestionBank(
            @PathVariable String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String keyword
    ) {
        return questionBankService.getCourseQuestionBank(code, page, size, keyword);
    }

    @PostMapping("/import/luogu")
    public QuestionBankImportResponse importLuoguProblems(
            @RequestParam(defaultValue = "1") int pages,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return questionBankService.importLuoguProblems(pages, limit);
    }
}
