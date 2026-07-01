# OJ Module File Map

This document records how to enter the OJ page, where OJ files live, and what each file is responsible for.

## How To Open The OJ Page

Start the backend:

```powershell
cd StudyPlatform-back
.\mvnw.cmd spring-boot:run
```

Start the frontend:

```powershell
cd studyplatform-vue
npm run dev
```

Open:

```text
http://127.0.0.1:5173/oj.html
```

The OJ page is a separate Vite page. It does not replace or modify the existing home page at `index.html`.

If the backend is not on `http://localhost:8080`, set the frontend API base URL:

```powershell
$env:VITE_API_BASE_URL='http://localhost:8081'
npm run dev
```

## Backend Layout

Root:

```text
StudyPlatform-back/src/main/java/com/cupk/oj
```

Files:

```text
config/
  OjAsyncConfig.java
  OjProperties.java

controller/
  OjProblemController.java
  OjSubmissionController.java

dto/
  CreateProblemRequest.java
  UpdateProblemRequest.java
  CreateTestCaseRequest.java
  CreateSubmissionRequest.java
  ProblemSummary.java
  JudgeResult.java
  JudgeCaseResult.java

model/
  OjProblem.java
  OjTestCase.java
  OjSubmission.java
  OjSubmissionCase.java
  ProblemDifficulty.java
  ProblemStatus.java
  SubmissionStatus.java

repository/
  OjProblemRepository.java
  OjTestCaseRepository.java
  OjSubmissionRepository.java
  OjSubmissionCaseRepository.java

service/
  OjProblemService.java
  OjTestCaseService.java
  OjSubmissionService.java
  OjJudgeService.java
  JudgeSandboxClient.java
  RemoteJudgeSandboxClient.java
```

Responsibilities:

- `config/OjAsyncConfig.java`: enables async judging so submission requests can return quickly.
- `config/OjProperties.java`: binds `oj.sandbox-url` from application configuration.
- `controller/OjProblemController.java`: exposes problem and test case REST APIs.
- `controller/OjSubmissionController.java`: exposes submission and judge result REST APIs.
- `dto/*Request.java`: validates incoming API payloads.
- `dto/ProblemSummary.java`: compact problem list response.
- `dto/JudgeResult.java`: aggregate judge result returned by the sandbox boundary.
- `dto/JudgeCaseResult.java`: per-test-case judge result returned by the sandbox boundary.
- `model/OjProblem.java`: problem statement, limits, tags, and status.
- `model/OjTestCase.java`: input/output cases for a problem.
- `model/OjSubmission.java`: user submission and aggregate judge status.
- `model/OjSubmissionCase.java`: per-case result for one submission.
- `model/*Status.java` and `model/*Difficulty.java`: allowed enum values.
- `repository/*Repository.java`: JDBC persistence for the OJ tables.
- `service/OjProblemService.java`: problem business operations.
- `service/OjTestCaseService.java`: test case business operations.
- `service/OjSubmissionService.java`: creates submissions and schedules judging after transaction commit.
- `service/OjJudgeService.java`: loads submission context, calls the judge boundary, and persists judge results.
- `service/JudgeSandboxClient.java`: interface that isolates the backend from the concrete judge implementation.
- `service/RemoteJudgeSandboxClient.java`: remote sandbox adapter; also includes local `answer` mode for development tests.

Database migrations:

```text
StudyPlatform-back/src/main/resources/db/migration/V2__create_oj_tables.sql
StudyPlatform-back/src/main/resources/db/migration/V3__seed_oj_sample_problems.sql
```

- `V2__create_oj_tables.sql`: creates problem, test case, submission, and submission case tables.
- `V3__seed_oj_sample_problems.sql`: inserts sample problems and test cases for local testing.

Shared backend configuration:

```text
StudyPlatform-back/src/main/java/com/cupk/config/SecurityConfig.java
StudyPlatform-back/src/main/resources/application.properties
```

- `SecurityConfig.java`: permits OJ APIs during development and configures CORS for Vite.
- `application.properties`: contains `oj.sandbox-url`.

## Frontend Layout

Root:

```text
studyplatform-vue
```

Files:

```text
oj.html
src/oj/main.js
src/oj/api.js
src/oj/oj.css
src/pages/OjPlatform.vue
vite.config.js
```

Responsibilities:

- `oj.html`: separate HTML entry for the OJ page.
- `src/oj/main.js`: mounts the OJ Vue app to `#oj-app`.
- `src/oj/api.js`: wraps OJ backend API calls.
- `src/oj/oj.css`: OJ-only styles; does not affect the existing home page.
- `src/pages/OjPlatform.vue`: OJ page UI, including problem list, statement, submission editor, and result view.
- `vite.config.js`: registers `index.html` and `oj.html` as build entries.

## Judge Sandbox Layout

Root:

```text
judge-sandbox
```

Files:

```text
package.json
Dockerfile
README.md
src/server.js
```

Responsibilities:

- `src/server.js`: standalone HTTP service that implements `POST /judge` and runs C++ test cases.
- `package.json`: Node service metadata and `npm start` command.
- `Dockerfile`: Linux container image with Node and `g++`.
- `README.md`: sandbox startup, compiler, Docker, and security notes.

## Current Test Mode

The built-in `answer` language is for local demonstration only. It does not execute user code. It compares each submitted output block with the expected output.

Separate multiple test case outputs with a line containing only:

```text
---
```

Example for `A + B Problem`:

```text
3
---
6
```

Production Java/C++/Python judging must be implemented in a separate sandbox service and configured with:

```properties
oj.sandbox-url=http://localhost:9000
```

The project now includes a development C++ sandbox in `judge-sandbox`.
