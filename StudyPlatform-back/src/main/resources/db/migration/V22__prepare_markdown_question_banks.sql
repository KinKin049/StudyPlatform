UPDATE course_question_bank_sets
SET title = '数据库题库',
    subtitle = '全国计算机等级考试二级 MySQL',
    description = '围绕 MySQL 数据库基础、SQL 语句、视图、存储过程、权限和备份恢复建立课程练习题库。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = '全国计算机等级考试二级 MySQL Markdown 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/National-Computer-Rank-Examination-Level-2-MySQL.md'),
    route_path = '/academy/question-bank/courses/database',
    sort_order = 40
WHERE set_code = 'database';

UPDATE course_question_bank_sets
SET title = 'Python题库',
    subtitle = 'Python 程序设计',
    description = '基于 Python 选择题错题集整理数据结构、语法、函数和组合数据类型练习题。',
    difficulty_label = '基础到综合',
    status_label = '已接入',
    source_name = 'python-study-note Markdown 题库',
    source_url = 'https://github.com/zmn626/python-study-note/tree/master',
    source_refs = JSON_ARRAY('CET46/python-study-note.md', 'https://github.com/zmn626/python-study-note/tree/master'),
    route_path = '/academy/question-bank/courses/python',
    sort_order = 30
WHERE set_code = 'python';

UPDATE course_question_bank_sets
SET title = '计算机等级考试',
    subtitle = '二级公共基础与 C 语言错题复习',
    description = '按运算符、控制结构、数组、指针、数据库和公共基础等主题整理计算机等级考试错题复盘卡片。',
    difficulty_label = '错题复习',
    status_label = '已接入',
    source_name = '计算机等级考试 Markdown 错题复习',
    source_url = NULL,
    source_refs = JSON_ARRAY('CET46/jisuanjierji.md'),
    route_path = '/academy/question-bank/courses/ncre',
    sort_order = 20
WHERE set_code = 'ncre';

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code IN ('database', 'python', 'ncre');
