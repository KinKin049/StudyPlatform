CREATE TABLE IF NOT EXISTS course_question_bank_categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(64) NOT NULL,
  description VARCHAR(512) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_question_bank_categories_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_question_bank_sets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  set_code VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  subtitle VARCHAR(128) NULL,
  description VARCHAR(512) NULL,
  cover_url VARCHAR(512) NULL,
  cover_file_path VARCHAR(255) NULL,
  difficulty_label VARCHAR(64) NULL,
  status_label VARCHAR(64) NULL,
  source_name VARCHAR(128) NULL,
  source_url VARCHAR(512) NULL,
  source_refs JSON NULL,
  route_path VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_question_bank_sets_code (set_code),
  KEY idx_course_question_bank_sets_category (category_id),
  CONSTRAINT fk_course_question_bank_sets_category
    FOREIGN KEY (category_id) REFERENCES course_question_bank_categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_question_bank_questions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  set_id BIGINT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  stem TEXT NOT NULL,
  options_json JSON NULL,
  answer TEXT NULL,
  explanation TEXT NULL,
  difficulty_label VARCHAR(64) NULL,
  source_url VARCHAR(512) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_question_bank_questions_set (set_id),
  CONSTRAINT fk_course_question_bank_questions_set
    FOREIGN KEY (set_id) REFERENCES course_question_bank_sets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO course_question_bank_categories (category_code, category_name, description, sort_order)
VALUES
  ('computer', '计算机专业', '覆盖程序设计、数据库、数据结构等计算机基础课程题库。', 10),
  ('english', '英语四六级', '面向 CET-4、CET-6 的词汇、阅读、听力和写作训练。', 20),
  ('public', '公共课', '覆盖马克思主义原理、近现代史纲要、思想道德与法治等公共基础课程。', 30),
  ('qualification', '职业资格', '面向教师资格证、计算机等级考试、普通话测试等证书考试。', 40)
ON DUPLICATE KEY UPDATE
  category_name = VALUES(category_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

INSERT INTO course_question_bank_sets
  (category_id, set_code, title, subtitle, description, cover_url, cover_file_path, difficulty_label,
   status_label, source_name, source_url, source_refs, route_path, sort_order)
SELECT c.id, data.set_code, data.title, data.subtitle, data.description, data.cover_url, data.cover_file_path,
       data.difficulty_label, data.status_label, data.source_name, data.source_url, data.source_refs,
       data.route_path, data.sort_order
FROM course_question_bank_categories c
JOIN (
  SELECT 'computer' AS category_code, 'c-language' AS set_code, 'C语言题库' AS title,
         'C语言程序设计(下)' AS subtitle,
         '围绕基础语法、分支循环、数组、函数和指针建立课程练习入口。' AS description,
         'https://edu-image.nosdn.127.net/CBE2AB75BF5450085811FDFCB4E38870.jpg?imageView&thumbnail=426y240&quality=100' AS cover_url,
         'storage/online_course/计算机/46004_1476538444.jpg' AS cover_file_path,
         '基础到综合' AS difficulty_label,
         '已接入' AS status_label,
         'Examcoo 题库入口' AS source_name,
         'https://examcoo.com/editor/do/view/id/2251893' AS source_url,
         JSON_ARRAY(
           'https://examcoo.com/editor/do/view/id/2251893',
           'https://examcoo.com/editor/do/view/id/3000334',
           'https://examcoo.com/editor/do/view/id/984085'
         ) AS source_refs,
         '/academy/question-bank/courses/c-language' AS route_path,
         10 AS sort_order
  UNION ALL
  SELECT 'computer', 'java', 'Java题库', 'Java程序设计', '面向 Java 基础语法、面向对象和集合框架的课程题库。',
         'https://mooc-image.nosdn.127.net/3e185a4f596f4a54b4ce8727d33faa5d.jpg',
         'storage/online_course/计算机/1473176181_1476745579.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 20
  UNION ALL
  SELECT 'computer', 'python', 'Python题库', 'Python程序设计', '面向 Python 语法、函数、数据结构和应用编程的题库。',
         'https://mooc-image.nosdn.127.net/3e185a4f596f4a54b4ce8727d33faa5d.jpg',
         'storage/online_course/计算机/1473176181_1476745579.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 30
  UNION ALL
  SELECT 'computer', 'database', '数据库题库', '数据库系统概论', '覆盖关系模型、SQL 查询、事务和范式设计。',
         'https://edu-image.nosdn.127.net/66030EC707D1D621F63C29AB2D92A04B.jpg?imageView&thumbnail=510y288&quality=100',
         'storage/online_course/计算机/1207042802_1476755441.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 40
  UNION ALL
  SELECT 'computer', 'data-structure', '数据结构题库', '数据结构', '覆盖线性表、树、图、查找和排序等核心知识点。',
         'https://mooc-image.nosdn.127.net/0ee67821ee8f4c1183e27c3541d833ae.jpg',
         'storage/online_course/理学/1474462161_1476202443.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 50
  UNION ALL
  SELECT 'english', 'cet4-vocabulary', '四级词汇', 'CET-4', '高频词汇、短语搭配与基础语法训练。',
         'https://edu-image.nosdn.127.net/3E630378563072DC7EE918282B891F5B.png?imageView&thumbnail=510y288&quality=100',
         'storage/online_course/外语/1003632002_1476832444.png', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 10
  UNION ALL
  SELECT 'english', 'cet4-reading', '四级阅读', 'CET-4', '阅读理解、长篇匹配和仔细阅读专项训练。',
         'https://mooc-image.nosdn.127.net/dde707dcc2f4447296d521f3136818da.jpg',
         'storage/online_course/外语/1473099163_1476794459.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 20
  UNION ALL
  SELECT 'english', 'cet6-listening', '六级听力', 'CET-6', '听力新闻、长对话和篇章理解训练入口。',
         'https://nos.netease.com/edu-image/ae260b0225cf4f15b75e801e4675a276.png',
         'storage/online_course/外语/1464931170_1476649481.png', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 30
  UNION ALL
  SELECT 'english', 'cet6-writing', '六级写作', 'CET-6', '作文模板、翻译表达和综合写作训练。',
         'https://mooc-image.nosdn.127.net/e2607c3490f04762a4b7a5d740a87a44.jpg',
         'storage/online_course/外语/1474717164_1476478499.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 40
  UNION ALL
  SELECT 'public', 'marxism', '马克思主义基本原理', '公共基础课', '哲学、政治经济学和科学社会主义基础知识训练。',
         'https://p.ananas.chaoxing.com/star3/origin/e806eecd9918a02038e99b3b5913abf6.png',
         'storage/general_course/尔雅通识课/25541792.png', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 10
  UNION ALL
  SELECT 'public', 'modern-history', '中国近现代史纲要', '公共基础课', '围绕近现代史重要事件、人物和历史逻辑建立题库。',
         'https://p.ananas.chaoxing.com/star3/origin/21da68b3505f8bc8a738d0ccc7a08fd9.jpg',
         'storage/general_course/尔雅通识课/25541789.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 20
  UNION ALL
  SELECT 'public', 'ideology', '思想道德与法治', '公共基础课', '覆盖道德修养、法律基础和时代新人培养相关题型。',
         'https://edu-image.nosdn.127.net/5FE1274AAD28ADB903AE49A47E2D5BBB.png?imageView&thumbnail=426y240&quality=100',
         'storage/online_course/文学历史/1002025001_1476793480.png', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 30
  UNION ALL
  SELECT 'public', 'maoism', '毛泽东思想和中国特色社会主义理论体系概论', '公共基础课', '围绕理论体系、时代主题和实践案例进行练习。',
         'https://edu-image.nosdn.127.net/9BC10A26C19CBFB9D5F0B67726A165DB.jpg?imageView&thumbnail=510y288&quality=100',
         'storage/online_course/文学历史/1207424804_1476734557.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 40
  UNION ALL
  SELECT 'qualification', 'teacher-cert', '教师资格证', '职业资格', '教育知识、综合素质和学科能力模拟练习。',
         'https://p.ananas.chaoxing.com/star3/origin/2942991237b3811e3a2708c472db517c.jpg',
         'storage/general_course/尔雅通识课/46642911.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 10
  UNION ALL
  SELECT 'qualification', 'ncre', '计算机等级考试', '职业资格', '二级 C、Office 和程序设计基础专项训练。',
         'https://edu-image.nosdn.127.net/E8E3A8FCBCB1A24CF9BF663485F9DDF8.jpg?imageView&thumbnail=510y288&quality=100',
         'storage/online_course/计算机/1207176813_1476681455.jpg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 20
  UNION ALL
  SELECT 'qualification', 'mandarin', '普通话测试', '职业资格', '普通话水平测试朗读、命题说话和字词训练。',
         'https://p.ananas.chaoxing.com/star3/origin/53a58be865714a5eb519d8652b824ae5.png',
         'storage/general_course/尔雅通识课/25541795.png', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 30
  UNION ALL
  SELECT 'qualification', 'accounting-basic', '会计从业基础', '职业资格', '会计要素、凭证、账簿和财务处理基础练习。',
         'https://mooc-image.nosdn.127.net/617c051e9fd746ed85376a53e4ebd6f8.jpeg',
         'storage/online_course/经济管理/1462120171_1476781489.jpeg', '建设中', '建设中',
         '课程题库', NULL, JSON_ARRAY(), NULL, 40
) data ON data.category_code = c.category_code
ON DUPLICATE KEY UPDATE
  category_id = VALUES(category_id),
  title = VALUES(title),
  subtitle = VALUES(subtitle),
  description = VALUES(description),
  cover_url = VALUES(cover_url),
  cover_file_path = VALUES(cover_file_path),
  difficulty_label = VALUES(difficulty_label),
  status_label = VALUES(status_label),
  source_name = VALUES(source_name),
  source_url = VALUES(source_url),
  source_refs = VALUES(source_refs),
  route_path = VALUES(route_path),
  sort_order = VALUES(sort_order);

INSERT INTO course_question_bank_questions
  (set_id, question_type, stem, options_json, answer, explanation, difficulty_label, source_url, sort_order)
SELECT s.id, q.question_type, q.stem, q.options_json, q.answer, q.explanation, q.difficulty_label, q.source_url, q.sort_order
FROM course_question_bank_sets s
JOIN (
  SELECT 'c-language' AS set_code, 'single' AS question_type,
         '在 C 语言中，以下哪个符号用于取变量的地址？' AS stem,
         JSON_ARRAY('A. *', 'B. &', 'C. %', 'D. #') AS options_json,
         'B' AS answer,
         '& 是取地址运算符，常用于 scanf 或指针初始化。' AS explanation,
         '基础' AS difficulty_label,
         'https://examcoo.com/editor/do/view/id/2251893' AS source_url,
         10 AS sort_order
  UNION ALL
  SELECT 'c-language', 'single',
         '表达式 sizeof(char) 在标准 C 语言中的结果通常是多少？',
         JSON_ARRAY('A. 1', 'B. 2', 'C. 4', 'D. 与编译器无关但不是固定值'),
         'A',
         'C 标准规定 sizeof(char) 的结果为 1，单位是字节。',
         '基础',
         'https://examcoo.com/editor/do/view/id/2251893',
         20
  UNION ALL
  SELECT 'c-language', 'single',
         '若 int a = 5, b = 2; 则表达式 a / b 的值是？',
         JSON_ARRAY('A. 2', 'B. 2.5', 'C. 3', 'D. 0'),
         'A',
         '两个 int 相除执行整数除法，小数部分被截断。',
         '基础',
         'https://examcoo.com/editor/do/view/id/2251893',
         30
  UNION ALL
  SELECT 'c-language', 'multiple',
         '以下哪些写法可以表示循环结构？',
         JSON_ARRAY('A. for 语句', 'B. while 语句', 'C. do while 语句', 'D. switch 语句'),
         'A,B,C',
         'for、while、do while 都是循环结构；switch 是多分支选择结构。',
         '基础',
         'https://examcoo.com/editor/do/view/id/2251893',
         40
  UNION ALL
  SELECT 'c-language', 'single',
         '数组 int a[10]; 中最后一个元素的下标是？',
         JSON_ARRAY('A. 9', 'B. 10', 'C. 11', 'D. 不确定'),
         'A',
         'C 数组下标从 0 开始，长度为 10 的数组最后一个下标是 9。',
         '基础',
         'https://examcoo.com/editor/do/view/id/2251893',
         50
  UNION ALL
  SELECT 'c-language', 'short',
         '简述函数参数按值传递的含义。',
         JSON_ARRAY(),
         '函数接收实参值的副本，函数内部修改形参通常不会改变原实参。',
         '按值传递强调复制数据；如需修改外部变量，通常传入地址并通过指针间接访问。',
         '应用',
         'https://examcoo.com/editor/do/view/id/2251893',
         60
  UNION ALL
  SELECT 'c-language', 'single',
         '以下哪一个头文件通常用于 printf 和 scanf？',
         JSON_ARRAY('A. stdio.h', 'B. math.h', 'C. string.h', 'D. stdlib.h'),
         'A',
         'printf 和 scanf 声明在 stdio.h 中。',
         '基础',
         'https://examcoo.com/editor/do/view/id/2251893',
         70
  UNION ALL
  SELECT 'c-language', 'short',
         '写出一个判断整数 n 是否为偶数的 C 语言条件表达式。',
         JSON_ARRAY(),
         'n % 2 == 0',
         '取模运算 % 可以得到除法余数，余数为 0 表示该整数可被 2 整除。',
         '应用',
         'https://examcoo.com/editor/do/view/id/2251893',
         80
) q ON q.set_code = s.set_code
WHERE NOT EXISTS (
  SELECT 1
  FROM course_question_bank_questions existing
  WHERE existing.set_id = s.id AND existing.sort_order = q.sort_order
);
