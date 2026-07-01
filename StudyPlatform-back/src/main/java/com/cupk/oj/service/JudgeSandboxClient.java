package com.cupk.oj.service;

import com.cupk.oj.dto.JudgeResult;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjTestCase;
import java.util.List;

/**
 * Boundary for judge execution.
 *
 * Implementations must keep untrusted user code outside the Spring Boot process.
 */
public interface JudgeSandboxClient {
    JudgeResult judge(OjProblem problem, OjSubmission submission, List<OjTestCase> testCases);
}
