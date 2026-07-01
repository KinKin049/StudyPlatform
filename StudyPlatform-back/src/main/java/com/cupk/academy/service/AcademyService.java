package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.repository.AcademyRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class AcademyService {
    private final AcademyRepository academyRepository;

    public AcademyService(AcademyRepository academyRepository) {
        this.academyRepository = academyRepository;
    }

    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return withCourseCovers(academyRepository.findOnlineOpenCourses());
    }

    public List<AcademyCourseResponse> listGeneralCourses() {
        return withCourseCovers(academyRepository.findGeneralCourses());
    }

    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return withCourseCovers(academyRepository.findMicroMajorCourses());
    }

    public List<AcademyTextbookResponse> listTextbooks() {
        return academyRepository.findTextbooks().stream()
                .map(textbook -> new AcademyTextbookResponse(
                        textbook.id(),
                        textbook.name(),
                        textbook.editor(),
                        textbook.category(),
                        textbook.publisher(),
                        textbook.publishDate(),
                        textbook.isbn(),
                        textbook.description(),
                        fileUrl(textbook.coverFilePath()),
                        textbook.coverUrl(),
                        textbook.coverFilePath(),
                        textbook.link()
                ))
                .toList();
    }

    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyRepository.findCategories("online_open_courses");
    }

    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyRepository.findCategories("general_courses");
    }

    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyRepository.findCategories("micro_major_courses");
    }

    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyRepository.findCategories("excellent_textbooks");
    }

    private List<AcademyCourseResponse> withCourseCovers(List<AcademyCourseResponse> courses) {
        return courses.stream()
                .map(course -> new AcademyCourseResponse(
                        course.id(),
                        course.name(),
                        course.teacher(),
                        course.category(),
                        course.school(),
                        fileUrl(course.coverFilePath()),
                        course.coverUrl(),
                        course.coverFilePath(),
                        course.startTime(),
                        course.participants(),
                        course.comment(),
                        course.description(),
                        course.link()
                ))
                .toList();
    }

    private String fileUrl(String coverFilePath) {
        if (coverFilePath == null || coverFilePath.isBlank()) {
            return "";
        }
        String normalizedPath = coverFilePath.replace("\\", "/");
        if (normalizedPath.startsWith("storage/")) {
            normalizedPath = normalizedPath.substring("storage/".length());
        }
        String encodedPath = java.util.Arrays.stream(normalizedPath.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        return "/files/" + encodedPath;
    }
}
