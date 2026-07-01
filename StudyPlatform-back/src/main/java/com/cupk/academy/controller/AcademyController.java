package com.cupk.academy.controller;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.service.AcademyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService academyService;

    public AcademyController(AcademyService academyService) {
        this.academyService = academyService;
    }

    @GetMapping("/online-open-courses")
    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return academyService.listOnlineOpenCourses();
    }

    @GetMapping("/online-open-courses/categories")
    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyService.listOnlineOpenCourseCategories();
    }

    @GetMapping("/general-courses")
    public List<AcademyCourseResponse> listGeneralCourses() {
        return academyService.listGeneralCourses();
    }

    @GetMapping("/general-courses/categories")
    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyService.listGeneralCourseCategories();
    }

    @GetMapping("/micro-major-courses")
    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return academyService.listMicroMajorCourses();
    }

    @GetMapping("/micro-major-courses/categories")
    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyService.listMicroMajorCourseCategories();
    }

    @GetMapping("/textbooks")
    public List<AcademyTextbookResponse> listTextbooks() {
        return academyService.listTextbooks();
    }

    @GetMapping("/textbooks/categories")
    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyService.listTextbookCategories();
    }
}
