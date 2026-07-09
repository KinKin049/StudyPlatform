<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchTeacherWorkbench, markTeacherMailboxRead, replyAcademyCourseReview } from '../api/academy'
import { getStoredAuthUser } from '../api/auth'

const router = useRouter()
const authUser = ref(getStoredAuthUser())
const workbench = ref(null)
const loading = ref(false)
const error = ref('')
const replyDrafts = ref({})
const replyingId = ref(null)
const markingRead = ref(false)

const mailbox = computed(() => workbench.value?.mailbox || [])
const unreadCount = computed(() => Number(workbench.value?.unreadComments ?? 0))
const metrics = computed(() => workbench.value?.metrics || [])
const isTeacher = computed(() => authUser.value?.roleType === 'teacher')

const loadMailbox = async () => {
  authUser.value = getStoredAuthUser()
  if (!authUser.value?.id) {
    router.push('/login')
    return
  }
  if (!isTeacher.value) {
    error.value = '只有教师账号可以访问信箱'
    return
  }

  loading.value = true
  error.value = ''
  try {
    workbench.value = await fetchTeacherWorkbench()
  } catch (err) {
    error.value = err.message || '信箱加载失败'
    workbench.value = null
  } finally {
    loading.value = false
  }
}

const updateReplyDraft = (messageId, value) => {
  replyDrafts.value = {
    ...replyDrafts.value,
    [messageId]: value,
  }
}

const submitReply = async (message) => {
  const content = replyDrafts.value[message.id]?.trim()
  if (!content || replyingId.value) return

  replyingId.value = message.id
  error.value = ''
  try {
    await replyAcademyCourseReview(message.id, {
      rating: 5,
      content,
    })
    const nextDrafts = { ...replyDrafts.value }
    delete nextDrafts[message.id]
    replyDrafts.value = nextDrafts
    await loadMailbox()
  } catch (err) {
    error.value = err.message || '回复失败'
  } finally {
    replyingId.value = null
  }
}

const markAllRead = async () => {
  if (unreadCount.value <= 0 || markingRead.value) return
  markingRead.value = true
  error.value = ''
  try {
    workbench.value = await markTeacherMailboxRead()
  } catch (err) {
    error.value = err.message || '标记已读失败'
  } finally {
    markingRead.value = false
  }
}

const openCourse = (message) => {
  if (!message.courseId) return
  router.push(`/academy/open-courses/${encodeURIComponent(message.courseId)}`)
}

const formatRole = (roleType) => {
  if (roleType === 'teacher') return '教师'
  if (roleType === 'admin') return '管理员'
  return ''
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(loadMailbox)
</script>

<template>
  <main class="teacher-mailbox-page">
    <section class="teacher-mailbox-hero">
      <div>
        <p>Teacher Mailbox</p>
        <h1>教师信箱</h1>
        <span>学生在你发布课程下的评论会集中到这里，可以直接回复。</span>
      </div>
      <button type="button" :disabled="unreadCount <= 0 || markingRead" @click="markAllRead">
        {{ unreadCount > 0 ? `全部标为已读（${unreadCount}）` : '全部已读' }}
      </button>
    </section>

    <section class="teacher-mailbox-metrics" aria-label="教师待办数据">
      <article v-for="item in metrics" :key="item.label">
        <i :style="{ background: item.color }"></i>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <p v-if="error" class="teacher-mailbox-alert">{{ error }}</p>

    <section class="teacher-mailbox-panel">
      <div v-if="loading" class="teacher-mailbox-state">正在加载信箱...</div>
      <div v-else-if="mailbox.length === 0" class="teacher-mailbox-state">
        暂无课程评论消息
      </div>
      <template v-else>
        <article
          v-for="message in mailbox"
          :key="message.id"
          :class="['teacher-mailbox-card', { 'is-unread': message.unread }]"
        >
          <header>
            <button type="button" @click="openCourse(message)">
              {{ message.courseTitle || '课程评论' }}
            </button>
            <time>{{ formatTime(message.createdAt) }}</time>
          </header>
          <div class="teacher-mailbox-author-line">
            <strong>{{ message.userName || '用户' }}</strong>
            <em v-if="formatRole(message.userRoleType)">{{ formatRole(message.userRoleType) }}</em>
            <span v-if="message.unread">未读</span>
          </div>
          <p>{{ message.content }}</p>
          <textarea
            :value="replyDrafts[message.id] || ''"
            rows="3"
            placeholder="回复这条评论"
            @input="updateReplyDraft(message.id, $event.target.value)"
          ></textarea>
          <footer>
            <button
              type="button"
              :disabled="replyingId === message.id || !(replyDrafts[message.id] || '').trim()"
              @click="submitReply(message)"
            >
              {{ replyingId === message.id ? '回复中...' : '回复' }}
            </button>
          </footer>
        </article>
      </template>
    </section>
  </main>
</template>
