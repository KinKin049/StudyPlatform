<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  createSubmission,
  getProblem,
  getSubmission,
  listProblems,
  listSubmissionCases,
} from '../oj/api'

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

const difficultyLabel = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
}

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

watch(language, () => {
  sourceCode.value = defaultSourceFor(selectedProblem.value?.slug, language.value)
})

async function loadProblems() {
  loading.value = true
  errorMessage.value = ''
  try {
    problems.value = await listProblems()
    if (problems.value.length > 0) {
      await selectProblem(problems.value[0])
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

    long long a, b;
    cin >> a >> b;
    cout << a + b << '\\n';
    return 0;
}
`
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
      <a class="home-link" href="/">返回首页</a>
    </header>

    <main class="oj-layout">
      <aside class="problem-list" aria-label="题库">
        <div class="list-header">
          <h2>题库</h2>
          <span>{{ problems.length }} 题</span>
        </div>

        <p v-if="loading" class="muted-text">正在加载题库...</p>
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
        </button>
      </aside>

      <section v-if="selectedProblem" class="problem-workspace">
        <div class="workspace-toolbar">
          <div>
            <h2>{{ selectedProblem.title }}</h2>
            <div class="tag-row">
              <span v-for="tag in parsedTags" :key="tag" class="tag-pill">
                {{ tag }}
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
          <textarea v-model="sourceCode" spellcheck="false" aria-label="代码或答案输出" />
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
