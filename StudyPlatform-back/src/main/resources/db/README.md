# Database SQL Layout

This directory is the single source for backend database SQL files.

## Directories

- `migration/`: Flyway-managed migrations. Spring Boot executes these automatically on startup.
- `manual/`: Manual setup SQL that is not executed automatically, such as creating the local database.

## Rules

- Add schema or seed changes as a new `migration/V{number}__description.sql` file.
- Do not edit, rename, or move migrations that may already have been executed.
- Do not place runtime SQL scripts in `StudyPlatform-back/sql` or the repository root.
- Keep temporary export, debug, and local-only SQL files outside the repository.
