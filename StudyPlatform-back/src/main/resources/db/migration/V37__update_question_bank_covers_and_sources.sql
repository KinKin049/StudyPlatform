UPDATE course_question_bank_sets
SET cover_file_path = CASE set_code
    WHEN 'cet4' THEN 'storage/question-bank/covers/cet4-vocabulary.jpg'
    WHEN 'cet6' THEN 'storage/question-bank/covers/cet6.jpg'
    WHEN 'java' THEN 'storage/question-bank/covers/java.jpg'
    WHEN 'marxism' THEN 'storage/question-bank/covers/marxism.jpg'
    WHEN 'modern-history' THEN 'storage/question-bank/covers/modern-history.jpg'
    WHEN 'ideology' THEN 'storage/question-bank/covers/ideology-law.jpg'
    WHEN 'maoism' THEN 'storage/question-bank/covers/maoism-socialism-theory.jpg'
    WHEN 'teacher-cert' THEN 'storage/question-bank/covers/teacher-cert.jpg'
    WHEN 'counselor' THEN 'storage/question-bank/covers/counselor.jpg'
    WHEN 'ncre' THEN 'storage/question-bank/covers/ncre.jpg'
    ELSE cover_file_path
  END,
  cover_url = CASE
    WHEN set_code IN ('cet4', 'cet6', 'java', 'marxism', 'modern-history', 'ideology', 'maoism', 'teacher-cert', 'counselor', 'ncre') THEN NULL
    ELSE cover_url
  END
WHERE set_code IN ('cet4', 'cet6', 'java', 'marxism', 'modern-history', 'ideology', 'maoism', 'teacher-cert', 'counselor', 'ncre');

UPDATE course_question_bank_sets
SET source_name = '项目内置 CET4 词汇表',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/cet4-vocabulary.json')
WHERE set_code = 'cet4';

UPDATE course_question_bank_sets
SET source_name = '项目内置 CET6 词汇表',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/cet6-vocabulary.json')
WHERE set_code = 'cet6';

UPDATE course_question_bank_sets
SET source_name = '项目内置中国近现代史纲要 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/modern-history.html')
WHERE set_code = 'modern-history';

UPDATE course_question_bank_sets
SET source_name = '项目内置思想道德与法治 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/ideology-law.html')
WHERE set_code = 'ideology';

UPDATE course_question_bank_sets
SET source_name = '项目内置马克思主义基本原理 JSON 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/marxism.json')
WHERE set_code = 'marxism';

UPDATE course_question_bank_sets
SET source_name = '项目内置毛泽东思想和中国特色社会主义理论体系 HTML 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/maoism.html')
WHERE set_code = 'maoism';

UPDATE course_question_bank_sets
SET source_name = '项目内置全国计算机等级考试二级 MySQL Markdown 题库',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/database-mysql.md')
WHERE set_code = 'database';

UPDATE course_question_bank_sets
SET source_name = '项目内置 python-study-note Markdown 题库',
    source_url = 'https://github.com/zmn626/python-study-note/tree/master',
    source_refs = JSON_ARRAY('question-bank-sources/python-study-note.md', 'https://github.com/zmn626/python-study-note/tree/master')
WHERE set_code = 'python';

UPDATE course_question_bank_sets
SET source_name = '项目内置计算机等级考试 Markdown 错题复习',
    source_url = NULL,
    source_refs = JSON_ARRAY('question-bank-sources/ncre.md')
WHERE set_code = 'ncre';
