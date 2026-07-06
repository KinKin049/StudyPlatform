from pathlib import Path

base = Path("StudyPlatform-back/src/main/resources/db/migration")
for name in [
    "V3__create_oj_tables.sql",
    "V14__create_academy_question_bank_tables.sql",
    "V25__create_profile_learning_events.sql",
    "V32__create_academy_assignment_tables.sql",
    "V36__create_academy_exam_tables.sql",
]:
    print(f"\n--- {name} ---")
    print((base / name).read_text(encoding="utf-8", errors="replace")[:16000])
