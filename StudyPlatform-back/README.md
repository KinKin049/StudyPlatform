# StudyPlatform Back

## Local MySQL setup

Create the local database first:

```sql
CREATE DATABASE IF NOT EXISTS study_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

The same SQL is available in `sql/create-database.sql`.

Copy the local configuration template:

```powershell
Copy-Item application-local.example.properties application-local.properties
```

Then edit `application-local.properties` with your own MySQL username and password. This file is ignored by Git.

If you start the application from the repository root in IntelliJ, the backend also supports:

```text
StudyPlatform-back/application-local.properties
```

The most common startup error is:

```text
Access denied for user 'root'@'localhost' (using password: NO)
```

That means the application did not receive a MySQL password and fell back to the default empty password. Create `application-local.properties` or set `DB_USERNAME` and `DB_PASSWORD` in the run configuration.

## Flyway migrations

Flyway runs automatically when the Spring Boot application starts. Migration files are stored in:

```text
src/main/resources/db/migration
```

Naming format:

```text
V1__init_schema.sql
V2__create_user_table.sql
V3__create_course_table.sql
```

Rules for team development:

- Do not edit a migration file that has already been pulled or executed by others.
- Add a new versioned migration file for every database schema change.
- Commit migration files together with the backend code that depends on them.
- Do not commit local database passwords.

## Run

Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

If port `8080` is already in use, start with another port:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Check Flyway migration status manually:

```powershell
.\mvnw.cmd flyway:info '-Ddb.username=root' '-Ddb.password=your_mysql_password'
```

Run migrations manually:

```powershell
.\mvnw.cmd flyway:migrate '-Ddb.username=root' '-Ddb.password=your_mysql_password'
```

Run tests:

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database, so they can run without a local MySQL instance.

## OJ module

The OJ backend module is under `src/main/java/com/cupk/oj`.

It provides problem, test case, submission, and judge-result APIs. User code is not executed in this Spring Boot service. Configure an external sandbox service with:

```properties
oj.sandbox-url=http://localhost:9000
```

See `docs/oj-module.md` for API details and the sandbox contract.

## AI pet model

The AI pet calls the model through the backend proxy endpoint `POST /api/ai-pet/chat`.
Do not put the API key in the Vue frontend. Configure it in `application-local.properties`
or environment variables:

```properties
ai.pet.base-url=https://yunwu.ai
ai.pet.api-key=your_token_here
ai.pet.model=deepseek-v4-falsh
```

Environment variables are also supported:

```text
AI_PET_BASE_URL=https://yunwu.ai
AI_PET_API_KEY=your_token_here
AI_PET_MODEL=deepseek-v4-falsh
```

The proxy uses an OpenAI-compatible chat endpoint:

```text
POST {ai.pet.base-url}/v1/chat/completions
```
