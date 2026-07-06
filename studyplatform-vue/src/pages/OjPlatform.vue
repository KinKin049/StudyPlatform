<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useLearningTimeTracker } from '../composables/useLearningTimeTracker'
import {
  createSubmission,
  getProblem,
  getSubmission,
  listProblems,
  listSubmissionCases,
} from '../oj/api'

useLearningTimeTracker({
  moduleType: 'oj',
  targetCode: 'lab-oj',
  targetTitle: 'OJ 在线判题',
})

const problems = ref([])
const selectedProblem = ref(null)
const submissions = ref([])
const selectedSubmissionCases = ref([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const activeTab = ref('statement')
const sourceCode = ref('')
const language = ref('cpp')
const searchKeyword = ref('')
const filterPanelOpen = ref(false)
const selectedAlgorithmCategories = ref([])
const selectedDifficultyCategories = ref([])
const selectedStatementLanguages = ref([])
let searchTimer = 0

const difficultyLabel = {
  EASY: '简单 Easy',
  MEDIUM: '中等 Medium',
  HARD: '困难 Hard',
}

const difficultyOptions = [
  { value: 'EASY', label: '简单', labelEn: 'Easy' },
  { value: 'MEDIUM', label: '中等', labelEn: 'Medium' },
  { value: 'HARD', label: '困难', labelEn: 'Hard' },
]

const statementLanguageOptions = [
  { value: 'zh', label: '中文题面', labelEn: 'Chinese' },
  { value: 'en', label: '英文题面', labelEn: 'English' },
]

const algorithmCategoryOptions = [
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

const quickAlgorithmCategories = ['beginner', 'array', 'string', 'dp', 'graph', 'bfs']

const statusLabel = {
  PENDING: '等待中',
  JUDGING: '判题中',
  ACCEPTED: '通过',
  WRONG_ANSWER: '答案错误',
  TIME_LIMIT_EXCEEDED: '超时',
  MEMORY_LIMIT_EXCEEDED: '内存超限',
  RUNTIME_ERROR: '运行错误',
  COMPILE_ERROR: '编译错误',
  SYSTEM_ERROR: '系统错误',
}

const parsedSamples = computed(() => {
  if (!selectedProblem.value?.samples) return []
  try {
    return JSON.parse(selectedProblem.value.samples)
  } catch {
    return []
  }
})

const parsedTags = computed(() => {
  if (!selectedProblem.value?.tags) return []
  try {
    return JSON.parse(selectedProblem.value.tags)
  } catch {
    return []
  }
})

const latestSubmission = computed(() => submissions.value[0] || null)
const activeFilterCount = computed(
  () =>
    selectedAlgorithmCategories.value.length +
    selectedDifficultyCategories.value.length +
    selectedStatementLanguages.value.length,
)
const quickCategoryOptions = computed(() =>
  algorithmCategoryOptions.filter((item) => quickAlgorithmCategories.includes(item.value)),
)

watch(language, () => {
  sourceCode.value = defaultSourceFor(selectedProblem.value?.slug, language.value)
})

watch(
  [
    searchKeyword,
    () => selectedAlgorithmCategories.value.slice(),
    () => selectedDifficultyCategories.value.slice(),
    () => selectedStatementLanguages.value.slice(),
  ],
  () => {
    window.clearTimeout(searchTimer)
    searchTimer = window.setTimeout(() => {
      loadProblems()
    }, 300)
  },
)

async function loadProblems() {
  loading.value = true
  errorMessage.value = ''
  try {
    problems.value = await listProblems(searchKeyword.value, {
      tags: selectedAlgorithmCategories.value,
      difficulties: selectedDifficultyCategories.value,
      languages: selectedStatementLanguages.value,
    })
    if (problems.value.length > 0) {
      await selectProblem(problems.value[0])
    } else {
      selectedProblem.value = null
      selectedSubmissionCases.value = []
    }
  } catch (error) {
    errorMessage.value = formatError(error)
  } finally {
    loading.value = false
  }
}

async function selectProblem(problem) {
  errorMessage.value = ''
  activeTab.value = 'statement'
  selectedSubmissionCases.value = []
  selectedProblem.value = await getProblem(problem.id)
  sourceCode.value = defaultSourceFor(selectedProblem.value.slug, language.value)
}

async function submitAnswer() {
  if (!selectedProblem.value) return
  submitting.value = true
  errorMessage.value = ''
  selectedSubmissionCases.value = []
  try {
    const submission = await createSubmission({
      problemId: selectedProblem.value.id,
      userId: null,
      language: language.value,
      sourceCode: sourceCode.value,
    })
    submissions.value.unshift(submission)
    activeTab.value = 'result'
    await pollSubmission(submission.id)
  } catch (error) {
    errorMessage.value = formatError(error)
  } finally {
    submitting.value = false
  }
}

async function pollSubmission(id) {
  for (let index = 0; index < 10; index += 1) {
    const submission = await getSubmission(id)
    submissions.value = [submission, ...submissions.value.filter((item) => item.id !== id)]
    if (!['PENDING', 'JUDGING'].includes(submission.status)) {
      selectedSubmissionCases.value = await listSubmissionCases(id)
      return
    }
    await delay(800)
  }
}

async function showSubmissionCases(submission) {
  selectedSubmissionCases.value = await listSubmissionCases(submission.id)
  activeTab.value = 'result'
}

function defaultSourceFor(slug, selectedLanguage) {
  if (selectedLanguage === 'answer') {
    return defaultAnswerFor(slug)
  }
  if (selectedLanguage === 'cpp') {
    return defaultCppFor(slug)
  }
  return ''
}

function defaultAnswerFor(slug) {
  if (slug === 'a-plus-b') return '3\n---\n6\n'
  if (slug === 'maximum-number') return '9\n---\n-3\n'
  if (slug === 'fibonacci') return '55\n---\n1134903170\n'
  return ''
}

function defaultCppFor(slug) {
  if (slug === 'maximum-number') {
    return `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long ans;
    cin >> ans;
    for (int i = 1; i < n; ++i) {
        long long x;
        cin >> x;
        ans = max(ans, x);
    }
    cout << ans << '\\n';
    return 0;
}
`
  }

  if (slug === 'fibonacci') {
    return `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    cin >> n;
    long long a = 0, b = 1;
    for (int i = 0; i < n; ++i) {
        long long c = a + b;
        a = b;
        b = c;
    }
    cout << a << '\\n';
    return 0;
}
`
  }

  return `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    // 在这里编写你的解题代码。
    return 0;
}
`
}

function clearSearch() {
  searchKeyword.value = ''
}

function clearFilters() {
  selectedAlgorithmCategories.value = []
  selectedDifficultyCategories.value = []
  selectedStatementLanguages.value = []
}

function tagLabel(tag) {
  const option = algorithmCategoryOptions.find((item) => item.value === tag)
  return option ? `${option.label} ${option.labelEn}` : tag
}

function toggleAlgorithmCategory(value) {
  toggleValue(selectedAlgorithmCategories, value)
}

function toggleDifficultyCategory(value) {
  toggleValue(selectedDifficultyCategories, value)
}

function toggleStatementLanguage(value) {
  toggleValue(selectedStatementLanguages, value)
}

function isAlgorithmSelected(value) {
  return selectedAlgorithmCategories.value.includes(value)
}

function isDifficultySelected(value) {
  return selectedDifficultyCategories.value.includes(value)
}

function isStatementLanguageSelected(value) {
  return selectedStatementLanguages.value.includes(value)
}

function problemTags(problem) {
  try {
    return JSON.parse(problem.tags || '[]')
  } catch {
    return []
  }
}

function toggleValue(targetRef, value) {
  if (targetRef.value.includes(value)) {
    targetRef.value = targetRef.value.filter((item) => item !== value)
  } else {
    targetRef.value = [...targetRef.value, value]
  }
}

function delay(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function formatError(error) {
  return error instanceof Error ? error.message : '请求失败'
}

onMounted(loadProblems)
</script>

<template>
  <div class="oj-shell">
    <header class="oj-header">
      <div>
        <p class="oj-kicker">StudyPlatform OJ</p>
        <h1>在线判题平台</h1>
      </div>
      <RouterLink class="home-link" to="/lab">返回实验平台</RouterLink>
    </header>

    <main class="oj-layout">
      <aside class="problem-list" aria-label="题库">
        <div class="list-header">
          <h2>题库</h2>
          <span>{{ problems.length }} 题</span>
        </div>

        <div class="problem-search">
          <input
            v-model="searchKeyword"
            type="search"
            placeholder="搜索题名、算法分类或标签"
            aria-label="搜索题目"
          />
          <button
            class="filter-toggle"
            type="button"
            :class="{ active: filterPanelOpen || activeFilterCount > 0 }"
            @click="filterPanelOpen = !filterPanelOpen"
          >
            分类筛选<span v-if="activeFilterCount"> {{ activeFilterCount }}</span>
          </button>
          <button v-if="searchKeyword" type="button" @click="clearSearch">清空</button>
        </div>

        <div class="quick-filter-row" aria-label="快捷分类筛选">
          <button
            v-for="item in quickCategoryOptions"
            :key="item.value"
            type="button"
            :class="{ active: isAlgorithmSelected(item.value) }"
            @click="toggleAlgorithmCategory(item.value)"
          >
            {{ item.label }} / {{ item.labelEn }}
          </button>
          <button
            v-for="item in difficultyOptions"
            :key="item.value"
            type="button"
            :class="{ active: isDifficultySelected(item.value) }"
            @click="toggleDifficultyCategory(item.value)"
          >
            {{ item.label }} / {{ item.labelEn }}
          </button>
          <button
            v-for="item in statementLanguageOptions"
            :key="item.value"
            type="button"
            :class="{ active: isStatementLanguageSelected(item.value) }"
            @click="toggleStatementLanguage(item.value)"
          >
            {{ item.label }} / {{ item.labelEn }}
          </button>
        </div>

        <div v-if="filterPanelOpen" class="filter-dropdown">
          <section>
            <div class="filter-title">
              <strong>算法分类</strong>
              <span>Algorithm Categories</span>
            </div>
            <label v-for="item in algorithmCategoryOptions" :key="item.value" class="filter-option">
              <input v-model="selectedAlgorithmCategories" type="checkbox" :value="item.value" />
              <span>{{ item.label }}</span>
              <em>{{ item.labelEn }}</em>
            </label>
          </section>

          <section>
            <div class="filter-title">
              <strong>难度分类</strong>
              <span>Difficulty Levels</span>
            </div>
            <label v-for="item in difficultyOptions" :key="item.value" class="filter-option">
              <input v-model="selectedDifficultyCategories" type="checkbox" :value="item.value" />
              <span>{{ item.label }}</span>
              <em>{{ item.labelEn }}</em>
            </label>
          </section>

          <section>
            <div class="filter-title">
              <strong>中英文分类</strong>
              <span>Statement Language</span>
            </div>
            <label v-for="item in statementLanguageOptions" :key="item.value" class="filter-option">
              <input v-model="selectedStatementLanguages" type="checkbox" :value="item.value" />
              <span>{{ item.label }}</span>
              <em>{{ item.labelEn }}</em>
            </label>
          </section>

          <button v-if="activeFilterCount" class="filter-clear" type="button" @click="clearFilters">
            清除分类筛选
          </button>
        </div>

        <div class="problem-results">
          <p v-if="loading" class="muted-text">正在加载题库...</p>
          <p v-else-if="problems.length === 0" class="muted-text">未找到匹配题目。</p>
          <button
            v-for="problem in problems"
            :key="problem.id"
            class="problem-item"
            :class="{ active: selectedProblem?.id === problem.id }"
            type="button"
            @click="selectProblem(problem)"
          >
            <span class="problem-title">{{ problem.title }}</span>
            <span class="problem-meta">
              <span :class="['difficulty', problem.difficulty?.toLowerCase()]">
                {{ difficultyLabel[problem.difficulty] || problem.difficulty }}
              </span>
              <span>{{ problem.timeLimitMs }} ms</span>
            </span>
            <span class="problem-tag-preview">
              <span v-for="tag in problemTags(problem).slice(0, 3)" :key="tag">{{ tagLabel(tag) }}</span>
            </span>
          </button>
        </div>
      </aside>

      <section v-if="selectedProblem" class="problem-workspace">
        <div class="workspace-toolbar">
          <div>
            <h2>{{ selectedProblem.title }}</h2>
            <div class="tag-row">
              <span v-for="tag in parsedTags" :key="tag" class="tag-pill">
                {{ tagLabel(tag) }}
              </span>
            </div>
          </div>
          <div class="limit-box">
            <span>{{ selectedProblem.timeLimitMs }} ms</span>
            <span>{{ Math.round(selectedProblem.memoryLimitKb / 1024) }} MB</span>
          </div>
        </div>

        <div class="tab-strip">
          <button :class="{ active: activeTab === 'statement' }" type="button" @click="activeTab = 'statement'">
            题面
          </button>
          <button :class="{ active: activeTab === 'submit' }" type="button" @click="activeTab = 'submit'">
            提交
          </button>
          <button :class="{ active: activeTab === 'result' }" type="button" @click="activeTab = 'result'">
            结果
          </button>
        </div>

        <section v-if="activeTab === 'statement'" class="statement-panel">
          <p class="statement-text">{{ selectedProblem.description }}</p>
          <h3>输入说明</h3>
          <p>{{ selectedProblem.inputDescription }}</p>
          <h3>输出说明</h3>
          <p>{{ selectedProblem.outputDescription }}</p>
          <h3>样例</h3>
          <div v-for="(sample, index) in parsedSamples" :key="index" class="sample-grid">
            <div>
              <strong>输入</strong>
              <pre>{{ sample.input }}</pre>
            </div>
            <div>
              <strong>输出</strong>
              <pre>{{ sample.output }}</pre>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'submit'" class="submit-panel">
          <div class="submit-toolbar">
            <label>
              语言
              <select v-model="language">
                <option value="cpp">C++</option>
                <option value="answer">answer 测试模式</option>
                <option value="java">Java</option>
                <option value="python">Python</option>
              </select>
            </label>
            <button class="primary-button" type="button" :disabled="submitting" @click="submitAnswer">
              {{ submitting ? '提交中...' : '提交判题' }}
            </button>
          </div>
          <textarea v-model="sourceCode" spellcheck="false" aria-label="代码或答案输入" />
          <p class="helper-text">
            C++ 判题需要先启动 judge-sandbox 服务，并确保后端配置 oj.sandbox-url=http://localhost:9000。
          </p>
        </section>

        <section v-if="activeTab === 'result'" class="result-panel">
          <div v-if="latestSubmission" class="result-summary">
            <span :class="['status-badge', latestSubmission.status?.toLowerCase()]">
              {{ statusLabel[latestSubmission.status] || latestSubmission.status }}
            </span>
            <span>得分 {{ latestSubmission.score }}</span>
            <span v-if="latestSubmission.message">{{ latestSubmission.message }}</span>
          </div>

          <div class="case-list">
            <button
              v-for="submission in submissions"
              :key="submission.id"
              class="submission-row"
              type="button"
              @click="showSubmissionCases(submission)"
            >
              <span>#{{ submission.id }}</span>
              <span>{{ submission.language }}</span>
              <span :class="['status-text', submission.status?.toLowerCase()]">
                {{ statusLabel[submission.status] || submission.status }}
              </span>
              <span>{{ submission.score }} 分</span>
            </button>
          </div>

          <div v-if="selectedSubmissionCases.length" class="case-detail">
            <h3>测试点详情</h3>
            <div v-for="item in selectedSubmissionCases" :key="item.id" class="case-row">
              <span>Case {{ item.testCaseId }}</span>
              <span :class="['status-text', item.status?.toLowerCase()]">
                {{ statusLabel[item.status] || item.status }}
              </span>
              <span>{{ item.timeUsedMs ?? '-' }} ms</span>
              <span>{{ item.message }}</span>
            </div>
          </div>
        </section>
      </section>

      <section v-else class="empty-state">
        <p>暂无题目。</p>
      </section>
    </main>

    <p v-if="errorMessage" class="error-toast">{{ errorMessage }}</p>
  </div>
</template>
