<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  fetchProfileOverview,
  fetchProfileUser,
  updateProfileUser,
  uploadProfileAvatar,
} from '../api/profile'
import { resolveResourceUrl } from '../api/request'

const fallbackUser = {
  name: 'Kinkin',
  handle: '@study-platform',
  role: 'StudyPlatform 学习者',
  bio: '在题库、课程、实验与背单词之间来回穿梭，把零散练习沉淀成稳定的学习曲线。',
  location: 'China',
  school: 'StudyPlatform',
  avatarUrl: '',
}

const fallbackOverview = {
  stats: [
    { label: '累计练习', value: '0', hint: '题 / 词 / 卡片' },
    { label: '已掌握', value: '0', hint: '正确或标记认识' },
    { label: '连续学习', value: '0', hint: '天' },
    { label: '题库进度', value: '0%', hint: '整体完成度' },
  ],
  overallProgress: 0,
  difficultyStats: [
    { label: '选择题', solved: 0, total: 0, color: '#2dd4bf' },
    { label: '词汇卡片', solved: 0, total: 0, color: '#60a5fa' },
    { label: '主观题', solved: 0, total: 0, color: '#f59e0b' },
  ],
  skillTracks: [
    { name: '英语四六级', progress: 0, solved: '0 / 0', tone: 'cyan' },
    { name: '公共课', progress: 0, solved: '0 / 0', tone: 'blue' },
    { name: '计算机专业', progress: 0, solved: '0 / 0', tone: 'violet' },
    { name: '职业资格', progress: 0, solved: '0 / 0', tone: 'amber' },
  ],
  recentActivities: [
    { title: '暂无真实练习记录', meta: '完成一道题或标记一个单词后，这里会自动刷新' },
  ],
  badges: ['等待第一条数据', '真实数据接口已准备'],
  activityDays: Array.from({ length: 119 }, (_, index) => ({
    id: index,
    level: 0,
    count: 0,
  })),
  learningTimes: [
    { label: '视频学习时长', value: '0m', hint: '数据库暂无记录', tone: 'cyan' },
    { label: '可视化学习时长', value: '0m', hint: '数据库暂无记录', tone: 'violet' },
  ],
  codingDifficulties: [
    { label: '简单', level: 'EASY', solved: 0, total: 0, color: '#00b8a3' },
    { label: '中等', level: 'MEDIUM', solved: 0, total: 0, color: '#ffc01e' },
    { label: '困难', level: 'HARD', solved: 0, total: 0, color: '#ef476f' },
  ],
  gameMetrics: [
    { title: '游戏时长', value: '2h 18m', meta: '样式预览 · 待接入真实计时', tone: 'cyan' },
    { title: '跳跃游戏最高纪录', value: '128 层', meta: '第一个游戏 · 最高记录', tone: 'blue' },
    { title: '节奏游戏最终得分', value: '92,480', meta: '第二个游戏 · 最终得分', tone: 'violet' },
    { title: '节奏游戏最高连击', value: '186 Combo', meta: '第二个游戏 · 最高连击', tone: 'amber' },
  ],
  mistakeMetrics: [
    { title: '错题本', value: '42 题', meta: '待复习 12 题 · 样式预览', tone: 'rose' },
    { title: '薄弱知识点', value: '7 个', meta: '选择题 / 词汇 / 主观题', tone: 'amber' },
  ],
  rankingMetrics: [
    { title: '全站排名', value: '#128', meta: '超过 82% 学习者', tone: 'cyan' },
    { title: '本周排名', value: '#19', meta: '连续学习加成中', tone: 'blue' },
  ],
  achievementMetrics: [
    { title: '成就点数', value: '1,260', meta: '样式预览 · 12 枚徽章', tone: 'violet' },
    { title: '稀有成就', value: '3 枚', meta: 'CET / OJ / 可视化', tone: 'amber' },
  ],
  textbookOrders: [
    { title: '教材订单', value: '3 单', meta: '待支付 1 · 已完成 2', tone: 'cyan' },
    { title: '教材收藏', value: '18 本', meta: '计算机 / 公共课 / 英语', tone: 'blue' },
  ],
}

const overview = ref(null)
const profileUser = ref(null)
const profileLoading = ref(false)
const profileError = ref('')
const userError = ref('')
const editingProfile = ref(false)
const savingProfile = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref(null)
const avatarCropFrameRef = ref(null)
const avatarCropImageRef = ref(null)
const avatarCropVisible = ref(false)
const avatarCropImageUrl = ref('')
const avatarCropZoom = ref(1)
const avatarCropOffset = ref({ x: 0, y: 0 })
const avatarCropBaseSize = ref({ width: 0, height: 0 })
const avatarCropDragging = ref(false)
const avatarCropDragStart = ref({ x: 0, y: 0, offsetX: 0, offsetY: 0 })
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
let feedbackTimer = null
let activeTiltElements = new Set()
const profileForm = ref({
  name: fallbackUser.name,
  bio: fallbackUser.bio,
})

const profileTiltSelector = [
  '.profile-card',
  '.profile-summary',
  '.profile-panel',
  '.profile-stats article',
  '.profile-time-list > div',
  '.profile-preview-list > div',
  '.profile-coding-list > div',
  '.profile-difficulty-list > div',
  '.profile-track-item',
  '.profile-activity-list > div',
].join(',')

const user = computed(() => profileUser.value || fallbackUser)
const dashboard = computed(() => overview.value || fallbackOverview)
const stats = computed(() => dashboard.value.stats?.length ? dashboard.value.stats : fallbackOverview.stats)
const difficultyStats = computed(() =>
  dashboard.value.difficultyStats?.length ? dashboard.value.difficultyStats : fallbackOverview.difficultyStats,
)
const skillTracks = computed(() =>
  dashboard.value.skillTracks?.length ? dashboard.value.skillTracks : fallbackOverview.skillTracks,
)
const recentActivities = computed(() =>
  dashboard.value.recentActivities?.length ? dashboard.value.recentActivities : fallbackOverview.recentActivities,
)
const badges = computed(() => dashboard.value.badges?.length ? dashboard.value.badges : fallbackOverview.badges)
const activityDays = computed(() =>
  dashboard.value.activityDays?.length ? dashboard.value.activityDays : fallbackOverview.activityDays,
)
const learningTimes = computed(() =>
  dashboard.value.learningTimes?.length ? dashboard.value.learningTimes : fallbackOverview.learningTimes,
)
const codingDifficulties = computed(() =>
  dashboard.value.codingDifficulties?.length ? dashboard.value.codingDifficulties : fallbackOverview.codingDifficulties,
)
const gameMetrics = computed(() =>
  dashboard.value.gameMetrics?.length ? dashboard.value.gameMetrics : fallbackOverview.gameMetrics,
)
const mistakeMetrics = computed(() =>
  dashboard.value.mistakeMetrics?.length ? dashboard.value.mistakeMetrics : fallbackOverview.mistakeMetrics,
)
const rankingMetrics = computed(() =>
  dashboard.value.rankingMetrics?.length ? dashboard.value.rankingMetrics : fallbackOverview.rankingMetrics,
)
const achievementMetrics = computed(() =>
  dashboard.value.achievementMetrics?.length ? dashboard.value.achievementMetrics : fallbackOverview.achievementMetrics,
)
const textbookOrders = computed(() =>
  dashboard.value.textbookOrders?.length ? dashboard.value.textbookOrders : fallbackOverview.textbookOrders,
)
const previewSections = computed(() => [
  { key: 'games', eyebrow: 'Games', title: '游戏数据', items: gameMetrics.value },
  { key: 'mistakes', eyebrow: 'Mistakes', title: '错题本', items: mistakeMetrics.value },
  { key: 'ranking', eyebrow: 'Ranking', title: '排名', items: rankingMetrics.value },
  { key: 'achievements', eyebrow: 'Achievements', title: '成就', items: achievementMetrics.value },
  { key: 'orders', eyebrow: 'Orders', title: '教材购物订单', items: textbookOrders.value },
])
const overallProgress = computed(() => {
  const progress = Number(dashboard.value.overallProgress ?? 0)
  return Math.min(Math.max(Number.isFinite(progress) ? Math.round(progress) : 0, 0), 100)
})
const progressRingStyle = computed(() => ({
  '--profile-progress': `${overallProgress.value}%`,
}))
const userInitial = computed(() => (user.value.name || 'K').trim().slice(0, 1).toUpperCase())
const avatarSrc = computed(() => resolveResourceUrl(user.value.avatarUrl))
const codingSolvedTotal = computed(() =>
  codingDifficulties.value.reduce((sum, item) => sum + Number(item.solved || 0), 0),
)
const codingQuestionTotal = computed(() =>
  codingDifficulties.value.reduce((sum, item) => sum + Number(item.total || 0), 0),
)
const codingCompletion = computed(() => {
  if (!codingQuestionTotal.value) return 0
  return Math.round((codingSolvedTotal.value / codingQuestionTotal.value) * 100)
})
const codingRingStyle = computed(() => {
  const safeSolved = Math.max(codingSolvedTotal.value, 1)
  const easy = Number(codingDifficulties.value[0]?.solved || 0) / safeSolved * 180
  const medium = Number(codingDifficulties.value[1]?.solved || 0) / safeSolved * 180
  const hard = Number(codingDifficulties.value[2]?.solved || 0) / safeSolved * 180
  return {
    '--coding-easy-end': `${easy}deg`,
    '--coding-medium-end': `${easy + medium}deg`,
    '--coding-hard-end': `${easy + medium + hard}deg`,
  }
})
const avatarCropImageStyle = computed(() => ({
  width: `${avatarCropBaseSize.value.width}px`,
  height: `${avatarCropBaseSize.value.height}px`,
  left: `calc(50% + ${avatarCropOffset.value.x}px)`,
  top: `calc(50% + ${avatarCropOffset.value.y}px)`,
  transform: `translate(-50%, -50%) scale(${avatarCropZoom.value})`,
}))

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
  }
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
    feedbackTimer = null
  }, 1800)
}

const resetTiltElement = (element) => {
  if (!element) return
  element.classList.remove('is-tilting')
  element.style.removeProperty('--profile-tilt-x')
  element.style.removeProperty('--profile-tilt-y')
}

const resetProfileTilt = () => {
  activeTiltElements.forEach(resetTiltElement)
  activeTiltElements = new Set()
}

const collectProfileTiltElements = (target, container) => {
  const tiltElements = []
  let current = target?.closest?.(profileTiltSelector)

  while (current && current !== container) {
    if (container.contains(current) && current.matches(profileTiltSelector)) {
      tiltElements.unshift(current)
    }
    current = current.parentElement?.closest?.(profileTiltSelector)
  }

  return tiltElements
}

const applyProfileTilt = (element, event) => {
  const rect = element.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width
  const y = (event.clientY - rect.top) / rect.height
  const rotateX = (0.5 - y) * 7
  const rotateY = (x - 0.5) * 9

  element.classList.add('is-tilting')
  element.style.setProperty('--profile-tilt-x', `${rotateX.toFixed(2)}deg`)
  element.style.setProperty('--profile-tilt-y', `${rotateY.toFixed(2)}deg`)
}

const handleProfileTilt = (event) => {
  const tiltElements = collectProfileTiltElements(event.target, event.currentTarget)

  if (!tiltElements.length) {
    resetProfileTilt()
    return
  }

  const nextTiltElements = new Set(tiltElements)
  activeTiltElements.forEach((element) => {
    if (!nextTiltElements.has(element)) {
      resetTiltElement(element)
    }
  })

  tiltElements.forEach((element) => applyProfileTilt(element, event))
  activeTiltElements = nextTiltElements
}

const loadProfileOverview = async () => {
  profileLoading.value = true
  profileError.value = ''
  try {
    overview.value = await fetchProfileOverview()
  } catch (error) {
    profileError.value = error.message || '真实数据加载失败'
    console.warn('profile overview load failed:', error)
  } finally {
    profileLoading.value = false
  }
}

const applyProfileUser = (nextUser) => {
  profileUser.value = nextUser
  profileForm.value = {
    name: nextUser?.name || fallbackUser.name,
    bio: nextUser?.bio || fallbackUser.bio,
  }
  window.dispatchEvent(new CustomEvent('study-platform:profile-updated', { detail: nextUser }))
}

const loadProfileUser = async () => {
  userError.value = ''
  try {
    applyProfileUser(await fetchProfileUser())
  } catch (error) {
    userError.value = error.message || '用户资料加载失败'
    console.warn('profile user load failed:', error)
  }
}

const startProfileEdit = () => {
  profileForm.value = {
    name: user.value.name,
    bio: user.value.bio,
  }
  editingProfile.value = true
  userError.value = ''
}

const cancelProfileEdit = () => {
  editingProfile.value = false
  userError.value = ''
}

const saveProfileEdit = async () => {
  const name = profileForm.value.name.trim()
  if (!name) {
    userError.value = '昵称不能为空'
    console.warn('profile user update rejected: blank name')
    showFeedback('昵称不能为空')
    return
  }
  savingProfile.value = true
  userError.value = ''
  try {
    const updatedUser = await updateProfileUser({
      name,
      bio: profileForm.value.bio.trim(),
    })
    applyProfileUser(updatedUser)
    console.info('profile user updated successfully:', updatedUser)
    showFeedback('修改个人信息成功')
    editingProfile.value = false
  } catch (error) {
    userError.value = error.message || '资料保存失败'
    console.warn('profile user update failed:', error)
    showFeedback(userError.value)
  } finally {
    savingProfile.value = false
  }
}

const openAvatarPicker = () => {
  avatarInputRef.value?.click()
}

const resetAvatarCrop = () => {
  const frame = avatarCropFrameRef.value
  const image = avatarCropImageRef.value
  if (!frame || !image?.naturalWidth || !image?.naturalHeight) return
  const frameRect = frame.getBoundingClientRect()
  const baseScale = Math.max(frameRect.width / image.naturalWidth, frameRect.height / image.naturalHeight)
  avatarCropBaseSize.value = {
    width: image.naturalWidth * baseScale,
    height: image.naturalHeight * baseScale,
  }
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
}

const limitAvatarCropOffset = (offset, zoom = avatarCropZoom.value) => {
  const frame = avatarCropFrameRef.value
  if (!frame) return offset
  const frameRect = frame.getBoundingClientRect()
  const renderedWidth = avatarCropBaseSize.value.width * zoom
  const renderedHeight = avatarCropBaseSize.value.height * zoom
  const maxX = Math.max(0, (renderedWidth - frameRect.width) / 2)
  const maxY = Math.max(0, (renderedHeight - frameRect.height) / 2)
  return {
    x: Math.min(Math.max(offset.x, -maxX), maxX),
    y: Math.min(Math.max(offset.y, -maxY), maxY),
  }
}

const normalizeAvatarCropOffset = () => {
  avatarCropOffset.value = limitAvatarCropOffset(avatarCropOffset.value)
}

const startAvatarCropDrag = (event) => {
  if (avatarUploading.value) return
  avatarCropDragging.value = true
  avatarCropDragStart.value = {
    x: event.clientX,
    y: event.clientY,
    offsetX: avatarCropOffset.value.x,
    offsetY: avatarCropOffset.value.y,
  }
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

const moveAvatarCropDrag = (event) => {
  if (!avatarCropDragging.value) return
  avatarCropOffset.value = limitAvatarCropOffset({
    x: avatarCropDragStart.value.offsetX + event.clientX - avatarCropDragStart.value.x,
    y: avatarCropDragStart.value.offsetY + event.clientY - avatarCropDragStart.value.y,
  })
}

const stopAvatarCropDrag = (event) => {
  avatarCropDragging.value = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)
}

const closeAvatarCropper = (force = false) => {
  if (avatarUploading.value && !force) return
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
  avatarCropVisible.value = false
  avatarCropImageUrl.value = ''
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
  avatarCropBaseSize.value = { width: 0, height: 0 }
  avatarCropDragging.value = false
}

const handleAvatarSelected = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    userError.value = '请选择图片文件'
    console.warn('profile avatar update rejected: non-image file:', file)
    showFeedback(userError.value)
    return
  }
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
  avatarCropImageUrl.value = URL.createObjectURL(file)
  avatarCropVisible.value = true
  userError.value = ''
}

const createCroppedAvatarFile = async () => {
  const frame = avatarCropFrameRef.value
  const image = avatarCropImageRef.value
  if (!frame || !image?.naturalWidth || !image?.naturalHeight) {
    throw new Error('头像图片尚未加载完成')
  }
  const frameRect = frame.getBoundingClientRect()
  const renderedWidth = avatarCropBaseSize.value.width * avatarCropZoom.value
  const renderedHeight = avatarCropBaseSize.value.height * avatarCropZoom.value
  const renderedLeft = frameRect.width / 2 + avatarCropOffset.value.x - renderedWidth / 2
  const renderedTop = frameRect.height / 2 + avatarCropOffset.value.y - renderedHeight / 2
  const sourceX = (0 - renderedLeft) / renderedWidth * image.naturalWidth
  const sourceY = (0 - renderedTop) / renderedHeight * image.naturalHeight
  const sourceSize = frameRect.width / renderedWidth * image.naturalWidth
  const canvasSize = 512
  const canvas = document.createElement('canvas')
  canvas.width = canvasSize
  canvas.height = canvasSize
  const context = canvas.getContext('2d')
  context.clearRect(0, 0, canvasSize, canvasSize)
  context.save()
  context.beginPath()
  context.arc(canvasSize / 2, canvasSize / 2, canvasSize / 2, 0, Math.PI * 2)
  context.clip()
  context.drawImage(
    image,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    canvasSize,
    canvasSize,
  )
  context.restore()
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    throw new Error('头像裁剪失败')
  }
  return new File([blob], 'avatar-cropped.png', { type: 'image/png' })
}

const confirmAvatarCrop = async () => {
  avatarUploading.value = true
  userError.value = ''
  try {
    const croppedFile = await createCroppedAvatarFile()
    const updatedUser = await uploadProfileAvatar(croppedFile)
    applyProfileUser(updatedUser)
    console.info('profile avatar updated successfully:', {
      fileName: croppedFile.name,
      fileSize: croppedFile.size,
      avatarUrl: updatedUser.avatarUrl,
    })
    showFeedback('头像修改成功')
    closeAvatarCropper(true)
  } catch (error) {
    userError.value = error.message || '头像上传失败'
    console.warn('profile avatar update failed:', error)
    showFeedback(userError.value)
  } finally {
    avatarUploading.value = false
  }
}

onMounted(() => {
  loadProfileOverview()
  loadProfileUser()
})

onBeforeUnmount(() => {
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
  }
  resetProfileTilt()
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
})
</script>

<template>
  <main class="profile-main" @pointermove="handleProfileTilt" @pointerleave="resetProfileTilt">
    <section class="profile-hero">
      <div class="profile-card">
        <button
          class="profile-avatar"
          type="button"
          :disabled="avatarUploading"
          :aria-label="avatarUploading ? '头像上传中' : '上传头像'"
          @click="openAvatarPicker"
        >
          <img v-if="avatarSrc" :src="avatarSrc" :alt="user.name" />
          <span v-else>{{ userInitial }}</span>
          <small>{{ avatarUploading ? '上传中' : '换头像' }}</small>
        </button>
        <input
          ref="avatarInputRef"
          class="profile-avatar-input"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          @change="handleAvatarSelected"
        />
        <div class="profile-identity">
          <p class="profile-role">{{ user.role }}</p>
          <h1>{{ user.name }}</h1>
          <span>{{ user.handle }}</span>
        </div>
        <p class="profile-bio">{{ user.bio }}</p>
        <button class="profile-edit-button" type="button" @click="startProfileEdit">编辑资料</button>
        <p v-if="userError && !editingProfile" class="profile-user-message">{{ userError }}</p>
        <div class="profile-meta">
          <span>{{ user.school }}</span>
          <span>{{ user.location }}</span>
          <span>目标：稳稳变强</span>
        </div>
      </div>

      <div class="profile-summary">
        <p>{{ profileLoading ? 'Syncing Data' : 'Learning Dashboard' }}</p>
        <h2>今天也有一点进步，被系统悄悄记下来了。</h2>
        <span v-if="profileError" class="profile-data-note">暂时使用兜底数据：{{ profileError }}</span>
        <div class="profile-stats">
          <article v-for="item in stats" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </div>
    </section>

    <section class="profile-insight-grid">
      <article class="profile-panel profile-time-panel">
        <div class="profile-panel-head">
          <div>
            <p>Learning Time</p>
            <h2>学习时长</h2>
          </div>
          <span>数据库</span>
        </div>
        <div class="profile-time-list">
          <div v-for="item in learningTimes" :key="item.label" :class="`is-${item.tone}`">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </article>

      <article class="profile-panel profile-coding-panel">
        <div class="profile-panel-head">
          <div>
            <p>Code Progress</p>
            <h2>编程题难度</h2>
          </div>
          <span>OJ 数据</span>
        </div>
        <div class="profile-coding-content">
          <div class="profile-coding-ring" :style="codingRingStyle">
            <div class="profile-coding-ring-mask">
              <strong>{{ codingSolvedTotal }}</strong>
              <span>已完成 / {{ codingQuestionTotal }}</span>
              <small>{{ codingCompletion }}%</small>
            </div>
          </div>
          <div class="profile-coding-list">
            <div v-for="item in codingDifficulties" :key="item.level">
              <span>
                <i :style="{ background: item.color }"></i>
                {{ item.label }}
              </span>
              <strong>{{ item.solved }} / {{ item.total }}</strong>
            </div>
          </div>
        </div>
      </article>

      <article class="profile-panel profile-badge-panel">
        <div class="profile-panel-head">
          <div>
            <p>Badges</p>
            <h2>成就徽章</h2>
          </div>
        </div>
        <div class="profile-badges">
          <span v-for="badge in badges" :key="badge">{{ badge }}</span>
        </div>
      </article>
    </section>

    <section class="profile-preview-grid" aria-label="个人主页扩展数据预览">
      <article v-for="section in previewSections" :key="section.key" class="profile-panel profile-preview-panel">
        <div class="profile-panel-head">
          <div>
            <p>{{ section.eyebrow }}</p>
            <h2>{{ section.title }}</h2>
          </div>
        </div>
        <div class="profile-preview-list">
          <div v-for="item in section.items" :key="item.title" :class="`is-${item.tone}`">
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.meta }}</small>
          </div>
        </div>
      </article>
    </section>

    <section class="profile-grid">
      <div class="profile-column">
        <article class="profile-panel profile-progress-panel">
          <div class="profile-panel-head">
            <div>
              <p>Progress</p>
              <h2>练习分布</h2>
            </div>
            <strong>{{ overallProgress }}%</strong>
          </div>
          <div
            class="profile-ring"
            :style="progressRingStyle"
            :aria-label="`整体题库完成度 ${overallProgress}%`"
          >
            <span>{{ overallProgress }}%</span>
          </div>
          <div class="profile-difficulty-list">
            <div v-for="item in difficultyStats" :key="item.label">
              <span>
                <i :style="{ background: item.color }"></i>
                {{ item.label }}
              </span>
              <strong>{{ item.solved }} / {{ item.total }}</strong>
            </div>
          </div>
        </article>

      </div>

      <div class="profile-column">
        <article class="profile-panel profile-heatmap-panel">
          <div class="profile-panel-head">
            <div>
              <p>Activity</p>
              <h2>学习热力图</h2>
            </div>
            <span>近 17 周</span>
          </div>
          <div class="profile-heatmap" aria-label="近 17 周学习活跃度">
            <span
              v-for="day in activityDays"
              :key="day.id"
              :class="`is-level-${day.level}`"
            ></span>
          </div>
          <div class="profile-heatmap-legend">
            <span>少</span>
            <i class="is-level-1"></i>
            <i class="is-level-2"></i>
            <i class="is-level-3"></i>
            <i class="is-level-4"></i>
            <span>多</span>
          </div>
        </article>

        <article class="profile-panel profile-track-panel">
          <div class="profile-panel-head">
            <div>
              <p>Question Banks</p>
              <h2>题库进度</h2>
            </div>
          </div>
          <div class="profile-track-list">
            <div v-for="track in skillTracks" :key="track.name" class="profile-track-item">
              <div>
                <strong>{{ track.name }}</strong>
                <span>{{ track.solved }}</span>
              </div>
              <div class="profile-track-bar">
                <i :class="`is-${track.tone}`" :style="{ width: `${track.progress}%` }"></i>
              </div>
              <em>{{ track.progress }}%</em>
            </div>
          </div>
        </article>

        <article class="profile-panel profile-activity-panel">
          <div class="profile-panel-head">
            <div>
              <p>Recent</p>
              <h2>最近动态</h2>
            </div>
          </div>
          <div class="profile-activity-list">
            <div v-for="activity in recentActivities" :key="activity.title">
              <i></i>
              <div>
                <strong>{{ activity.title }}</strong>
                <span>{{ activity.meta }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <div
      v-if="avatarCropVisible"
      class="profile-modal-backdrop profile-crop-backdrop"
      role="presentation"
      @click.self="closeAvatarCropper()"
    >
      <section
        class="profile-edit-dialog profile-avatar-crop-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-avatar-crop-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Avatar Crop</p>
            <h2 id="profile-avatar-crop-title">选取头像区域</h2>
          </div>
          <button type="button" :disabled="avatarUploading" aria-label="关闭头像裁剪" @click="closeAvatarCropper()">
            ×
          </button>
        </div>

        <div
          ref="avatarCropFrameRef"
          class="profile-avatar-crop-frame"
          :class="{ 'is-dragging': avatarCropDragging }"
          @pointerdown="startAvatarCropDrag"
          @pointermove="moveAvatarCropDrag"
          @pointerup="stopAvatarCropDrag"
          @pointercancel="stopAvatarCropDrag"
        >
          <img
            ref="avatarCropImageRef"
            :src="avatarCropImageUrl"
            alt="待裁剪头像"
            :style="avatarCropImageStyle"
            draggable="false"
            @dragstart.prevent
            @load="resetAvatarCrop"
          />
          <span class="profile-avatar-crop-ring" aria-hidden="true"></span>
        </div>

        <div class="profile-avatar-crop-tools">
          <label>
            缩放
            <input
              v-model.number="avatarCropZoom"
              type="range"
              min="1"
              max="3"
              step="0.01"
              :disabled="avatarUploading"
              @input="normalizeAvatarCropOffset"
            />
          </label>
          <p>拖动图片调整位置，圆形区域会成为最终头像。</p>
        </div>

        <p v-if="userError" class="profile-user-message">{{ userError }}</p>
        <div class="profile-edit-actions">
          <button type="button" :disabled="avatarUploading" @click="confirmAvatarCrop">
            {{ avatarUploading ? '上传中' : '确认使用' }}
          </button>
          <button type="button" :disabled="avatarUploading" @click="closeAvatarCropper()">取消</button>
        </div>
      </section>
    </div>

    <div
      v-if="editingProfile"
      class="profile-modal-backdrop"
      role="presentation"
      @click.self="cancelProfileEdit"
    >
      <section
        class="profile-edit-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-edit-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Profile Settings</p>
            <h2 id="profile-edit-title">编辑个人资料</h2>
          </div>
          <button type="button" :disabled="savingProfile" aria-label="关闭编辑窗口" @click="cancelProfileEdit">
            ×
          </button>
        </div>

        <form class="profile-edit-form" @submit.prevent="saveProfileEdit">
          <label>
            昵称
            <input v-model="profileForm.name" type="text" maxlength="64" />
          </label>
          <label>
            简介
            <textarea v-model="profileForm.bio" maxlength="512" rows="4"></textarea>
          </label>
          <p v-if="userError" class="profile-user-message">{{ userError }}</p>
          <div class="profile-edit-actions">
            <button type="submit" :disabled="savingProfile">
              {{ savingProfile ? '保存中' : '保存资料' }}
            </button>
            <button type="button" :disabled="savingProfile" @click="cancelProfileEdit">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="feedbackVisible"
      class="profile-feedback-toast"
      role="status"
      aria-live="polite"
    >
      {{ feedbackMessage }}
    </div>
  </main>
</template>
