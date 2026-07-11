UPDATE course_question_bank_sets
SET title = '四级词汇',
    subtitle = 'CET-4',
    description = '基于 CET4 词汇 JSON 数据建立背单词卡片，适合顺序记忆、随机抽查和错词复习。',
    difficulty_label = '核心词汇',
    status_label = '已接入',
    source_name = 'CET46 JSON 词汇表',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/3-CET4-顺序.json'),
    route_path = '/academy/question-bank/courses/cet4',
    sort_order = 10
WHERE set_code = 'cet4';

UPDATE course_question_bank_sets
SET title = '六级词汇',
    subtitle = 'CET-6',
    description = '基于 CET6 词汇 JSON 数据建立背单词卡片，适合顺序记忆、随机抽查和错词复习。',
    difficulty_label = '核心词汇',
    status_label = '已接入',
    source_name = 'CET46 JSON 词汇表',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/4-CET6-顺序.json'),
    route_path = '/academy/question-bank/courses/cet6',
    sort_order = 20
WHERE set_code = 'cet6';

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code IN ('cet4', 'cet6');
