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

Tests disable database auto-configuration, so they can run without a local MySQL instance.
