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

@Service
public class OjProblemService {
    private final OjProblemRepository problemRepository;

    public OjProblemService(OjProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    public List<ProblemSummary> listProblems(ProblemStatus status) {
        return problemRepository.findAll(status);
    }

    public OjProblem getProblem(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
    }

    @Transactional
    public OjProblem createProblem(CreateProblemRequest request) {
        Long id = problemRepository.create(request);
        return getProblem(id);
    }

    @Transactional
    public OjProblem updateProblem(Long id, UpdateProblemRequest request) {
        int updated = problemRepository.update(id, request);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
        return getProblem(id);
    }
}
