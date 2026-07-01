package com.cupk.oj.service;

import com.cupk.oj.config.OjProperties;
import com.cupk.oj.dto.JudgeCaseResult;
import com.cupk.oj.dto.JudgeResult;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.OjTestCase;
import com.cupk.oj.model.SubmissionStatus;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Calls a remote judge sandbox.
 *
 * When no sandbox URL is configured, only the non-executing `answer` mode is supported for local demos.
 */
@Component
public class RemoteJudgeSandboxClient implements JudgeSandboxClient {
    private final OjProperties properties;
    private final RestClient.Builder restClientBuilder;

    public RemoteJudgeSandboxClient(OjProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public JudgeResult judge(OjProblem problem, OjSubmission submission, List<OjTestCase> testCases) {
        if (!StringUtils.hasText(properties.getSandboxUrl())) {
            if ("answer".equalsIgnoreCase(submission.language())) {
                return judgeAnswerSubmission(submission, testCases);
            }
            return new JudgeResult(
                    SubmissionStatus.SYSTEM_ERROR,
                    0,
                    null,
                    null,
                    "OJ sandbox is not configured. Set oj.sandbox-url to enable real judging.",
                    List.of()
            );
        }
        var client = restClientBuilder.baseUrl(properties.getSandboxUrl()).build();
        return client.post()
                .uri("/judge")
                .body(JudgeSandboxRequest.from(problem, submission, testCases))
                .retrieve()
                .body(JudgeResult.class);
    }

    private JudgeResult judgeAnswerSubmission(OjSubmission submission, List<OjTestCase> testCases) {
        String[] submittedOutputs = submission.sourceCode().split("(?m)^---\\s*$", -1);
        int totalWeight = testCases.stream().mapToInt(OjTestCase::weight).sum();
        int acceptedWeight = 0;
        int maxTimeUsedMs = 0;
        int maxMemoryUsedKb = 0;

        var caseResults = new java.util.ArrayList<JudgeCaseResult>();
        for (int i = 0; i < testCases.size(); i++) {
            OjTestCase testCase = testCases.get(i);
            String actual = i < submittedOutputs.length ? submittedOutputs[i] : "";
            boolean accepted = normalizeOutput(actual).equals(normalizeOutput(testCase.expectedOutput()));
            if (accepted) {
                acceptedWeight += testCase.weight();
            }
            int timeUsedMs = accepted ? 1 : 0;
            int memoryUsedKb = 0;
            maxTimeUsedMs = Math.max(maxTimeUsedMs, timeUsedMs);
            maxMemoryUsedKb = Math.max(maxMemoryUsedKb, memoryUsedKb);
            caseResults.add(new JudgeCaseResult(
                    testCase.id(),
                    accepted ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER,
                    timeUsedMs,
                    memoryUsedKb,
                    accepted ? "Accepted" : "Wrong answer"
            ));
        }

        int score = totalWeight == 0 ? 0 : acceptedWeight * 100 / totalWeight;
        SubmissionStatus status = acceptedWeight == totalWeight ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER;
        return new JudgeResult(
                status,
                score,
                maxTimeUsedMs,
                maxMemoryUsedKb,
                status == SubmissionStatus.ACCEPTED ? "Accepted by answer checker" : "Some cases failed",
                caseResults
        );
    }

    private String normalizeOutput(String output) {
        return output
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+(?=\\n)", "")
                .strip();
    }

    private record JudgeSandboxRequest(
            Long problemId,
            Long submissionId,
            String language,
            String sourceCode,
            Integer timeLimitMs,
            Integer memoryLimitKb,
            List<JudgeSandboxCase> cases
    ) {
        static JudgeSandboxRequest from(OjProblem problem, OjSubmission submission, List<OjTestCase> testCases) {
            return new JudgeSandboxRequest(
                    problem.id(),
                    submission.id(),
                    submission.language(),
                    submission.sourceCode(),
                    problem.timeLimitMs(),
                    problem.memoryLimitKb(),
                    testCases.stream().map(JudgeSandboxCase::from).toList()
            );
        }
    }

    private record JudgeSandboxCase(
            Long testCaseId,
            String inputData,
            String expectedOutput,
            Integer weight
    ) {
        static JudgeSandboxCase from(OjTestCase testCase) {
            return new JudgeSandboxCase(
                    testCase.id(),
                    testCase.inputData(),
                    testCase.expectedOutput(),
                    testCase.weight()
            );
        }
    }
}
