INSERT INTO course_question_bank_sets
  (category_id, set_code, title, subtitle, description, cover_url, cover_file_path, difficulty_label,
   status_label, source_name, source_url, source_refs, route_path, sort_order)
SELECT c.id, data.set_code, data.title, data.subtitle, data.description, data.cover_url, data.cover_file_path,
       data.difficulty_label, data.status_label, data.source_name, data.source_url, data.source_refs,
       data.route_path, data.sort_order
FROM course_question_bank_categories c
JOIN (
  SELECT 'computer' AS category_code, 'java' AS set_code, 'Java题库' AS title,
         'Java程序设计' AS subtitle,
         '围绕 Java 基础语法、面向对象、异常处理、集合框架和线程基础建立练习题库。' AS description,
         'https://mooc-image.nosdn.127.net/3e185a4f596f4a54b4ce8727d33faa5d.jpg' AS cover_url,
         'storage/online_course/计算机/1473176181_1476745579.jpg' AS cover_file_path,
         '基础到综合' AS difficulty_label,
         '已接入' AS status_label,
         'Examcoo Java 题库入口' AS source_name,
         'https://examcoo.com/editor/do/view/id/198663' AS source_url,
         JSON_ARRAY('https://examcoo.com/editor/do/view/id/198663') AS source_refs,
         '/academy/question-bank/courses/java' AS route_path,
         20 AS sort_order
  UNION ALL
  SELECT 'computer', 'python', 'Python题库', 'Python程序设计',
         '围绕 Python 变量、容器、函数、模块、异常和面向对象基础整理中文练习题。',
         'https://mooc-image.nosdn.127.net/3e185a4f596f4a54b4ce8727d33faa5d.jpg',
         'storage/online_course/计算机/1473176181_1476745579.jpg',
         '基础到综合',
         '已接入',
         'MCQSS Python MCQ',
         'https://mcqss.com/python-mcq',
         JSON_ARRAY('https://mcqss.com/python-mcq'),
         '/academy/question-bank/courses/python',
         30
  UNION ALL
  SELECT 'computer', 'database', '数据库题库', 'Redis 与 NoSQL 数据库',
         '围绕 Redis 数据结构、持久化、事务、发布订阅和 NoSQL 使用场景整理中文练习题。',
         'https://edu-image.nosdn.127.net/66030EC707D1D621F63C29AB2D92A04B.jpg?imageView&thumbnail=510y288&quality=100',
         'storage/online_course/计算机/1207042802_1476755441.jpg',
         '基础到综合',
         '已接入',
         'Fatskills Redis Practice Test',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         JSON_ARRAY('https://www.fatskills.com/databases/quiz/redis-practice-test-nosql'),
         '/academy/question-bank/courses/database',
         40
  UNION ALL
  SELECT 'computer', 'data-structure', '数据结构题库', '数据结构与算法',
         '围绕线性表、栈、队列、树、图、排序和查找整理中文练习题。',
         'https://mooc-image.nosdn.127.net/0ee67821ee8f4c1183e27c3541d833ae.jpg',
         'storage/online_course/理学/1474462161_1476202443.jpg',
         '基础到综合',
         '已接入',
         'MCQSS 数据结构与算法 MCQ',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         JSON_ARRAY('https://mcqss.com/zh/data-structures-and-algorithms-mcq'),
         '/academy/question-bank/courses/data-structure',
         50
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
  SELECT 'java' AS set_code, 'single' AS question_type,
         '在 Java 中，哪个关键字用于定义一个类继承另一个类？' AS stem,
         JSON_ARRAY('A. implements', 'B. extends', 'C. import', 'D. package') AS options_json,
         'B' AS answer,
         'extends 用于类继承；implements 用于实现接口。' AS explanation,
         '基础' AS difficulty_label,
         'https://examcoo.com/editor/do/view/id/198663' AS source_url,
         10 AS sort_order
  UNION ALL
  SELECT 'java', 'single',
         'Java 程序的入口方法通常写成哪一种形式？',
         JSON_ARRAY('A. public static void main(String[] args)', 'B. public void start(String[] args)', 'C. static int main()', 'D. private static main(String args)'),
         'A',
         'Java 命令行程序通常从 public static void main(String[] args) 方法开始执行。',
         '基础',
         'https://examcoo.com/editor/do/view/id/198663',
         20
  UNION ALL
  SELECT 'java', 'single',
         '以下哪个类型属于 Java 的基本数据类型？',
         JSON_ARRAY('A. String', 'B. Integer', 'C. boolean', 'D. ArrayList'),
         'C',
         'boolean 是基本数据类型；String、Integer、ArrayList 都是引用类型。',
         '基础',
         'https://examcoo.com/editor/do/view/id/198663',
         30
  UNION ALL
  SELECT 'java', 'single',
         '关于 Java 异常处理，哪一个语句用于捕获异常？',
         JSON_ARRAY('A. throw', 'B. catch', 'C. finally', 'D. throws'),
         'B',
         'catch 块用于捕获并处理 try 块中抛出的异常。',
         '基础',
         'https://examcoo.com/editor/do/view/id/198663',
         40
  UNION ALL
  SELECT 'java', 'single',
         'ArrayList 所在的常用包是？',
         JSON_ARRAY('A. java.io', 'B. java.net', 'C. java.util', 'D. java.sql'),
         'C',
         'ArrayList 是 Java 集合框架的一部分，位于 java.util 包。',
         '基础',
         'https://examcoo.com/editor/do/view/id/198663',
         50
  UNION ALL
  SELECT 'java', 'single',
         '在 Java 中，接口中的方法默认更接近哪种访问级别？',
         JSON_ARRAY('A. private', 'B. protected', 'C. public', 'D. package-private'),
         'C',
         '接口中的抽象方法默认是 public abstract；实现时不能降低访问权限。',
         '综合',
         'https://examcoo.com/editor/do/view/id/198663',
         60
  UNION ALL
  SELECT 'java', 'single',
         '以下哪项最符合封装的含义？',
         JSON_ARRAY('A. 把数据和操作数据的方法组织在类中并限制直接访问', 'B. 一个类只能有一个对象', 'C. 所有方法必须是静态方法', 'D. 子类必须重写父类所有方法'),
         'A',
         '封装强调把状态和行为组合到对象内部，并通过访问控制保护内部状态。',
         '综合',
         'https://examcoo.com/editor/do/view/id/198663',
         70
  UNION ALL
  SELECT 'java', 'single',
         'Java 中创建线程的一种常见方式是？',
         JSON_ARRAY('A. 继承 Thread 类', 'B. 继承 String 类', 'C. 实现 Map 接口', 'D. 调用 System.gc()'),
         'A',
         '创建线程可以继承 Thread，也可以实现 Runnable 或 Callable。',
         '应用',
         'https://examcoo.com/editor/do/view/id/198663',
         80

  UNION ALL
  SELECT 'python', 'single',
         'Python 中哪个关键字用于定义函数？',
         JSON_ARRAY('A. func', 'B. define', 'C. def', 'D. lambda'),
         'C',
         'def 用于定义具名函数；lambda 用于创建匿名函数表达式。',
         '基础',
         'https://mcqss.com/python-mcq',
         10
  UNION ALL
  SELECT 'python', 'single',
         '下列哪一种是 Python 的可变序列类型？',
         JSON_ARRAY('A. tuple', 'B. str', 'C. list', 'D. frozenset'),
         'C',
         'list 可以增删改元素；tuple、str、frozenset 创建后不可变。',
         '基础',
         'https://mcqss.com/python-mcq',
         20
  UNION ALL
  SELECT 'python', 'single',
         '表达式 len([1, 2, 3]) 的结果是？',
         JSON_ARRAY('A. 2', 'B. 3', 'C. 4', 'D. 报错'),
         'B',
         'len 返回容器中元素的个数，该列表包含 3 个元素。',
         '基础',
         'https://mcqss.com/python-mcq',
         30
  UNION ALL
  SELECT 'python', 'single',
         'Python 中用于捕获异常的关键字组合是？',
         JSON_ARRAY('A. try / except', 'B. do / catch', 'C. if / error', 'D. switch / case'),
         'A',
         'Python 使用 try 包裹可能出错的代码，并用 except 捕获异常。',
         '基础',
         'https://mcqss.com/python-mcq',
         40
  UNION ALL
  SELECT 'python', 'single',
         '以下哪个方法通常用于把字符串两端的空白字符去掉？',
         JSON_ARRAY('A. split()', 'B. strip()', 'C. replace()', 'D. join()'),
         'B',
         'strip() 返回去除首尾空白后的字符串；split、replace、join 用途不同。',
         '基础',
         'https://mcqss.com/python-mcq',
         50
  UNION ALL
  SELECT 'python', 'single',
         '关于 Python 字典 dict，哪项说法正确？',
         JSON_ARRAY('A. 只能用整数作为键', 'B. 通过键值对存储数据', 'C. 不支持嵌套结构', 'D. 元素一定按字母排序'),
         'B',
         'dict 使用键值对存储数据，键必须是可哈希对象。',
         '综合',
         'https://mcqss.com/python-mcq',
         60
  UNION ALL
  SELECT 'python', 'single',
         '在 Python 类中，实例方法的第一个参数通常命名为？',
         JSON_ARRAY('A. this', 'B. self', 'C. object', 'D. instance'),
         'B',
         'self 是约定俗成的实例引用名称，虽然不是保留关键字，但应按规范使用。',
         '综合',
         'https://mcqss.com/python-mcq',
         70
  UNION ALL
  SELECT 'python', 'single',
         '列表推导式 [x * x for x in range(3)] 的结果是？',
         JSON_ARRAY('A. [0, 1, 4]', 'B. [1, 4, 9]', 'C. [0, 1, 2]', 'D. [3, 6, 9]'),
         'A',
         'range(3) 产生 0、1、2，对每个数求平方得到 [0, 1, 4]。',
         '应用',
         'https://mcqss.com/python-mcq',
         80

  UNION ALL
  SELECT 'database', 'single',
         'Redis 最典型的数据存储方式是？',
         JSON_ARRAY('A. 键值存储', 'B. 纯关系表存储', 'C. 文件系统目录存储', 'D. 二维电子表格存储'),
         'A',
         'Redis 是内存型键值数据库，同时支持多种值类型。',
         '基础',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         10
  UNION ALL
  SELECT 'database', 'single',
         '以下哪种 Redis 数据类型适合保存不重复的成员集合？',
         JSON_ARRAY('A. String', 'B. Set', 'C. List', 'D. Hash'),
         'B',
         'Set 保存无序且不重复的成员，适合去重、交集、并集等操作。',
         '基础',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         20
  UNION ALL
  SELECT 'database', 'single',
         'Redis 中 EXPIRE 命令的主要作用是？',
         JSON_ARRAY('A. 设置键的过期时间', 'B. 删除所有数据库', 'C. 创建索引', 'D. 开启事务'),
         'A',
         'EXPIRE 可以为键设置 TTL，到期后键会被 Redis 删除。',
         '基础',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         30
  UNION ALL
  SELECT 'database', 'single',
         'Redis 持久化机制 RDB 的主要特点是？',
         JSON_ARRAY('A. 定期生成数据快照', 'B. 只保存 SQL 语句', 'C. 永远不落盘', 'D. 只能用于集群模式'),
         'A',
         'RDB 会在指定条件下生成快照文件，适合备份和快速恢复。',
         '综合',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         40
  UNION ALL
  SELECT 'database', 'single',
         'Redis 中 MULTI、EXEC 通常用于实现什么功能？',
         JSON_ARRAY('A. 事务', 'B. 复制', 'C. 分片', 'D. 订阅频道'),
         'A',
         'MULTI 开启事务队列，EXEC 执行队列中的命令。',
         '综合',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         50
  UNION ALL
  SELECT 'database', 'single',
         '以下哪项更符合 NoSQL 数据库的常见特点？',
         JSON_ARRAY('A. 数据模型通常更灵活', 'B. 必须使用固定表结构', 'C. 只能执行 SQL 查询', 'D. 不能分布式部署'),
         'A',
         'NoSQL 通常强调灵活的数据模型和横向扩展能力。',
         '基础',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         60
  UNION ALL
  SELECT 'database', 'single',
         'Redis 发布订阅模式中，哪个命令用于订阅频道？',
         JSON_ARRAY('A. SUBSCRIBE', 'B. LISTEN', 'C. WATCH', 'D. CONNECT'),
         'A',
         'SUBSCRIBE 用于订阅一个或多个频道；PUBLISH 用于向频道发送消息。',
         '应用',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         70
  UNION ALL
  SELECT 'database', 'single',
         '在缓存场景中，为热点数据设置过期时间的主要目的之一是？',
         JSON_ARRAY('A. 控制数据生命周期并减少陈旧数据', 'B. 禁止任何并发访问', 'C. 自动创建关系表', 'D. 关闭内存管理'),
         'A',
         '合理的过期时间可以降低脏数据风险，也能帮助释放内存。',
         '应用',
         'https://www.fatskills.com/databases/quiz/redis-practice-test-nosql',
         80

  UNION ALL
  SELECT 'data-structure', 'single',
         '栈这种数据结构通常遵循哪种访问原则？',
         JSON_ARRAY('A. 先进先出', 'B. 后进先出', 'C. 随机优先', 'D. 按键排序'),
         'B',
         '栈遵循后进先出原则，最后压入的元素最先弹出。',
         '基础',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         10
  UNION ALL
  SELECT 'data-structure', 'single',
         '队列这种数据结构通常遵循哪种访问原则？',
         JSON_ARRAY('A. 后进先出', 'B. 先进先出', 'C. 按权值优先', 'D. 随机访问'),
         'B',
         '普通队列遵循先进先出原则，先进入队列的元素先出队。',
         '基础',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         20
  UNION ALL
  SELECT 'data-structure', 'single',
         '在二叉搜索树中，左子树节点的值通常满足什么关系？',
         JSON_ARRAY('A. 大于根节点', 'B. 小于根节点', 'C. 等于根节点', 'D. 与根节点无关'),
         'B',
         '典型二叉搜索树要求左子树值小于根节点，右子树值大于根节点。',
         '基础',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         30
  UNION ALL
  SELECT 'data-structure', 'single',
         '对长度为 n 的无序数组进行线性查找，最坏时间复杂度是？',
         JSON_ARRAY('A. O(1)', 'B. O(log n)', 'C. O(n)', 'D. O(n log n)'),
         'C',
         '最坏情况下需要检查数组中的每一个元素，因此复杂度为 O(n)。',
         '基础',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         40
  UNION ALL
  SELECT 'data-structure', 'single',
         '以下哪种排序算法平均时间复杂度通常为 O(n log n)？',
         JSON_ARRAY('A. 冒泡排序', 'B. 快速排序', 'C. 选择排序', 'D. 插入排序'),
         'B',
         '快速排序平均复杂度通常为 O(n log n)，但最坏情况下可能退化到 O(n²)。',
         '综合',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         50
  UNION ALL
  SELECT 'data-structure', 'single',
         '图的广度优先遍历通常借助哪种辅助结构实现？',
         JSON_ARRAY('A. 队列', 'B. 栈', 'C. 哈希表', 'D. 堆'),
         'A',
         'BFS 按层扩展顶点，通常使用队列保存待访问节点。',
         '综合',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         60
  UNION ALL
  SELECT 'data-structure', 'single',
         '堆结构常用于实现哪一种抽象数据类型？',
         JSON_ARRAY('A. 优先队列', 'B. 普通队列', 'C. 顺序表', 'D. 邻接矩阵'),
         'A',
         '堆可以高效获得最大或最小优先级元素，常用于实现优先队列。',
         '综合',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         70
  UNION ALL
  SELECT 'data-structure', 'single',
         '哈希表查找接近 O(1) 的前提通常包括什么？',
         JSON_ARRAY('A. 哈希函数分布较均匀且冲突可控', 'B. 所有键必须递增', 'C. 数据必须已经排序', 'D. 只能保存整数'),
         'A',
         '哈希表性能依赖哈希函数和冲突处理，冲突过多会降低效率。',
         '应用',
         'https://mcqss.com/zh/data-structures-and-algorithms-mcq',
         80
) q ON q.set_code = s.set_code
WHERE NOT EXISTS (
  SELECT 1
  FROM course_question_bank_questions existing
  WHERE existing.set_id = s.id AND existing.sort_order = q.sort_order
);
