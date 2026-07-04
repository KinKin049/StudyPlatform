UPDATE course_question_bank_sets
SET title = '中国近现代史纲要',
    subtitle = '公共基础课',
    description = '围绕中国近现代史纲要中的重要事件、人物、会议和理论线索建立单选练习题库。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = '中国近现代史纲要 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/zhongguojindaishigangyao.html'),
    route_path = '/academy/question-bank/courses/modern-history',
    sort_order = 20
WHERE set_code = 'modern-history';

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code = 'modern-history';
