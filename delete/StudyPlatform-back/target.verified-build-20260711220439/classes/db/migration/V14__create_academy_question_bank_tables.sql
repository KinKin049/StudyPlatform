CREATE TABLE IF NOT EXISTS question_bank_subjects (
  id BIGINT NOT NULL AUTO_INCREMENT,
  subject_code VARCHAR(64) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  description VARCHAR(512) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_bank_subjects_code (subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_bank_tags (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source VARCHAR(32) NOT NULL,
  external_tag_id INT NOT NULL,
  tag_name VARCHAR(128) NOT NULL,
  tag_type INT NULL,
  parent_external_tag_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_bank_tags_source_external (source, external_tag_id),
  KEY idx_question_bank_tags_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_bank_problems (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source VARCHAR(32) NOT NULL,
  external_problem_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  difficulty INT NULL,
  difficulty_label VARCHAR(32) NULL,
  tag_ids JSON NULL,
  tag_names JSON NULL,
  description MEDIUMTEXT NULL,
  input_description MEDIUMTEXT NULL,
  output_description MEDIUMTEXT NULL,
  hint MEDIUMTEXT NULL,
  total_submit INT NULL,
  total_accepted INT NULL,
  source_url VARCHAR(512) NOT NULL,
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_bank_problems_source_external (source, external_problem_id),
  KEY idx_question_bank_problems_difficulty (difficulty),
  KEY idx_question_bank_problems_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_bank_problem_subjects (
  problem_id BIGINT NOT NULL,
  subject_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (problem_id, subject_id),
  KEY idx_question_bank_problem_subjects_subject (subject_id),
  CONSTRAINT fk_question_bank_problem_subjects_problem
    FOREIGN KEY (problem_id) REFERENCES question_bank_problems (id) ON DELETE CASCADE,
  CONSTRAINT fk_question_bank_problem_subjects_subject
    FOREIGN KEY (subject_id) REFERENCES question_bank_subjects (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO question_bank_subjects (subject_code, subject_name, description, sort_order)
VALUES
  ('c-language', 'C语言程序设计', '面向 C 语言基础语法、数组、字符串、函数和基础算法训练。', 10),
  ('java', 'Java程序设计', '面向 Java 基础语法、面向对象、集合和算法题训练。', 20),
  ('python', 'Python程序设计', '面向 Python 基础语法、数据结构和编程思维训练。', 30)
ON DUPLICATE KEY UPDATE
  subject_name = VALUES(subject_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

INSERT INTO question_bank_tags (source, external_tag_id, tag_name, tag_type, parent_external_tag_id)
VALUES
  ('luogu', 1, '模拟', 2, 110),
  ('luogu', 2, '字符串', 2, NULL),
  ('luogu', 3, '动态规划 DP', 2, NULL),
  ('luogu', 4, '搜索', 2, NULL),
  ('luogu', 5, '数学', 2, NULL),
  ('luogu', 6, '图论', 2, NULL),
  ('luogu', 12, '递推', 2, 110)
ON DUPLICATE KEY UPDATE
  tag_name = VALUES(tag_name),
  tag_type = VALUES(tag_type),
  parent_external_tag_id = VALUES(parent_external_tag_id);

INSERT INTO question_bank_problems
  (source, external_problem_id, title, difficulty, difficulty_label, tag_ids, tag_names,
   description, input_description, output_description, hint, total_submit, total_accepted, source_url)
VALUES
  ('luogu', 'P1001', 'A+B Problem', 1, '入门', JSON_ARRAY(1), JSON_ARRAY('模拟'),
   '输入两个整数 a,b，输出它们的和。', '输入两个以空格分隔的整数 a,b。', '输出一个整数，表示 a+b。',
   '适合用于熟悉标准输入输出。', 2107924, 1216336, 'https://www.luogu.com.cn/problem/P1001'),
  ('luogu', 'P1002', '[NOIP 2002 普及组] 过河卒', 2, '普及-', JSON_ARRAY(3, 12), JSON_ARRAY('动态规划 DP', '递推'),
   '棋盘上有一个马控制若干点，求从左上角走到右下角且不经过被控制点的路径数。', '输入棋盘大小和马的位置。', '输出路径总数。',
   '可作为递推和动态规划入门题。', 1001072, 302461, 'https://www.luogu.com.cn/problem/P1002'),
  ('luogu', 'P1003', '[NOIP 2011 提高组] 铺地毯', 2, '普及-', JSON_ARRAY(1), JSON_ARRAY('模拟'),
   '给定多张地毯的位置与大小，查询某一点最上面的地毯编号。', '输入地毯数量、每张地毯参数和查询点。', '输出覆盖查询点的最上层地毯编号，没有则输出 -1。',
   '适合训练数组记录和逆序模拟。', 651461, 228547, 'https://www.luogu.com.cn/problem/P1003'),
  ('luogu', 'P1004', '[NOIP 2000 提高组] 方格取数', 4, '提高+/省选-', JSON_ARRAY(3), JSON_ARRAY('动态规划 DP'),
   '在方格中从左上角到右下角取数，两条路径总得分最大。', '输入方格大小和非零格子的坐标与数值。', '输出最大总得分。',
   '可用双线程动态规划理解状态转移。', 233802, 110833, 'https://www.luogu.com.cn/problem/P1004'),
  ('luogu', 'P1008', '[NOIP 1998 普及组] 三连击', 2, '普及-', JSON_ARRAY(1, 5), JSON_ARRAY('模拟', '数学'),
   '将 1 到 9 九个数字组成三个三位数，使其比例为 1:2:3。', '无输入。', '按格式输出所有满足条件的三连击。',
   '适合枚举、数字拆分和判重训练。', 552816, 226342, 'https://www.luogu.com.cn/problem/P1008'),
  ('luogu', 'P1019', '[NOIP 2000 提高组] 单词接龙', 4, '提高+/省选-', JSON_ARRAY(2, 4), JSON_ARRAY('字符串', '搜索'),
   '给定若干单词和起始字母，求可拼接出的最长接龙长度。', '输入单词数量、单词列表和起始字母。', '输出最长接龙长度。',
   '适合训练字符串匹配与 DFS 搜索。', 198165, 57184, 'https://www.luogu.com.cn/problem/P1019')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  difficulty = VALUES(difficulty),
  difficulty_label = VALUES(difficulty_label),
  tag_ids = VALUES(tag_ids),
  tag_names = VALUES(tag_names),
  description = VALUES(description),
  input_description = VALUES(input_description),
  output_description = VALUES(output_description),
  hint = VALUES(hint),
  total_submit = VALUES(total_submit),
  total_accepted = VALUES(total_accepted),
  source_url = VALUES(source_url);

INSERT IGNORE INTO question_bank_problem_subjects (problem_id, subject_id)
SELECT p.id, s.id
FROM question_bank_problems p
JOIN question_bank_subjects s ON s.subject_code IN ('c-language', 'java', 'python')
WHERE p.external_problem_id IN ('P1001', 'P1002', 'P1003', 'P1004', 'P1008', 'P1019');
