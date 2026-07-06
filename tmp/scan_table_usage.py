from pathlib import Path

root = Path("StudyPlatform-back/src/main/java/com/cupk")
tables = [
    "users",
    "auth_users",
    "profile_user_profiles",
    "profile_learning_events",
    "profile_learning_time_records",
    "coin_reward_records",
    "online_open_courses",
    "general_courses",
    "micro_major_courses",
    "teacher_published_courses",
    "academy_course_enrollments",
    "academy_course_reviews",
    "excellent_textbooks",
    "academy_textbook_details",
    "academy_textbook_cart_items",
    "academy_textbook_orders",
    "academy_textbook_order_items",
    "academy_textbook_reviews",
    "course_question_bank_categories",
    "course_question_bank_sets",
    "course_question_bank_questions",
    "course_question_bank_mistakes",
    "course_question_bank_favorites",
    "question_bank_subjects",
    "question_bank_tags",
    "question_bank_problems",
    "question_bank_problem_subjects",
    "academy_assignments",
    "academy_assignment_questions",
    "academy_assignment_submissions",
    "academy_exams",
    "academy_exam_questions",
    "academy_exam_submissions",
    "oj_problems",
    "oj_test_cases",
    "oj_submissions",
    "oj_submission_cases",
    "game_ladder_jump_records",
    "game_type_warrior_records",
    "production_reservoir_record",
    "production_pump_record",
    "production_waterflood_record",
    "production_stimulation_record",
    "well_log_record",
    "well_log_template",
    "password_reset_codes",
]

usage = {table: set() for table in tables}
for path in root.rglob("*.java"):
    text = path.read_text(encoding="utf-8", errors="ignore")
    for table in tables:
        if table in text:
            usage[table].add(str(path.relative_to(root)))

for table in tables:
    files = sorted(usage[table])
    print(f"{table}: {len(files)}")
    for file in files[:8]:
        print(f"  {file}")
