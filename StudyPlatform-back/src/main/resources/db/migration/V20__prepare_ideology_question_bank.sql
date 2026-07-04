UPDATE course_question_bank_sets
SET title = '思想道德与法治',
    subtitle = '公共基础课',
    description = '覆盖思想修养、道德建设、法治观念和时代新人培养等内容的课程练习题库。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = '思想道德与法治 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/sixiang-dao-de-yu-fazhi-quiz.html'),
    route_path = '/academy/question-bank/courses/ideology',
    sort_order = 30
WHERE set_code = 'ideology';

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code = 'ideology';
