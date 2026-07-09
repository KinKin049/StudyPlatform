export const difficultyLabel = {
  EASY: '简单 Easy',
  MEDIUM: '中等 Medium',
  HARD: '困难 Hard',
}

export const difficultyOptions = [
  { value: 'EASY', label: '简单', labelEn: 'Easy' },
  { value: 'MEDIUM', label: '中等', labelEn: 'Medium' },
  { value: 'HARD', label: '困难', labelEn: 'Hard' },
]

export const statementLanguageOptions = [
  { value: 'zh', label: '中文题面', labelEn: 'Chinese' },
  { value: 'en', label: '英文题面', labelEn: 'English' },
]

export const algorithmCategoryOptions = [
  { value: 'beginner', label: '入门', labelEn: 'Beginner' },
  { value: 'math', label: '数学', labelEn: 'Math' },
  { value: 'number-theory', label: '数论', labelEn: 'Number Theory' },
  { value: 'array', label: '数组', labelEn: 'Array' },
  { value: 'string', label: '字符串', labelEn: 'String' },
  { value: 'stack', label: '栈', labelEn: 'Stack' },
  { value: 'hash-table', label: '哈希表', labelEn: 'Hash Table' },
  { value: 'sort', label: '排序', labelEn: 'Sort' },
  { value: 'interval', label: '区间', labelEn: 'Interval' },
  { value: 'dp', label: '动态规划', labelEn: 'Dynamic Programming' },
  { value: 'binary-search', label: '二分', labelEn: 'Binary Search' },
  { value: 'graph', label: '图论', labelEn: 'Graph' },
  { value: 'bfs', label: '广度优先搜索', labelEn: 'BFS' },
  { value: 'grid', label: '网格', labelEn: 'Grid' },
  { value: 'sieve', label: '筛法', labelEn: 'Sieve' },
  { value: 'prefix', label: '前缀', labelEn: 'Prefix' },
]

export const quickAlgorithmCategories = ['beginner', 'array', 'string', 'dp', 'graph', 'bfs']

export function formatAlgorithmCategory(value) {
  const item = algorithmCategoryOptions.find((option) => option.value === value)
  return item ? `${item.label} ${item.labelEn}` : value
}

export function formatDifficulty(value) {
  return difficultyLabel[value] || value
}
