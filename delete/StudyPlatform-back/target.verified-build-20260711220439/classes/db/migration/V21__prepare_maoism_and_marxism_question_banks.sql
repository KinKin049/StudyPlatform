UPDATE course_question_bank_sets
SET title = '马克思主义基本原理',
    subtitle = '公共基础课',
    description = '围绕马克思主义哲学、政治经济学和科学社会主义基础知识建立课程练习题库。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = '马克思主义基本原理 JSON 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/questions.json'),
    route_path = '/academy/question-bank/courses/marxism',
    sort_order = 10
WHERE set_code = 'marxism';

UPDATE course_question_bank_sets
SET title = '毛泽东思想和中国特色社会主义理论体系概论',
    subtitle = '公共基础课',
    description = '围绕马克思主义中国化时代化、毛泽东思想和中国特色社会主义理论体系建立课程练习题库。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = '毛泽东思想和中国特色社会主义理论体系概论 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/maozhongte-exam.html'),
    route_path = '/academy/question-bank/courses/maoism',
    sort_order = 40
WHERE set_code = 'maoism';

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code IN ('marxism', 'maoism');
