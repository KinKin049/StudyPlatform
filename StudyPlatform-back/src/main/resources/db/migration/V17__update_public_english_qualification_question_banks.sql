DELETE FROM course_question_bank_sets
WHERE category_id = (
  SELECT id FROM course_question_bank_categories WHERE category_code = 'english'
)
AND set_code NOT IN ('cet4', 'cet6');

INSERT INTO course_question_bank_sets
  (category_id, set_code, title, subtitle, description, cover_url, cover_file_path, difficulty_label,
   status_label, source_name, source_url, source_refs, route_path, sort_order)
SELECT c.id, data.set_code, data.title, data.subtitle, data.description, data.cover_url, data.cover_file_path,
       data.difficulty_label, data.status_label, data.source_name, data.source_url, data.source_refs,
       data.route_path, data.sort_order
FROM course_question_bank_categories c
JOIN (
  SELECT 'english' AS category_code, 'cet4' AS set_code, '大学英语四级（CET4）' AS title,
         '英语四六级' AS subtitle,
         '围绕四级词汇、阅读理解、翻译和写作建立练习题库。' AS description,
         'https://edu-image.nosdn.127.net/3E630378563072DC7EE918282B891F5B.png?imageView&thumbnail=510y288&quality=100' AS cover_url,
         'storage/online_course/外语/1003632002_1476832444.png' AS cover_file_path,
         '基础到综合' AS difficulty_label,
         '已接入' AS status_label,
         'Examcoo CET4 题库入口' AS source_name,
         'https://examcoo.com/editor/do/view/id/125926' AS source_url,
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/125926') AS source_refs,
         '/academy/question-bank/courses/cet4' AS route_path,
         10 AS sort_order
  UNION ALL
  SELECT 'english', 'cet6', '大学英语六级（CET6）', '英语四六级',
         '围绕六级阅读、长难句、翻译和综合应用能力建立练习题库。',
         'https://mooc-image.nosdn.127.net/dde707dcc2f4447296d521f3136818da.jpg',
         'storage/online_course/外语/1473099163_1476794459.jpg',
         '基础到综合',
         '已接入',
         'Examcoo CET6 题库入口',
         'https://examcoo.com/editor/do/view/id/3077621',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/3077621'),
         '/academy/question-bank/courses/cet6',
         20
  UNION ALL
  SELECT 'public', 'marxism', '马克思主义基本原理', '公共基础课',
         '围绕马克思主义哲学、政治经济学和科学社会主义基础知识建立练习题库。',
         'https://p.ananas.chaoxing.com/star3/origin/e806eecd9918a02038e99b3b5913abf6.png',
         'storage/general_course/尔雅通识课/25541792.png',
         '基础到综合',
         '已接入',
         'Examcoo 马克思主义原理题库入口',
         'https://examcoo.com/editor/do/view/id/152441',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/152441'),
         '/academy/question-bank/courses/marxism',
         10
  UNION ALL
  SELECT 'public', 'reform-opening', '改革开放', '公共基础课',
         '围绕改革开放历史进程、重要会议、理论创新和实践成就建立练习题库。',
         'https://p.ananas.chaoxing.com/star3/origin/21da68b3505f8bc8a738d0ccc7a08fd9.jpg',
         'storage/general_course/尔雅通识课/25541789.jpg',
         '基础到综合',
         '已接入',
         'Examcoo 改革开放题库入口',
         'https://examcoo.com/editor/do/view/id/819828',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/819828'),
         '/academy/question-bank/courses/reform-opening',
         25
  UNION ALL
  SELECT 'qualification', 'teacher-cert', '教师资格证', '职业资格',
         '围绕综合素质、教育知识与能力和教学基本规范建立练习题库。',
         'https://p.ananas.chaoxing.com/star3/origin/2942991237b3811e3a2708c472db517c.jpg',
         'storage/general_course/尔雅通识课/46642911.jpg',
         '基础到综合',
         '已接入',
         'Examcoo 教师资格证题库入口',
         'https://examcoo.com/editor/do/view/id/113521',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/113521'),
         '/academy/question-bank/courses/teacher-cert',
         10
  UNION ALL
  SELECT 'qualification', 'counselor', '心理咨询师考核', '职业资格',
         '围绕普通心理学、咨询伦理、测量与咨询技能建立练习题库。',
         'https://mooc-image.nosdn.127.net/6115d0cd750f4141ae20debc7a0b0b42.png',
         'storage/online_course/心理学/1473188164_1487078442.png',
         '基础到综合',
         '已接入',
         'Examcoo 心理咨询师题库入口',
         'https://examcoo.com/editor/do/view/id/151921',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/151921'),
         '/academy/question-bank/courses/counselor',
         35
  UNION ALL
  SELECT 'qualification', 'accounting-basic', '会计从业基础', '职业资格',
         '围绕会计基础、会计要素、凭证账簿和财务处理建立练习题库。',
         'https://mooc-image.nosdn.127.net/617c051e9fd746ed85376a53e4ebd6f8.jpeg',
         'storage/online_course/经济管理/1462120171_1476781489.jpeg',
         '基础到综合',
         '已接入',
         'Examcoo 会计从业基础题库入口',
         'https://examcoo.com/editor/do/view/id/2246598',
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/2246598'),
         '/academy/question-bank/courses/accounting-basic',
         40
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

DELETE q
FROM course_question_bank_questions q
JOIN course_question_bank_sets s ON s.id = q.set_id
WHERE s.set_code IN ('cet4', 'cet6', 'marxism', 'reform-opening', 'teacher-cert', 'counselor', 'accounting-basic');

INSERT INTO course_question_bank_questions
  (set_id, question_type, stem, options_json, answer, explanation, difficulty_label, source_url, sort_order)
SELECT s.id, q.question_type, q.stem, q.options_json, q.answer, q.explanation, q.difficulty_label, q.source_url, q.sort_order
FROM course_question_bank_sets s
JOIN (
  SELECT 'cet4' AS set_code, 'single' AS question_type,
         'CET4 阅读理解中，遇到生词时较合理的策略是？' AS stem,
         JSON_ARRAY('A. 立即放弃该段', 'B. 结合上下文推断词义', 'C. 只看选项不读文章', 'D. 将所有生词逐个翻译') AS options_json,
         'B' AS answer,
         '四级阅读强调根据上下文、转折关系和主题句推断信息。' AS explanation,
         '基础' AS difficulty_label,
         'https://examcoo.com/editor/do/view/id/125926' AS source_url,
         10 AS sort_order
  UNION ALL
  SELECT 'cet4', 'single', 'Which word is closest in meaning to “purchase”?',
         JSON_ARRAY('A. buy', 'B. borrow', 'C. sell', 'D. repair'),
         'A', 'purchase 常表示“购买”，与 buy 意义最接近。', '基础', 'https://examcoo.com/editor/do/view/id/125926', 20
  UNION ALL
  SELECT 'cet4', 'single', '四级写作中，主题句通常起什么作用？',
         JSON_ARRAY('A. 引出段落中心', 'B. 替代所有论据', 'C. 放在参考文献中', 'D. 只用于结尾署名'),
         'A', '主题句用于概括段落中心，后续句子围绕它展开说明。', '基础', 'https://examcoo.com/editor/do/view/id/125926', 30
  UNION ALL
  SELECT 'cet4', 'single', '短语 “due to” 最常见的意思是？',
         JSON_ARRAY('A. 因为，由于', 'B. 尽管', 'C. 除了', 'D. 代替'),
         'A', 'due to 表示原因，常译为“由于”。', '基础', 'https://examcoo.com/editor/do/view/id/125926', 40
  UNION ALL
  SELECT 'cet4', 'single', '翻译 “保护环境” 较合适的表达是？',
         JSON_ARRAY('A. protect the environment', 'B. produce the environment', 'C. predict the environment', 'D. prevent the environment'),
         'A', 'protect the environment 是常用表达，意为保护环境。', '基础', 'https://examcoo.com/editor/do/view/id/125926', 50
  UNION ALL
  SELECT 'cet4', 'single', 'CET4 听力做题时，预读选项的主要目的是什么？',
         JSON_ARRAY('A. 预测话题和考点', 'B. 跳过原文', 'C. 背诵所有选项', 'D. 避免听关键词'),
         'A', '预读选项可以帮助预测场景、人物关系和问题方向。', '应用', 'https://examcoo.com/editor/do/view/id/125926', 60

  UNION ALL
  SELECT 'cet6', 'single', 'CET6 阅读中，作者态度题通常需要重点关注什么？',
         JSON_ARRAY('A. 情感色彩词和评价性表达', 'B. 每个单词的音标', 'C. 标点数量', 'D. 文章字体'),
         'A', '作者态度常通过褒贬色彩词、让步转折和结论句体现。', '综合', 'https://examcoo.com/editor/do/view/id/3077621', 10
  UNION ALL
  SELECT 'cet6', 'single', 'Which phrase means “in spite of”?',
         JSON_ARRAY('A. despite', 'B. because of', 'C. instead of', 'D. according to'),
         'A', 'despite 表示“尽管”，与 in spite of 意义接近。', '基础', 'https://examcoo.com/editor/do/view/id/3077621', 20
  UNION ALL
  SELECT 'cet6', 'single', '六级翻译中，处理长句较合理的方法是？',
         JSON_ARRAY('A. 先分析主干再补充修饰成分', 'B. 完全逐字翻译', 'C. 删除所有从句', 'D. 只翻译名词'),
         'A', '长句翻译应先找主谓宾等主干，再处理定语、状语和从句。', '综合', 'https://examcoo.com/editor/do/view/id/3077621', 30
  UNION ALL
  SELECT 'cet6', 'single', '“sustainable development” 常译为？',
         JSON_ARRAY('A. 可持续发展', 'B. 快速消费', 'C. 临时计划', 'D. 传统生产'),
         'A', 'sustainable development 是“可持续发展”的固定表达。', '基础', 'https://examcoo.com/editor/do/view/id/3077621', 40
  UNION ALL
  SELECT 'cet6', 'single', '六级写作中，例证法的作用主要是？',
         JSON_ARRAY('A. 支撑观点并增强说服力', 'B. 代替文章标题', 'C. 减少逻辑关系', 'D. 只用于列单词'),
         'A', '例证可以把抽象观点具体化，使论证更充分。', '应用', 'https://examcoo.com/editor/do/view/id/3077621', 50
  UNION ALL
  SELECT 'cet6', 'single', 'Which word is closest to “allocate”?',
         JSON_ARRAY('A. distribute', 'B. hide', 'C. destroy', 'D. ignore'),
         'A', 'allocate 表示分配、配置，与 distribute 接近。', '综合', 'https://examcoo.com/editor/do/view/id/3077621', 60

  UNION ALL
  SELECT 'marxism', 'single', '马克思主义哲学认为，世界的本原是什么？',
         JSON_ARRAY('A. 物质', 'B. 感觉', 'C. 观念', 'D. 个人意志'),
         'A', '辩证唯物主义认为世界统一于物质。', '基础', 'https://examcoo.com/editor/do/view/id/152441', 10
  UNION ALL
  SELECT 'marxism', 'single', '实践在认识论中的地位是？',
         JSON_ARRAY('A. 认识的来源和检验真理的标准', 'B. 与认识无关', 'C. 只是主观想象', 'D. 只能证明错误'),
         'A', '实践是认识的来源、动力、目的，也是检验真理的唯一标准。', '基础', 'https://examcoo.com/editor/do/view/id/152441', 20
  UNION ALL
  SELECT 'marxism', 'single', '矛盾的基本属性包括？',
         JSON_ARRAY('A. 同一性和斗争性', 'B. 偶然性和幻想性', 'C. 静止性和封闭性', 'D. 主观性和任意性'),
         'A', '矛盾具有同一性和斗争性，两者共同推动事物发展。', '综合', 'https://examcoo.com/editor/do/view/id/152441', 30
  UNION ALL
  SELECT 'marxism', 'single', '商品价值量主要由什么决定？',
         JSON_ARRAY('A. 社会必要劳动时间', 'B. 商品颜色', 'C. 购买者心情', 'D. 广告篇幅'),
         'A', '马克思主义政治经济学认为商品价值量由社会必要劳动时间决定。', '基础', 'https://examcoo.com/editor/do/view/id/152441', 40
  UNION ALL
  SELECT 'marxism', 'single', '历史唯物主义认为，社会发展的最终决定力量是？',
         JSON_ARRAY('A. 生产力', 'B. 偶像崇拜', 'C. 个人偏好', 'D. 自然语言'),
         'A', '生产力是社会发展的最终决定力量，生产关系应适应生产力发展。', '综合', 'https://examcoo.com/editor/do/view/id/152441', 50
  UNION ALL
  SELECT 'marxism', 'single', '真理的客观性是指什么？',
         JSON_ARRAY('A. 真理内容不以人的意志为转移', 'B. 真理只属于个人感受', 'C. 真理不能被检验', 'D. 真理没有条件'),
         'A', '真理内容反映客观对象及其规律，因此具有客观性。', '综合', 'https://examcoo.com/editor/do/view/id/152441', 60

  UNION ALL
  SELECT 'reform-opening', 'single', '中国改革开放的历史起点通常认为是哪次会议？',
         JSON_ARRAY('A. 十一届三中全会', 'B. 遵义会议', 'C. 中共一大', 'D. 瓦窑堡会议'),
         'A', '1978 年召开的十一届三中全会开启了改革开放和社会主义现代化建设新时期。', '基础', 'https://examcoo.com/editor/do/view/id/819828', 10
  UNION ALL
  SELECT 'reform-opening', 'single', '改革开放首先在农村突破的重要制度创新是？',
         JSON_ARRAY('A. 家庭联产承包责任制', 'B. 科举制', 'C. 分封制', 'D. 闭关政策'),
         'A', '家庭联产承包责任制极大调动了农民生产积极性。', '基础', 'https://examcoo.com/editor/do/view/id/819828', 20
  UNION ALL
  SELECT 'reform-opening', 'single', '设立经济特区的重要目的之一是？',
         JSON_ARRAY('A. 探索对外开放和经济体制改革经验', 'B. 完全隔绝国际交流', 'C. 取消市场机制', 'D. 停止工业发展'),
         'A', '经济特区承担改革开放试验田作用。', '综合', 'https://examcoo.com/editor/do/view/id/819828', 30
  UNION ALL
  SELECT 'reform-opening', 'single', '改革开放的鲜明特征是？',
         JSON_ARRAY('A. 解放和发展社会生产力', 'B. 固守封闭状态', 'C. 否定现代化建设', 'D. 排斥科技进步'),
         'A', '改革开放的根本目的在于解放和发展社会生产力、改善人民生活。', '基础', 'https://examcoo.com/editor/do/view/id/819828', 40
  UNION ALL
  SELECT 'reform-opening', 'single', '社会主义市场经济体制目标的确立与哪次会议关系密切？',
         JSON_ARRAY('A. 中共十四大', 'B. 中共二大', 'C. 八七会议', 'D. 古田会议'),
         'A', '中共十四大明确提出建立社会主义市场经济体制的改革目标。', '综合', 'https://examcoo.com/editor/do/view/id/819828', 50
  UNION ALL
  SELECT 'reform-opening', 'single', '改革开放以来我国发展的重要经验之一是？',
         JSON_ARRAY('A. 坚持党的领导和中国特色社会主义道路', 'B. 放弃制度建设', 'C. 脱离人民群众', 'D. 拒绝开放合作'),
         'A', '坚持党的领导和中国特色社会主义道路是改革开放取得成就的重要保证。', '综合', 'https://examcoo.com/editor/do/view/id/819828', 60

  UNION ALL
  SELECT 'teacher-cert', 'single', '教师职业道德中，“关爱学生”要求教师做到什么？',
         JSON_ARRAY('A. 尊重学生人格并公平对待学生', 'B. 只关注成绩最好的学生', 'C. 随意公开学生隐私', 'D. 以惩罚代替教育'),
         'A', '关爱学生要求尊重、理解、信任学生，促进学生全面发展。', '基础', 'https://examcoo.com/editor/do/view/id/113521', 10
  UNION ALL
  SELECT 'teacher-cert', 'single', '教学设计中，教学目标应尽量具有什么特点？',
         JSON_ARRAY('A. 明确、可观察、可评价', 'B. 越模糊越好', 'C. 与学生无关', 'D. 只写教师活动'),
         'A', '明确可评价的目标有助于组织教学和检测学习效果。', '基础', 'https://examcoo.com/editor/do/view/id/113521', 20
  UNION ALL
  SELECT 'teacher-cert', 'single', '班级管理中，建立规则的合理方式是？',
         JSON_ARRAY('A. 民主讨论并明确执行', 'B. 临时随意改变', 'C. 不解释直接处罚', 'D. 只由个别学生决定'),
         'A', '规则应清晰、稳定并让学生理解其意义。', '应用', 'https://examcoo.com/editor/do/view/id/113521', 30
  UNION ALL
  SELECT 'teacher-cert', 'single', '学生发展具有差异性，这要求教师？',
         JSON_ARRAY('A. 因材施教', 'B. 使用完全相同的要求否定差异', 'C. 只看考试排名', 'D. 放弃学习困难学生'),
         'A', '尊重个体差异，采用有针对性的教学支持。', '综合', 'https://examcoo.com/editor/do/view/id/113521', 40
  UNION ALL
  SELECT 'teacher-cert', 'single', '形成性评价主要发生在教学的哪个阶段？',
         JSON_ARRAY('A. 教学过程中', 'B. 只在毕业后', 'C. 只在招生前', 'D. 与教学无关'),
         'A', '形成性评价服务于学习过程改进。', '基础', 'https://examcoo.com/editor/do/view/id/113521', 50
  UNION ALL
  SELECT 'teacher-cert', 'single', '课堂提问后适当等待的主要目的是什么？',
         JSON_ARRAY('A. 给学生思考时间', 'B. 避免学生回答', 'C. 缩短课堂互动', 'D. 转移教学目标'),
         'A', '等待时间能提高回答质量并鼓励更多学生参与。', '应用', 'https://examcoo.com/editor/do/view/id/113521', 60

  UNION ALL
  SELECT 'counselor', 'single', '心理咨询中，保密原则的基本含义是？',
         JSON_ARRAY('A. 保护来访者隐私，法律和安全例外除外', 'B. 可以任意公开咨询内容', 'C. 只对熟人保密', 'D. 不需要告知来访者'),
         'A', '保密是咨询伦理核心，但涉及严重伤害风险等情况时存在例外。', '基础', 'https://examcoo.com/editor/do/view/id/151921', 10
  UNION ALL
  SELECT 'counselor', 'single', '共情在心理咨询中的作用主要是？',
         JSON_ARRAY('A. 理解并回应来访者的感受和处境', 'B. 替来访者做所有决定', 'C. 批评来访者', 'D. 忽视情绪体验'),
         'A', '共情有助于建立咨询关系并促进来访者表达。', '基础', 'https://examcoo.com/editor/do/view/id/151921', 20
  UNION ALL
  SELECT 'counselor', 'single', '心理测验使用时应注意什么？',
         JSON_ARRAY('A. 遵守标准化程序并结合专业解释', 'B. 单凭一次测验给终身标签', 'C. 随意改动题目', 'D. 不需要说明目的'),
         'A', '测验结果需要结合背景资料和专业判断，不能机械定论。', '综合', 'https://examcoo.com/editor/do/view/id/151921', 30
  UNION ALL
  SELECT 'counselor', 'single', '咨询初期建立关系时，较重要的态度是？',
         JSON_ARRAY('A. 尊重、真诚和接纳', 'B. 指责和命令', 'C. 冷漠和回避', 'D. 讽刺和否定'),
         'A', '良好咨询关系依赖尊重、真诚、共情和积极关注。', '基础', 'https://examcoo.com/editor/do/view/id/151921', 40
  UNION ALL
  SELECT 'counselor', 'single', '当来访者出现明确自伤风险时，咨询师应优先考虑什么？',
         JSON_ARRAY('A. 安全评估和危机干预', 'B. 完全忽略', 'C. 立即结束关系且不记录', 'D. 只讨论学习成绩'),
         'A', '危机情境应优先保护生命安全，并按伦理和法律规范处理。', '应用', 'https://examcoo.com/editor/do/view/id/151921', 50
  UNION ALL
  SELECT 'counselor', 'single', '开放式提问的主要特点是？',
         JSON_ARRAY('A. 鼓励来访者展开表达', 'B. 只能回答“是”或“否”', 'C. 用于打断来访者', 'D. 不允许描述感受'),
         'A', '开放式问题能促进来访者更充分地表达经历、想法和情绪。', '应用', 'https://examcoo.com/editor/do/view/id/151921', 60

  UNION ALL
  SELECT 'accounting-basic', 'single', '会计恒等式通常表示为？',
         JSON_ARRAY('A. 资产 = 负债 + 所有者权益', 'B. 收入 = 资产 + 费用', 'C. 负债 = 收入 + 利润', 'D. 资产 = 成本 - 权益'),
         'A', '资产、负债和所有者权益构成基本会计等式。', '基础', 'https://examcoo.com/editor/do/view/id/2246598', 10
  UNION ALL
  SELECT 'accounting-basic', 'single', '会计凭证的主要作用是？',
         JSON_ARRAY('A. 记录和证明经济业务发生', 'B. 替代所有财务报表', 'C. 删除账簿', 'D. 改变企业性质'),
         'A', '会计凭证是登记账簿的重要依据。', '基础', 'https://examcoo.com/editor/do/view/id/2246598', 20
  UNION ALL
  SELECT 'accounting-basic', 'single', '借贷记账法中，账户哪一方增加取决于什么？',
         JSON_ARRAY('A. 账户性质', 'B. 书写位置', 'C. 凭证颜色', 'D. 会计人员年龄'),
         'A', '不同性质账户的增加方向不同，应结合账户类别判断。', '综合', 'https://examcoo.com/editor/do/view/id/2246598', 30
  UNION ALL
  SELECT 'accounting-basic', 'single', '下列哪项通常属于流动资产？',
         JSON_ARRAY('A. 库存现金', 'B. 长期借款', 'C. 实收资本', 'D. 应付债券'),
         'A', '库存现金属于流动资产；长期借款和应付债券属于负债。', '基础', 'https://examcoo.com/editor/do/view/id/2246598', 40
  UNION ALL
  SELECT 'accounting-basic', 'single', '利润表主要反映企业什么情况？',
         JSON_ARRAY('A. 一定期间的经营成果', 'B. 某一时点的资产结构', 'C. 员工花名册', 'D. 固定资产位置'),
         'A', '利润表反映一定会计期间收入、费用和利润情况。', '基础', 'https://examcoo.com/editor/do/view/id/2246598', 50
  UNION ALL
  SELECT 'accounting-basic', 'single', '期末结账前进行账实核对的目的主要是？',
         JSON_ARRAY('A. 保证账面记录与实际财产相符', 'B. 增加凭证数量', 'C. 删除所有明细账', 'D. 改变会计期间'),
         'A', '账实核对有助于发现盘盈、盘亏和记录差错。', '应用', 'https://examcoo.com/editor/do/view/id/2246598', 60
) q ON q.set_code = s.set_code;
