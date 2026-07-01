# OJ Module

This backend module provides the business API for online judge workflows. It stores problems, test cases, submissions, and judge results. It does not execute untrusted user code inside the Spring Boot process.

For the complete cross-project file map and page entry method, see `../docs/oj-file-map.md`.

## Architecture

- `com.cupk.oj.controller`: REST API for problems, test cases, and submissions.
- `com.cupk.oj.service`: Business orchestration and async judge dispatch.
- `com.cupk.oj.repository`: JDBC persistence.
- `com.cupk.oj.model`: Domain records and status enums.
- `com.cupk.oj.dto`: Request and response DTOs.
- `JudgeSandboxClient`: boundary for a real sandbox or judge service.

## Database

Migration:

```text
src/main/resources/db/migration/V2__create_oj_tables.sql
```

Tables:

- `oj_problems`: problem statement, limits, tags, and publish status.
- `oj_test_cases`: inputs and expected outputs for each problem.
- `oj_submissions`: submitted source code and aggregate judge result.
- `oj_submission_cases`: per-test-case judge result.

## REST API

Problem APIs:

```http
GET    /api/oj/problems?status=PUBLISHED
GET    /api/oj/problems/{id}
POST   /api/oj/problems
PUT    /api/oj/problems/{id}
```

Test case APIs:

```http
GET    /api/oj/problems/{problemId}/test-cases
POST   /api/oj/problems/{problemId}/test-cases
DELETE /api/oj/problems/{problemId}/test-cases/{testCaseId}
```

Submission APIs:

```http
POST   /api/oj/submissions
GET    /api/oj/submissions/{id}
GET    /api/oj/submissions?problemId={problemId}
GET    /api/oj/submissions/{id}/cases
```

## Sandbox Contract

Configure the judge service URL:

```properties
oj.sandbox-url=http://localhost:9000
```

The backend sends:

```http
POST /judge
Content-Type: application/json
```

Request shape:

```json
{
  "problemId": 1,
  "submissionId": 10,
  "language": "java",
  "sourceCode": "class Main { ... }",
  "timeLimitMs": 1000,
  "memoryLimitKb": 262144,
  "cases": [
    {
      "testCaseId": 1,
      "inputData": "1 2\n",
      "expectedOutput": "3\n",
      "weight": 1
    }
  ]
}
```

Expected response shape:

```json
{
  "status": "ACCEPTED",
  "score": 100,
  "timeUsedMs": 64,
  "memoryUsedKb": 32768,
  "message": "Accepted",
  "cases": [
    {
      "testCaseId": 1,
      "status": "ACCEPTED",
      "timeUsedMs": 64,
      "memoryUsedKb": 32768,
      "message": "Accepted"
    }
  ]
}
```

Supported statuses:

```text
PENDING, JUDGING, ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED,
MEMORY_LIMIT_EXCEEDED, RUNTIME_ERROR, COMPILE_ERROR, SYSTEM_ERROR
```

## Implementation Notes

- Submission creation returns `202 Accepted`. Judging starts after the database transaction commits.
- If `oj.sandbox-url` is empty, submissions are marked `SYSTEM_ERROR` with a clear configuration message.
- Keep user code execution in a separate sandbox process or service. Use Docker, cgroups, seccomp, filesystem isolation, process limits, and network isolation for production judging.
- Add authentication/authorization before exposing problem management and hidden test cases to real users.

## Next Steps

1. Run the development C++ sandbox in `../judge-sandbox`.
2. Harden the sandbox before production use.
3. Hide non-sample test case input/output from student-facing APIs.
4. Add role checks for creating problems and test cases.
5. Add pagination and filtering for submissions when data volume grows.
