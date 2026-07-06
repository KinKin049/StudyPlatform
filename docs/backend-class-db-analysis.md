# Backend Class and Database Schema Analysis

Source: `StudyPlatform-back/src/main/java` and MySQL `study_platform` `information_schema`.

## Summary

- Java source files: 168; package-info files: 6
- Java top-level types: 162
- Java nested types: 40
- Java declared types total: 202
- Top-level type kinds: class: 59, enum: 3, interface: 1, record: 99
- Database tables: 43
- Database columns: 453

## Java Top-Level Types

### `com.cupk` (1)

- `StudyPlatformBackApplication` (class) - `StudyPlatform-back/src/main/java/com/cupk/StudyPlatformBackApplication.java:7`

### `com.cupk.academy.controller` (3)

- `AcademyController` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/controller/AcademyController.java:39`
- `ProfileController` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/controller/ProfileController.java:22`
- `QuestionBankController` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/controller/QuestionBankController.java:32`

### `com.cupk.academy.dto` (57)

- `AcademyAssignmentAnswerRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentAnswerRequest.java:5`
- `AcademyAssignmentDetailResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentDetailResponse.java:7`
- `AcademyAssignmentQuestionResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentQuestionResponse.java:5`
- `AcademyAssignmentQuestionResultResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentQuestionResultResponse.java:3`
- `AcademyAssignmentSubmitResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentSubmitResponse.java:5`
- `AcademyAssignmentSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyAssignmentSummaryResponse.java:5`
- `AcademyCategoryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCategoryResponse.java:3`
- `AcademyCourseEnrollmentRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCourseEnrollmentRequest.java:3`
- `AcademyCourseEnrollmentResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCourseEnrollmentResponse.java:3`
- `AcademyCourseResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCourseResponse.java:3`
- `AcademyCourseReviewRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCourseReviewRequest.java:7`
- `AcademyCourseReviewResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyCourseReviewResponse.java:5`
- `AcademyEnrolledCourseResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyEnrolledCourseResponse.java:5`
- `AcademyExamAnswerRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamAnswerRequest.java:5`
- `AcademyExamDetailResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamDetailResponse.java:7`
- `AcademyExamQuestionResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamQuestionResponse.java:5`
- `AcademyExamQuestionResultResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamQuestionResultResponse.java:3`
- `AcademyExamSubmitResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamSubmitResponse.java:5`
- `AcademyExamSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyExamSummaryResponse.java:5`
- `AcademyHomeItemResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyHomeItemResponse.java:3`
- `AcademyHomeSectionResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyHomeSectionResponse.java:5`
- `AcademyTextbookResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/AcademyTextbookResponse.java:3`
- `CourseQuestionBankCategoryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/CourseQuestionBankCategoryResponse.java:5`
- `CourseQuestionBankDetailResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/CourseQuestionBankDetailResponse.java:5`
- `CourseQuestionBankQuestionPageResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/CourseQuestionBankQuestionPageResponse.java:5`
- `CourseQuestionBankQuestionResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/CourseQuestionBankQuestionResponse.java:5`
- `CourseQuestionBankSetResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/CourseQuestionBankSetResponse.java:5`
- `ProfileActivityDayResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileActivityDayResponse.java:3`
- `ProfileCodingDifficultyResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileCodingDifficultyResponse.java:3`
- `ProfileDifficultyResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileDifficultyResponse.java:3`
- `ProfileLearningEventRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileLearningEventRequest.java:3`
- `ProfileLearningTimeResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileLearningTimeResponse.java:3`
- `ProfileOverviewResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileOverviewResponse.java:5`
- `ProfilePreviewMetricResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfilePreviewMetricResponse.java:3`
- `ProfileRecentActivityResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileRecentActivityResponse.java:3`
- `ProfileStatResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileStatResponse.java:3`
- `ProfileTrackResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileTrackResponse.java:3`
- `ProfileUserResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileUserResponse.java:3`
- `ProfileUserUpdateRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/ProfileUserUpdateRequest.java:3`
- `QuestionBankFavoritePageResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoritePageResponse.java:5`
- `QuestionBankFavoriteRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoriteRequest.java:3`
- `QuestionBankFavoriteResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoriteResponse.java:6`
- `QuestionBankFavoriteSetSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoriteSetSummaryResponse.java:5`
- `QuestionBankFavoriteSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoriteSummaryResponse.java:5`
- `QuestionBankFavoriteToggleResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankFavoriteToggleResponse.java:3`
- `QuestionBankImportResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankImportResponse.java:3`
- `QuestionBankMistakeAnswerRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakeAnswerRequest.java:3`
- `QuestionBankMistakeAnswerResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakeAnswerResponse.java:3`
- `QuestionBankMistakePageResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakePageResponse.java:5`
- `QuestionBankMistakeResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakeResponse.java:6`
- `QuestionBankMistakeSetSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakeSetSummaryResponse.java:5`
- `QuestionBankMistakeSummaryResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankMistakeSummaryResponse.java:5`
- `QuestionBankProblemPageResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankProblemPageResponse.java:5`
- `QuestionBankProblemResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankProblemResponse.java:6`
- `QuestionBankSubjectResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/QuestionBankSubjectResponse.java:3`
- `TypeWarriorWordPoolResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/TypeWarriorWordPoolResponse.java:5`
- `TypeWarriorWordResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/dto/TypeWarriorWordResponse.java:3`

### `com.cupk.academy.repository` (5)

- `AcademyAssignmentRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyAssignmentRepository.java:17`
- `AcademyExamRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyExamRepository.java:17`
- `AcademyRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyRepository.java:17`
- `ProfileRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/ProfileRepository.java:16`
- `QuestionBankRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:35`

### `com.cupk.academy.service` (12)

- `AcademyAssignmentService` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/AcademyAssignmentService.java:25`
- `AcademyExamService` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/AcademyExamService.java:26`
- `AcademyService` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/AcademyService.java:29`
- `CetVocabularySeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/CetVocabularySeeder.java:20`
- `IdeologyQuestionBankSeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/IdeologyQuestionBankSeeder.java:22`
- `MaoismQuestionBankSeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MaoismQuestionBankSeeder.java:19`
- `MarkdownQuestionBankSeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarkdownQuestionBankSeeder.java:20`
- `MarxismQuestionBankSeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarxismQuestionBankSeeder.java:22`
- `ModernHistoryQuestionBankSeeder` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/ModernHistoryQuestionBankSeeder.java:19`
- `ProfileService` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/ProfileService.java:40`
- `QuestionBankService` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/QuestionBankService.java:47`
- `QuestionBankSourceResolver` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/QuestionBankSourceResolver.java:16`

### `com.cupk.admin.controller` (1)

- `AdminController` (class) - `StudyPlatform-back/src/main/java/com/cupk/admin/controller/AdminController.java:28`

### `com.cupk.admin.dto` (7)

- `AdminCourseRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminCourseRequest.java:3`
- `AdminCourseResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminCourseResponse.java:3`
- `AdminCourseReviewResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminCourseReviewResponse.java:5`
- `AdminQuestionBankSetRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminQuestionBankSetRequest.java:5`
- `AdminQuestionRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminQuestionRequest.java:5`
- `AdminUserResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminUserResponse.java:3`
- `AdminUserUpdateRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/dto/AdminUserUpdateRequest.java:3`

### `com.cupk.admin.repository` (1)

- `AdminRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/admin/repository/AdminRepository.java:24`

### `com.cupk.admin.service` (1)

- `AdminService` (class) - `StudyPlatform-back/src/main/java/com/cupk/admin/service/AdminService.java:21`

### `com.cupk.auth.controller` (1)

- `AuthController` (class) - `StudyPlatform-back/src/main/java/com/cupk/auth/controller/AuthController.java:22`

### `com.cupk.auth.dto` (7)

- `AuthLoginRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/AuthLoginRequest.java:8`
- `AuthMessageResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/AuthMessageResponse.java:3`
- `AuthOnboardingRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/AuthOnboardingRequest.java:8`
- `AuthRegisterRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/AuthRegisterRequest.java:12`
- `AuthUserResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/AuthUserResponse.java:8`
- `PasswordResetCodeRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/PasswordResetCodeRequest.java:6`
- `PasswordResetConfirmRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/dto/PasswordResetConfirmRequest.java:7`

### `com.cupk.auth.repository` (2)

- `AuthUserRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/auth/repository/AuthUserRepository.java:16`
- `PasswordResetCodeRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/auth/repository/PasswordResetCodeRepository.java:11`

### `com.cupk.auth.service` (1)

- `AuthService` (class) - `StudyPlatform-back/src/main/java/com/cupk/auth/service/AuthService.java:31`

### `com.cupk.config` (3)

- `ApiExceptionHandler` (class) - `StudyPlatform-back/src/main/java/com/cupk/config/ApiExceptionHandler.java:17`
- `SecurityConfig` (class) - `StudyPlatform-back/src/main/java/com/cupk/config/SecurityConfig.java:16`
- `WebMvcConfig` (class) - `StudyPlatform-back/src/main/java/com/cupk/config/WebMvcConfig.java:10`

### `com.cupk.games.controller` (3)

- `LadderJumpQuestionController` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/controller/LadderJumpQuestionController.java:18`
- `LadderJumpRecordController` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/controller/LadderJumpRecordController.java:18`
- `TypeWarriorRecordController` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/controller/TypeWarriorRecordController.java:18`

### `com.cupk.games.dto` (4)

- `LadderJumpQuestionBankResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/dto/LadderJumpQuestionBankResponse.java:6`
- `LadderJumpQuestionResponse` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/dto/LadderJumpQuestionResponse.java:8`
- `LadderJumpRecordSaveRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/dto/LadderJumpRecordSaveRequest.java:6`
- `TypeWarriorRecordSaveRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/dto/TypeWarriorRecordSaveRequest.java:6`

### `com.cupk.games.repository` (1)

- `GameRecordRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/repository/GameRecordRepository.java:12`

### `com.cupk.games.service` (2)

- `GameRecordService` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/service/GameRecordService.java:14`
- `LadderJumpQuestionService` (class) - `StudyPlatform-back/src/main/java/com/cupk/games/service/LadderJumpQuestionService.java:20`

### `com.cupk.oj.config` (2)

- `OjAsyncConfig` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/config/OjAsyncConfig.java:8`
- `OjProperties` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/config/OjProperties.java:11`

### `com.cupk.oj.controller` (2)

- `OjProblemController` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/controller/OjProblemController.java:28`
- `OjSubmissionController` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/controller/OjSubmissionController.java:22`

### `com.cupk.oj.dto` (7)

- `CreateProblemRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/CreateProblemRequest.java:11`
- `CreateSubmissionRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/CreateSubmissionRequest.java:7`
- `CreateTestCaseRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/CreateTestCaseRequest.java:7`
- `JudgeCaseResult` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/JudgeCaseResult.java:5`
- `JudgeResult` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/JudgeResult.java:6`
- `ProblemSummary` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/ProblemSummary.java:7`
- `UpdateProblemRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/dto/UpdateProblemRequest.java:11`

### `com.cupk.oj.model` (7)

- `OjProblem` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/OjProblem.java:5`
- `OjSubmission` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/OjSubmission.java:5`
- `OjSubmissionCase` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/OjSubmissionCase.java:5`
- `OjTestCase` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/OjTestCase.java:5`
- `ProblemDifficulty` (enum) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/ProblemDifficulty.java:3`
- `ProblemStatus` (enum) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/ProblemStatus.java:3`
- `SubmissionStatus` (enum) - `StudyPlatform-back/src/main/java/com/cupk/oj/model/SubmissionStatus.java:3`

### `com.cupk.oj.repository` (4)

- `OjProblemRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/repository/OjProblemRepository.java:23`
- `OjSubmissionCaseRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/repository/OjSubmissionCaseRepository.java:14`
- `OjSubmissionRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/repository/OjSubmissionRepository.java:21`
- `OjTestCaseRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/repository/OjTestCaseRepository.java:15`

### `com.cupk.oj.service` (6)

- `JudgeSandboxClient` (interface) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/JudgeSandboxClient.java:14`
- `OjJudgeService` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/OjJudgeService.java:18`
- `OjProblemService` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/OjProblemService.java:16`
- `OjSubmissionService` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/OjSubmissionService.java:19`
- `OjTestCaseService` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/OjTestCaseService.java:14`
- `RemoteJudgeSandboxClient` (class) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/RemoteJudgeSandboxClient.java:22`

### `com.cupk.production.controller` (1)

- `ProductionRecordController` (class) - `StudyPlatform-back/src/main/java/com/cupk/production/controller/ProductionRecordController.java:31`

### `com.cupk.production.dto` (5)

- `ProductionPage` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/dto/ProductionPage.java:14`
- `SavePumpRecordRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/dto/SavePumpRecordRequest.java:11`
- `SaveReservoirRecordRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/dto/SaveReservoirRecordRequest.java:10`
- `SaveStimulationRecordRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/dto/SaveStimulationRecordRequest.java:10`
- `SaveWaterfloodRecordRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/dto/SaveWaterfloodRecordRequest.java:11`

### `com.cupk.production.model` (4)

- `ProductionPumpRecord` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/model/ProductionPumpRecord.java:8`
- `ProductionReservoirRecord` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/model/ProductionReservoirRecord.java:8`
- `ProductionStimulationRecord` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/model/ProductionStimulationRecord.java:8`
- `ProductionWaterfloodRecord` (record) - `StudyPlatform-back/src/main/java/com/cupk/production/model/ProductionWaterfloodRecord.java:8`

### `com.cupk.production.repository` (1)

- `ProductionRecordRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/production/repository/ProductionRecordRepository.java:27`

### `com.cupk.production.service` (1)

- `ProductionRecordService` (class) - `StudyPlatform-back/src/main/java/com/cupk/production/service/ProductionRecordService.java:26`

### `com.cupk.welllog.controller` (2)

- `WellLogRecordController` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/controller/WellLogRecordController.java:25`
- `WellLogTemplateController` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/controller/WellLogTemplateController.java:17`

### `com.cupk.welllog.dto` (2)

- `SaveWellLogRecordRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/welllog/dto/SaveWellLogRecordRequest.java:11`
- `WellLogRecordPage` (record) - `StudyPlatform-back/src/main/java/com/cupk/welllog/dto/WellLogRecordPage.java:9`

### `com.cupk.welllog.model` (2)

- `WellLogRecord` (record) - `StudyPlatform-back/src/main/java/com/cupk/welllog/model/WellLogRecord.java:8`
- `WellLogTemplate` (record) - `StudyPlatform-back/src/main/java/com/cupk/welllog/model/WellLogTemplate.java:8`

### `com.cupk.welllog.repository` (2)

- `WellLogRecordRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/repository/WellLogRecordRepository.java:21`
- `WellLogTemplateRepository` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/repository/WellLogTemplateRepository.java:15`

### `com.cupk.welllog.service` (2)

- `WellLogRecordService` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/service/WellLogRecordService.java:19`
- `WellLogTemplateService` (class) - `StudyPlatform-back/src/main/java/com/cupk/welllog/service/WellLogTemplateService.java:15`

## Java Nested Types

### `com.cupk.academy.repository.AcademyAssignmentRepository` (4)

- `AcademyAssignmentRepository.AssignmentSummaryRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyAssignmentRepository.java:241`
- `AcademyAssignmentRepository.AssignmentDetailRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyAssignmentRepository.java:259`
- `AcademyAssignmentRepository.AssignmentQuestionRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyAssignmentRepository.java:274`
- `AcademyAssignmentRepository.SubmissionStatusRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyAssignmentRepository.java:290`

### `com.cupk.academy.repository.AcademyExamRepository` (4)

- `AcademyExamRepository.ExamSummaryRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyExamRepository.java:267`
- `AcademyExamRepository.ExamDetailRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyExamRepository.java:288`
- `AcademyExamRepository.ExamQuestionRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyExamRepository.java:304`
- `AcademyExamRepository.SubmissionStatusRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/AcademyExamRepository.java:320`

### `com.cupk.academy.repository.ProfileRepository` (4)

- `ProfileRepository.DistributionRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/ProfileRepository.java:405`
- `ProfileRepository.TrackRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/ProfileRepository.java:413`
- `ProfileRepository.CodingDifficultyRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/ProfileRepository.java:421`
- `ProfileRepository.RecentEventRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/ProfileRepository.java:428`

### `com.cupk.academy.repository.QuestionBankRepository` (7)

- `QuestionBankRepository.CourseQuestionBankQuestionSeed` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:944`
- `QuestionBankRepository.TypeWarriorVocabularyRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:956`
- `QuestionBankRepository.SingleChoiceQuestionBankRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:965`
- `QuestionBankRepository.CourseQuestionAnswerReference` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:973`
- `QuestionBankRepository.QuestionBankMistakeState` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:981`
- `QuestionBankRepository.QuestionBankMistakeTotals` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:988`
- `QuestionBankRepository.CourseQuestionBankCategoryBuilder` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/repository/QuestionBankRepository.java:995`

### `com.cupk.academy.service.CetVocabularySeeder` (2)

- `CetVocabularySeeder.CetVocabularyWord` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/CetVocabularySeeder.java:121`
- `CetVocabularySeeder.CetVocabularyTranslation` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/CetVocabularySeeder.java:127`

### `com.cupk.academy.service.IdeologyQuestionBankSeeder` (1)

- `IdeologyQuestionBankSeeder.IdeologyQuestion` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/IdeologyQuestionBankSeeder.java:255`

### `com.cupk.academy.service.MaoismQuestionBankSeeder` (2)

- `MaoismQuestionBankSeeder.MaoismQuestionBank` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MaoismQuestionBankSeeder.java:228`
- `MaoismQuestionBankSeeder.MaoismQuestion` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MaoismQuestionBankSeeder.java:235`

### `com.cupk.academy.service.MarkdownQuestionBankSeeder` (2)

- `MarkdownQuestionBankSeeder.MarkdownParser` (interface) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarkdownQuestionBankSeeder.java:394`
- `MarkdownQuestionBankSeeder.OptionSeed` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarkdownQuestionBankSeeder.java:398`

### `com.cupk.academy.service.MarxismQuestionBankSeeder` (2)

- `MarxismQuestionBankSeeder.MarxismQuestion` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarxismQuestionBankSeeder.java:165`
- `MarxismQuestionBankSeeder.MarxismOption` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/MarxismQuestionBankSeeder.java:177`

### `com.cupk.academy.service.ModernHistoryQuestionBankSeeder` (2)

- `ModernHistoryQuestionBankSeeder.ModernHistoryQuestion` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/ModernHistoryQuestionBankSeeder.java:123`
- `ModernHistoryQuestionBankSeeder.ModernHistoryOption` (record) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/ModernHistoryQuestionBankSeeder.java:134`

### `com.cupk.academy.service.QuestionBankSourceResolver` (1)

- `QuestionBankSourceResolver.SourceFile` (class) - `StudyPlatform-back/src/main/java/com/cupk/academy/service/QuestionBankSourceResolver.java:59`

### `com.cupk.admin.repository.AdminRepository` (1)

- `AdminRepository.AdminAuthRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/admin/repository/AdminRepository.java:640`

### `com.cupk.auth.repository.AuthUserRepository` (1)

- `AuthUserRepository.AuthUserRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/repository/AuthUserRepository.java:233`

### `com.cupk.auth.repository.PasswordResetCodeRepository` (1)

- `PasswordResetCodeRepository.PasswordResetCodeRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/auth/repository/PasswordResetCodeRepository.java:109`

### `com.cupk.games.repository.GameRecordRepository` (3)

- `GameRecordRepository.LadderJumpAggregateRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/repository/GameRecordRepository.java:172`
- `GameRecordRepository.TypeWarriorAggregateRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/repository/GameRecordRepository.java:186`
- `GameRecordRepository.CombinedDurationAggregateRow` (record) - `StudyPlatform-back/src/main/java/com/cupk/games/repository/GameRecordRepository.java:206`

### `com.cupk.oj.service.RemoteJudgeSandboxClient` (2)

- `RemoteJudgeSandboxClient.JudgeSandboxRequest` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/RemoteJudgeSandboxClient.java:113`
- `RemoteJudgeSandboxClient.JudgeSandboxCase` (record) - `StudyPlatform-back/src/main/java/com/cupk/oj/service/RemoteJudgeSandboxClient.java:135`

### `com.cupk.production.repository.ProductionRecordRepository` (1)

- `ProductionRecordRepository.StatementBinder` (interface) - `StudyPlatform-back/src/main/java/com/cupk/production/repository/ProductionRecordRepository.java:280`

## Database Tables

### `academy_assignment_questions` (15 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `assignment_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `question_order` | `int` | NO |  | `NULL` |  |  |
| `question_type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `question_label` | `varchar(64)` | YES |  | `NULL` |  |  |
| `question_title` | `text` | NO |  | `NULL` |  |  |
| `question_options` | `json` | YES |  | `NULL` |  |  |
| `placeholder_text` | `text` | YES |  | `NULL` |  |  |
| `score` | `int` | NO |  | `0` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `correct_answer` | `json` | YES |  | `NULL` |  |  |
| `answer_explanation` | `text` | YES |  | `NULL` |  |  |
| `auto_gradable` | `tinyint(1)` | NO |  | `0` |  |  |
| `oj_problem_id` | `bigint` | YES |  | `NULL` |  |  |
| `requires_teacher_review` | `tinyint(1)` | NO |  | `0` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_assignment_question_order`: UNIQUE BTREE (`assignment_id`, `question_order`)

Foreign keys:
- `fk_assignment_questions_assignment`: (`assignment_id`) -> `academy_assignments`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `academy_assignment_submissions` (10 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `assignment_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `submission_status` | `varchar(32)` | NO |  | `draft` |  |  |
| `answer_payload` | `json` | YES |  | `NULL` |  |  |
| `score` | `int` | YES |  | `NULL` |  |  |
| `teacher_feedback` | `text` | YES |  | `NULL` |  |  |
| `submitted_at` | `datetime` | YES |  | `NULL` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_assignment_submissions_assignment`: INDEX BTREE (`assignment_id`)
- `idx_assignment_submissions_user`: INDEX BTREE (`user_id`, `submission_status`)

Foreign keys:
- `fk_assignment_submissions_assignment`: (`assignment_id`) -> `academy_assignments`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `academy_assignments` (15 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `assignment_code` | `varchar(120)` | NO | UNI | `NULL` |  |  |
| `course_resource_type` | `varchar(64)` | NO | MUL | `NULL` |  |  |
| `course_id` | `varchar(120)` | NO |  | `NULL` |  |  |
| `course_title` | `varchar(255)` | YES |  | `NULL` |  |  |
| `assignment_title` | `varchar(255)` | NO |  | `NULL` |  |  |
| `teacher_name` | `varchar(120)` | YES |  | `NULL` |  |  |
| `assignment_status` | `varchar(32)` | NO | MUL | `���ڽ���` |  |  |
| `deadline_at` | `datetime` | YES |  | `NULL` |  |  |
| `attempts_limit` | `int` | NO |  | `1` |  |  |
| `duration_minutes` | `int` | YES |  | `NULL` |  |  |
| `total_score` | `int` | NO |  | `100` |  |  |
| `assignment_description` | `text` | YES |  | `NULL` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_academy_assignments_course`: INDEX BTREE (`course_resource_type`, `course_id`)
- `idx_academy_assignments_status`: INDEX BTREE (`assignment_status`)
- `uk_academy_assignments_code`: UNIQUE BTREE (`assignment_code`)

### `academy_course_enrollments` (5 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `resource_type` | `varchar(64)` | NO | MUL | `NULL` |  |  |
| `course_id` | `varchar(128)` | NO |  | `NULL` |  |  |
| `user_id` | `bigint` | NO |  | `1` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_academy_course_enrollment`: UNIQUE BTREE (`resource_type`, `course_id`, `user_id`)

### `academy_course_reviews` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `resource_type` | `varchar(64)` | NO | MUL | `NULL` |  |  |
| `course_id` | `varchar(128)` | NO |  | `NULL` |  |  |
| `user_name` | `varchar(64)` | NO |  | `NULL` |  |  |
| `rating` | `tinyint` | NO |  | `NULL` |  |  |
| `content` | `text` | NO |  | `NULL` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_academy_course_reviews_course`: INDEX BTREE (`resource_type`, `course_id`, `created_at`)

### `academy_exam_questions` (15 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `exam_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `question_order` | `int` | NO |  | `NULL` |  |  |
| `question_type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `question_label` | `varchar(64)` | YES |  | `NULL` |  |  |
| `question_title` | `text` | NO |  | `NULL` |  |  |
| `question_options` | `json` | YES |  | `NULL` |  |  |
| `placeholder_text` | `text` | YES |  | `NULL` |  |  |
| `score` | `int` | NO |  | `0` |  |  |
| `correct_answer` | `json` | YES |  | `NULL` |  |  |
| `answer_explanation` | `text` | YES |  | `NULL` |  |  |
| `auto_gradable` | `tinyint(1)` | NO |  | `0` |  |  |
| `oj_problem_id` | `bigint` | YES |  | `NULL` |  |  |
| `requires_teacher_review` | `tinyint(1)` | NO |  | `0` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_exam_question_order`: UNIQUE BTREE (`exam_id`, `question_order`)

Foreign keys:
- `fk_exam_questions_exam`: (`exam_id`) -> `academy_exams`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `academy_exam_submissions` (11 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `exam_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `submission_status` | `varchar(32)` | NO |  | `in_progress` |  |  |
| `answer_payload` | `json` | YES |  | `NULL` |  |  |
| `score` | `int` | YES |  | `NULL` |  |  |
| `teacher_feedback` | `text` | YES |  | `NULL` |  |  |
| `started_at` | `datetime` | YES |  | `NULL` |  |  |
| `submitted_at` | `datetime` | YES |  | `NULL` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_exam_submissions_exam`: INDEX BTREE (`exam_id`)
- `idx_exam_submissions_user`: INDEX BTREE (`user_id`, `submission_status`)

Foreign keys:
- `fk_exam_submissions_exam`: (`exam_id`) -> `academy_exams`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `academy_exams` (16 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `exam_code` | `varchar(120)` | NO | UNI | `NULL` |  |  |
| `course_resource_type` | `varchar(64)` | NO | MUL | `NULL` |  |  |
| `course_id` | `varchar(120)` | NO |  | `NULL` |  |  |
| `course_title` | `varchar(255)` | YES |  | `NULL` |  |  |
| `exam_title` | `varchar(255)` | NO |  | `NULL` |  |  |
| `teacher_name` | `varchar(120)` | YES |  | `NULL` |  |  |
| `exam_status` | `varchar(32)` | NO | MUL | `���ڽ���` |  |  |
| `starts_at` | `datetime` | YES | MUL | `NULL` |  |  |
| `deadline_at` | `datetime` | YES |  | `NULL` |  |  |
| `attempts_limit` | `int` | NO |  | `1` |  |  |
| `duration_minutes` | `int` | YES |  | `NULL` |  |  |
| `total_score` | `int` | NO |  | `100` |  |  |
| `exam_description` | `text` | YES |  | `NULL` |  |  |
| `created_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_academy_exams_course`: INDEX BTREE (`course_resource_type`, `course_id`)
- `idx_academy_exams_status`: INDEX BTREE (`exam_status`)
- `idx_academy_exams_time`: INDEX BTREE (`starts_at`, `deadline_at`)
- `uk_academy_exams_code`: UNIQUE BTREE (`exam_code`)

### `auth_users` (14 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `username` | `varchar(64)` | NO |  | `NULL` |  |  |
| `email` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `password_hash` | `varchar(255)` | NO |  | `NULL` |  |  |
| `role_type` | `varchar(24)` | YES |  | `NULL` |  |  |
| `learning_goal` | `varchar(255)` | YES |  | `NULL` |  |  |
| `interests_json` | `json` | YES |  | `NULL` |  |  |
| `school` | `varchar(128)` | YES |  | `NULL` |  |  |
| `teacher_name` | `varchar(64)` | YES |  | `NULL` |  |  |
| `pet_key` | `varchar(32)` | YES |  | `NULL` |  |  |
| `agreement_accepted` | `tinyint(1)` | NO |  | `0` |  |  |
| `onboarding_completed` | `tinyint(1)` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_auth_users_email`: UNIQUE BTREE (`email`)

### `course_question_bank_categories` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `category_code` | `varchar(64)` | NO | UNI | `NULL` |  |  |
| `category_name` | `varchar(64)` | NO |  | `NULL` |  |  |
| `description` | `varchar(512)` | YES |  | `NULL` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_course_question_bank_categories_code`: UNIQUE BTREE (`category_code`)

### `course_question_bank_favorites` (5 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `question_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_course_question_bank_favorites_question`: INDEX BTREE (`question_id`)
- `idx_course_question_bank_favorites_user_created`: INDEX BTREE (`user_id`, `created_at`)
- `uk_course_question_bank_favorites_user_question`: UNIQUE BTREE (`user_id`, `question_id`)

Foreign keys:
- `fk_course_question_bank_favorites_question`: (`question_id`) -> `course_question_bank_questions`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `course_question_bank_mistakes` (13 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `question_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `selected_answer` | `text` | YES |  | `NULL` |  |  |
| `correct_answer` | `text` | YES |  | `NULL` |  |  |
| `wrong_count` | `int` | NO |  | `0` |  |  |
| `correct_streak` | `int` | NO |  | `0` |  |  |
| `mastered` | `tinyint(1)` | NO |  | `0` |  |  |
| `first_wrong_at` | `datetime` | YES |  | `NULL` |  |  |
| `last_wrong_at` | `datetime` | YES |  | `NULL` |  |  |
| `last_reviewed_at` | `datetime` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_course_question_bank_mistakes_question`: INDEX BTREE (`question_id`)
- `idx_course_question_bank_mistakes_user_mastered`: INDEX BTREE (`user_id`, `mastered`, `updated_at`)
- `uk_course_question_bank_mistakes_user_question`: UNIQUE BTREE (`user_id`, `question_id`)

Foreign keys:
- `fk_course_question_bank_mistakes_question`: (`question_id`) -> `course_question_bank_questions`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `course_question_bank_questions` (12 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `set_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `question_type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `stem` | `text` | NO |  | `NULL` |  |  |
| `options_json` | `json` | YES |  | `NULL` |  |  |
| `answer` | `text` | YES |  | `NULL` |  |  |
| `explanation` | `text` | YES |  | `NULL` |  |  |
| `difficulty_label` | `varchar(64)` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(512)` | YES |  | `NULL` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_course_question_bank_questions_set`: INDEX BTREE (`set_id`)

Foreign keys:
- `fk_course_question_bank_questions_set`: (`set_id`) -> `course_question_bank_sets`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `course_question_bank_sets` (17 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `category_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `set_code` | `varchar(64)` | NO | UNI | `NULL` |  |  |
| `title` | `varchar(128)` | NO |  | `NULL` |  |  |
| `subtitle` | `varchar(128)` | YES |  | `NULL` |  |  |
| `description` | `varchar(512)` | YES |  | `NULL` |  |  |
| `cover_url` | `varchar(512)` | YES |  | `NULL` |  |  |
| `cover_file_path` | `varchar(255)` | YES |  | `NULL` |  |  |
| `difficulty_label` | `varchar(64)` | YES |  | `NULL` |  |  |
| `status_label` | `varchar(64)` | YES |  | `NULL` |  |  |
| `source_name` | `varchar(128)` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(512)` | YES |  | `NULL` |  |  |
| `source_refs` | `json` | YES |  | `NULL` |  |  |
| `route_path` | `varchar(255)` | YES |  | `NULL` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_course_question_bank_sets_category`: INDEX BTREE (`category_id`)
- `uk_course_question_bank_sets_code`: UNIQUE BTREE (`set_code`)

Foreign keys:
- `fk_course_question_bank_sets_category`: (`category_id`) -> `course_question_bank_categories`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `excellent_textbooks` (14 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `external_textbook_id` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `textbook_name` | `varchar(255)` | NO |  | `NULL` |  |  |
| `chief_editor` | `varchar(255)` | YES |  | `NULL` |  |  |
| `category` | `varchar(64)` | YES | MUL | `NULL` |  |  |
| `publisher` | `varchar(255)` | YES |  | `NULL` |  |  |
| `publish_date` | `varchar(64)` | YES |  | `NULL` |  |  |
| `isbn` | `varchar(64)` | YES |  | `NULL` |  |  |
| `cover_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `cover_file_path` | `varchar(512)` | YES |  | `NULL` |  |  |
| `description` | `text` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_excellent_textbooks_category`: INDEX BTREE (`category`)
- `uk_excellent_textbooks_external_id`: UNIQUE BTREE (`external_textbook_id`)

### `flyway_schema_history` (10 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `installed_rank` | `int` | NO | PRI | `NULL` |  |  |
| `version` | `varchar(50)` | YES |  | `NULL` |  |  |
| `description` | `varchar(200)` | NO |  | `NULL` |  |  |
| `type` | `varchar(20)` | NO |  | `NULL` |  |  |
| `script` | `varchar(1000)` | NO |  | `NULL` |  |  |
| `checksum` | `int` | YES |  | `NULL` |  |  |
| `installed_by` | `varchar(100)` | NO |  | `NULL` |  |  |
| `installed_on` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `execution_time` | `int` | NO |  | `NULL` |  |  |
| `success` | `tinyint(1)` | NO | MUL | `NULL` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`installed_rank`)
- `flyway_schema_history_s_idx`: INDEX BTREE (`success`)

### `flyway_schema_history_backup_20260702_161139` (10 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `installed_rank` | `int` | NO |  | `NULL` |  |  |
| `version` | `varchar(50)` | YES |  | `NULL` |  |  |
| `description` | `varchar(200)` | NO |  | `NULL` |  |  |
| `type` | `varchar(20)` | NO |  | `NULL` |  |  |
| `script` | `varchar(1000)` | NO |  | `NULL` |  |  |
| `checksum` | `int` | YES |  | `NULL` |  |  |
| `installed_by` | `varchar(100)` | NO |  | `NULL` |  |  |
| `installed_on` | `timestamp` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `execution_time` | `int` | NO |  | `NULL` |  |  |
| `success` | `tinyint(1)` | NO |  | `NULL` |  |  |

### `game_ladder_jump_records` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `question_bank_code` | `varchar(64)` | YES | MUL | `NULL` |  |  |
| `total_coins` | `int` | NO |  | `0` |  |  |
| `correct_count` | `int` | NO |  | `0` |  |  |
| `wrong_count` | `int` | NO |  | `0` |  |  |
| `duration_seconds` | `double` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_game_ladder_jump_records_bank`: INDEX BTREE (`question_bank_code`)
- `idx_game_ladder_jump_records_user_created`: INDEX BTREE (`user_id`, `created_at`)

### `game_type_warrior_records` (12 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `reached_wave` | `int` | NO |  | `0` |  |  |
| `completed_wave_count` | `int` | NO |  | `0` |  |  |
| `score` | `bigint` | NO |  | `0` |  |  |
| `max_combo` | `int` | NO |  | `0` |  |  |
| `solved_word_count` | `int` | NO |  | `0` |  |  |
| `total_kill_count` | `int` | NO |  | `0` |  |  |
| `typed_letter_count` | `int` | NO |  | `0` |  |  |
| `duration_seconds` | `double` | NO |  | `0` |  |  |
| `effective_typing_seconds` | `double` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_game_type_warrior_records_user_created`: INDEX BTREE (`user_id`, `created_at`)

### `general_courses` (16 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `external_course_id` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `course_name` | `varchar(255)` | NO |  | `NULL` |  |  |
| `teacher_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `category` | `varchar(64)` | YES | MUL | `NULL` |  |  |
| `school_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `cover_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `cover_file_path` | `varchar(512)` | YES |  | `NULL` |  |  |
| `start_time` | `varchar(64)` | YES |  | `NULL` |  |  |
| `participant_count` | `int` | YES |  | `NULL` |  |  |
| `course_comment` | `text` | YES |  | `NULL` |  |  |
| `course_description` | `text` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |
| `certified` | `tinyint(1)` | NO |  | `0` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_general_courses_category`: INDEX BTREE (`category`)
- `uk_general_courses_external_id`: UNIQUE BTREE (`external_course_id`)

### `learning_content_blocks` (9 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `block_code` | `varchar(64)` | NO | UNI | `NULL` |  |  |
| `block_name` | `varchar(64)` | NO |  | `NULL` |  |  |
| `description` | `varchar(255)` | YES |  | `NULL` |  |  |
| `storage_folder` | `varchar(255)` | YES |  | `NULL` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `enabled` | `tinyint(1)` | NO |  | `1` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_learning_content_blocks_code`: UNIQUE BTREE (`block_code`)

### `micro_major_courses` (16 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `external_course_id` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `course_name` | `varchar(255)` | NO |  | `NULL` |  |  |
| `teacher_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `category` | `varchar(64)` | YES | MUL | `NULL` |  |  |
| `school_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `cover_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `cover_file_path` | `varchar(512)` | YES |  | `NULL` |  |  |
| `start_time` | `varchar(64)` | YES |  | `NULL` |  |  |
| `participant_count` | `int` | YES |  | `NULL` |  |  |
| `course_comment` | `text` | YES |  | `NULL` |  |  |
| `course_description` | `text` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |
| `certified` | `tinyint(1)` | NO |  | `0` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_micro_major_courses_category`: INDEX BTREE (`category`)
- `uk_micro_major_courses_external_id`: UNIQUE BTREE (`external_course_id`)

### `oj_problems` (15 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `title` | `varchar(128)` | NO |  | `NULL` |  |  |
| `slug` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `description` | `text` | NO |  | `NULL` |  |  |
| `input_description` | `text` | YES |  | `NULL` |  |  |
| `output_description` | `text` | YES |  | `NULL` |  |  |
| `samples` | `json` | YES |  | `NULL` |  |  |
| `difficulty` | `varchar(16)` | NO |  | `EASY` |  |  |
| `time_limit_ms` | `int` | NO |  | `1000` |  |  |
| `memory_limit_kb` | `int` | NO |  | `262144` |  |  |
| `tags` | `json` | YES |  | `NULL` |  |  |
| `status` | `varchar(16)` | NO | MUL | `DRAFT` |  |  |
| `created_by` | `bigint` | YES | MUL | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `fk_oj_problems_created_by`: INDEX BTREE (`created_by`)
- `idx_oj_problems_status`: INDEX BTREE (`status`)
- `uk_oj_problems_slug`: UNIQUE BTREE (`slug`)

Foreign keys:
- `fk_oj_problems_created_by`: (`created_by`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `oj_submission_cases` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `submission_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `test_case_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `status` | `varchar(32)` | NO |  | `NULL` |  |  |
| `time_used_ms` | `int` | YES |  | `NULL` |  |  |
| `memory_used_kb` | `int` | YES |  | `NULL` |  |  |
| `message` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `fk_oj_submission_cases_test_case_id`: INDEX BTREE (`test_case_id`)
- `idx_oj_submission_cases_submission_id`: INDEX BTREE (`submission_id`)

Foreign keys:
- `fk_oj_submission_cases_submission_id`: (`submission_id`) -> `oj_submissions`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE
- `fk_oj_submission_cases_test_case_id`: (`test_case_id`) -> `oj_test_cases`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `oj_submissions` (13 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `problem_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `language` | `varchar(32)` | NO |  | `NULL` |  |  |
| `source_code` | `mediumtext` | NO |  | `NULL` |  |  |
| `status` | `varchar(32)` | NO | MUL | `PENDING` |  |  |
| `score` | `int` | NO |  | `0` |  |  |
| `time_used_ms` | `int` | YES |  | `NULL` |  |  |
| `memory_used_kb` | `int` | YES |  | `NULL` |  |  |
| `message` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `judged_at` | `datetime` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `fk_oj_submissions_user_id`: INDEX BTREE (`user_id`)
- `idx_oj_submissions_problem_user`: INDEX BTREE (`problem_id`, `user_id`)
- `idx_oj_submissions_status`: INDEX BTREE (`status`)

Foreign keys:
- `fk_oj_submissions_problem_id`: (`problem_id`) -> `oj_problems`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION
- `fk_oj_submissions_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `oj_test_cases` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `problem_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `input_data` | `mediumtext` | NO |  | `NULL` |  |  |
| `expected_output` | `mediumtext` | NO |  | `NULL` |  |  |
| `sample` | `tinyint(1)` | NO |  | `0` |  |  |
| `weight` | `int` | NO |  | `1` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_oj_test_cases_problem_id`: INDEX BTREE (`problem_id`)

Foreign keys:
- `fk_oj_test_cases_problem_id`: (`problem_id`) -> `oj_problems`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `online_open_courses` (15 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `external_course_id` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `course_name` | `varchar(255)` | NO |  | `NULL` |  |  |
| `teacher_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `category` | `varchar(64)` | YES | MUL | `NULL` |  |  |
| `school_name` | `varchar(255)` | YES |  | `NULL` |  |  |
| `cover_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `cover_file_path` | `varchar(512)` | YES |  | `NULL` |  |  |
| `start_time` | `varchar(64)` | YES |  | `NULL` |  |  |
| `participant_count` | `int` | YES |  | `NULL` |  |  |
| `course_comment` | `text` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(1024)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |
| `certified` | `tinyint(1)` | NO |  | `0` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_online_open_courses_category`: INDEX BTREE (`category`)
- `uk_online_open_courses_external_id`: UNIQUE BTREE (`external_course_id`)

### `password_reset_codes` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `email` | `varchar(128)` | NO | MUL | `NULL` |  |  |
| `code_hash` | `varchar(255)` | NO |  | `NULL` |  |  |
| `expires_at` | `datetime` | NO | MUL | `NULL` |  |  |
| `used` | `tinyint(1)` | NO |  | `0` |  |  |
| `attempt_count` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_password_reset_codes_email_created`: INDEX BTREE (`email`, `created_at`)
- `idx_password_reset_codes_expires`: INDEX BTREE (`expires_at`)

### `production_pump_record` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `stroke` | `double` | NO |  | `NULL` |  |  |
| `stroke_times` | `double` | NO |  | `NULL` |  |  |
| `pump_diameter` | `double` | NO |  | `NULL` |  |  |
| `work_condition` | `varchar(32)` | NO |  | `NULL` |  |  |
| `indicator_chart_data` | `json` | NO |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_production_pump_user_time`: INDEX BTREE (`user_id`, `create_time`)

Foreign keys:
- `fk_production_pump_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `production_reservoir_record` (9 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `formation_pressure` | `double` | NO |  | `NULL` |  |  |
| `permeability` | `double` | NO |  | `NULL` |  |  |
| `water_saturation` | `double` | NO |  | `NULL` |  |  |
| `viscosity` | `double` | NO |  | `NULL` |  |  |
| `daily_oil` | `double` | NO |  | `NULL` |  |  |
| `daily_water` | `double` | NO |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_production_reservoir_user_time`: INDEX BTREE (`user_id`, `create_time`)

Foreign keys:
- `fk_production_reservoir_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `production_stimulation_record` (9 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `sand_volume` | `double` | YES |  | `NULL` |  |  |
| `displacement` | `double` | NO |  | `NULL` |  |  |
| `acid_volume` | `double` | YES |  | `NULL` |  |  |
| `fracture_length` | `double` | NO |  | `NULL` |  |  |
| `stimulation_ratio` | `double` | NO |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_production_stimulation_user_time`: INDEX BTREE (`user_id`, `create_time`)

Foreign keys:
- `fk_production_stimulation_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `production_waterflood_record` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `injection_rate` | `double` | NO |  | `NULL` |  |  |
| `effect_day` | `int` | NO |  | `NULL` |  |  |
| `water_breakthrough_day` | `int` | NO |  | `NULL` |  |  |
| `peak_oil` | `double` | NO |  | `NULL` |  |  |
| `production_curve` | `json` | NO |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_production_waterflood_user_time`: INDEX BTREE (`user_id`, `create_time`)

Foreign keys:
- `fk_production_waterflood_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `profile_learning_events` (11 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `event_type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `set_code` | `varchar(64)` | YES |  | `NULL` |  |  |
| `question_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `question_type` | `varchar(32)` | YES |  | `NULL` |  |  |
| `selected_answer` | `text` | YES |  | `NULL` |  |  |
| `correct_answer` | `text` | YES |  | `NULL` |  |  |
| `is_correct` | `tinyint(1)` | YES |  | `NULL` |  |  |
| `vocabulary_status` | `varchar(32)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_profile_learning_events_question`: INDEX BTREE (`question_id`)
- `idx_profile_learning_events_user_created`: INDEX BTREE (`user_id`, `created_at`)
- `idx_profile_learning_events_user_set`: INDEX BTREE (`user_id`, `set_code`)

Foreign keys:
- `fk_profile_learning_events_question`: (`question_id`) -> `course_question_bank_questions`(`id`), ON UPDATE NO ACTION, ON DELETE SET NULL

### `profile_learning_time_records` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | NO | MUL | `1` |  |  |
| `module_type` | `varchar(32)` | NO |  | `NULL` |  |  |
| `target_code` | `varchar(128)` | YES |  | `NULL` |  |  |
| `target_title` | `varchar(128)` | YES |  | `NULL` |  |  |
| `duration_seconds` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_profile_learning_time_user_created`: INDEX BTREE (`user_id`, `created_at`)
- `idx_profile_learning_time_user_module`: INDEX BTREE (`user_id`, `module_type`)

### `profile_user_profiles` (12 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `user_id` | `bigint` | NO | PRI | `NULL` |  |  |
| `display_name` | `varchar(64)` | NO |  | `Kinkin` |  |  |
| `handle` | `varchar(64)` | NO |  | `@study-platform` |  |  |
| `role_label` | `varchar(128)` | NO |  | `StudyPlatform ѧϰ��` |  |  |
| `bio` | `varchar(512)` | NO |  | `����⡢�γ̡�ʵ���뱳����֮�����ش��󣬰���ɢ��ϰ�������ȶ���ѧϰ���ߡ�` |  |  |
| `location` | `varchar(64)` | NO |  | `China` |  |  |
| `school` | `varchar(128)` | NO |  | `StudyPlatform` |  |  |
| `avatar_path` | `varchar(255)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |
| `admin_coin_adjustment` | `bigint` | NO |  | `0` |  |  |
| `admin_data_note` | `varchar(512)` | YES |  | `NULL` |  |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`user_id`)

### `question_bank_problem_subjects` (3 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `problem_id` | `bigint` | NO | PRI | `NULL` |  |  |
| `subject_id` | `bigint` | NO | PRI | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`problem_id`, `subject_id`)
- `idx_question_bank_problem_subjects_subject`: INDEX BTREE (`subject_id`)

Foreign keys:
- `fk_question_bank_problem_subjects_problem`: (`problem_id`) -> `question_bank_problems`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE
- `fk_question_bank_problem_subjects_subject`: (`subject_id`) -> `question_bank_subjects`(`id`), ON UPDATE NO ACTION, ON DELETE CASCADE

### `question_bank_problems` (17 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `source` | `varchar(32)` | NO | MUL | `NULL` |  |  |
| `external_problem_id` | `varchar(64)` | NO |  | `NULL` |  |  |
| `title` | `varchar(255)` | NO | MUL | `NULL` |  |  |
| `difficulty` | `int` | YES | MUL | `NULL` |  |  |
| `difficulty_label` | `varchar(32)` | YES |  | `NULL` |  |  |
| `tag_ids` | `json` | YES |  | `NULL` |  |  |
| `tag_names` | `json` | YES |  | `NULL` |  |  |
| `description` | `mediumtext` | YES |  | `NULL` |  |  |
| `input_description` | `mediumtext` | YES |  | `NULL` |  |  |
| `output_description` | `mediumtext` | YES |  | `NULL` |  |  |
| `hint` | `mediumtext` | YES |  | `NULL` |  |  |
| `total_submit` | `int` | YES |  | `NULL` |  |  |
| `total_accepted` | `int` | YES |  | `NULL` |  |  |
| `source_url` | `varchar(512)` | NO |  | `NULL` |  |  |
| `imported_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_question_bank_problems_difficulty`: INDEX BTREE (`difficulty`)
- `idx_question_bank_problems_title`: INDEX BTREE (`title`)
- `uk_question_bank_problems_source_external`: UNIQUE BTREE (`source`, `external_problem_id`)

### `question_bank_subjects` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `subject_code` | `varchar(64)` | NO | UNI | `NULL` |  |  |
| `subject_name` | `varchar(64)` | NO |  | `NULL` |  |  |
| `description` | `varchar(512)` | YES |  | `NULL` |  |  |
| `sort_order` | `int` | NO |  | `0` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_question_bank_subjects_code`: UNIQUE BTREE (`subject_code`)

### `question_bank_tags` (8 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `source` | `varchar(32)` | NO | MUL | `NULL` |  |  |
| `external_tag_id` | `int` | NO |  | `NULL` |  |  |
| `tag_name` | `varchar(128)` | NO | MUL | `NULL` |  |  |
| `tag_type` | `int` | YES |  | `NULL` |  |  |
| `parent_external_tag_id` | `int` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_question_bank_tags_name`: INDEX BTREE (`tag_name`)
- `uk_question_bank_tags_source_external`: UNIQUE BTREE (`source`, `external_tag_id`)

### `teacher_published_courses` (9 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `course_id` | `varchar(128)` | NO | UNI | `NULL` |  |  |
| `publisher_user_id` | `bigint` | NO | MUL | `NULL` |  |  |
| `semester_plan` | `varchar(512)` | YES |  | `NULL` |  |  |
| `course_overview` | `text` | YES |  | `NULL` |  |  |
| `course_detail` | `text` | YES |  | `NULL` |  |  |
| `video_file_path` | `varchar(512)` | YES |  | `NULL` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_teacher_published_courses_user`: INDEX BTREE (`publisher_user_id`)
- `uk_teacher_published_courses_course`: UNIQUE BTREE (`course_id`)

### `users` (9 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `username` | `varchar(64)` | NO | UNI | `NULL` |  |  |
| `password_hash` | `varchar(255)` | NO |  | `NULL` |  |  |
| `nickname` | `varchar(64)` | YES |  | `NULL` |  |  |
| `avatar_url` | `varchar(512)` | YES |  | `NULL` |  |  |
| `role` | `varchar(32)` | NO |  | `STUDENT` |  |  |
| `enabled` | `tinyint(1)` | NO |  | `1` |  |  |
| `created_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `updated_at` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `uk_users_username`: UNIQUE BTREE (`username`)

### `well_log_record` (6 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `user_id` | `bigint` | YES | MUL | `NULL` |  |  |
| `porosity` | `double` | NO |  | `NULL` |  |  |
| `oil_saturation` | `double` | NO |  | `NULL` |  |  |
| `report_json` | `json` | NO |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)
- `idx_well_log_record_user_time`: INDEX BTREE (`user_id`, `create_time`)

Foreign keys:
- `fk_well_log_record_user_id`: (`user_id`) -> `users`(`id`), ON UPDATE NO ACTION, ON DELETE NO ACTION

### `well_log_template` (7 columns)

| Column | Type | Nullable | Key | Default | Extra | Comment |
|---|---|---|---|---|---|---|
| `id` | `bigint` | NO | PRI | `NULL` | auto_increment |  |
| `template_name` | `varchar(128)` | NO |  | `NULL` |  |  |
| `depth_array` | `json` | NO |  | `NULL` |  |  |
| `gr_base` | `json` | NO |  | `NULL` |  |  |
| `remark` | `varchar(512)` | YES |  | `NULL` |  |  |
| `create_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED |  |
| `update_time` | `datetime` | NO |  | `CURRENT_TIMESTAMP` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |  |

Indexes:
- `PRIMARY`: UNIQUE BTREE (`id`)

