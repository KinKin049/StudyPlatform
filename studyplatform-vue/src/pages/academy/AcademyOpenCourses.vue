<!-- 在线开放课程列表页面组件，支持课程搜索、分类筛选和教师发布课程功能 -->
<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute } from 'vue-router'
import { getStoredAuthUser } from '../../api/auth'
import { publishOnlineOpenCourse } from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'
import { useAcademyList } from '../../composables/useAcademyList'

const route = useRoute()

// 使用课程列表组合式函数，获取在线开放课程数据
const {
  selectedCategory,
  keyword,
  categories,
  filteredItems: filteredCourses,
  pagedItems: pagedCourses,
  currentPage,
  pageSize,
  totalItems,
  loading,
  error,
  loadItems,
} = useAcademyList('online-open-courses', ['name', 'teacher', 'school', 'category'])

// 当前登录用户信息
const authUser = ref(getStoredAuthUser())
// 发布课程相关状态
const publishing = ref(false)
const publishDialogOpen = ref(false)
const publishError = ref('')
const publishSuccess = ref('')
// 上传文件
const coverFile = ref(null)
const videoFile = ref(null)
// 发布课程表单数据
const publishForm = ref({
  courseName: '',
  startTime: '',
  category: '',
  semesterPlan: '',
  courseDetail: '',
  courseOverview: '',
})

/**
 * 计算属性：判断当前用户是否为教师角色
 */
const isTeacher = computed(() => authUser.value?.roleType === 'teacher')

/**
 * 解析课程封面图片 URL
 */
const resolveCover = (course) => resolveResourceUrl(course.cover || course.coverUrl)

/**
 * 封面图片加载失败时使用备用地址
 */
const useCoverFallback = (event, course) => {
  if (course.coverUrl && event.target.src !== course.coverUrl) {
    event.target.src = course.coverUrl
  }
}

/**
 * 打开发布课程弹窗
 */
const openPublishDialog = () => {
  publishDialogOpen.value = true
  publishError.value = ''
  publishSuccess.value = ''
}

/**
 * 关闭发布课程弹窗
 */
const closePublishDialog = () => {
  if (publishing.value) return
  publishDialogOpen.value = false
  publishError.value = ''
}

/**
 * 处理封面文件选择
 */
const handleCoverSelected = (event) => {
  coverFile.value = event.target.files?.[0] || null
}

/**
 * 处理视频文件选择
 */
const handleVideoSelected = (event) => {
  videoFile.value = event.target.files?.[0] || null
}

/**
 * 重置发布课程表单
 */
const resetPublishForm = () => {
  publishForm.value = {
    courseName: '',
    startTime: '',
    category: '',
    semesterPlan: '',
    courseDetail: '',
    courseOverview: '',
  }
  coverFile.value = null
  videoFile.value = null
}

/**
 * 提交发布课程请求
 * 构建 FormData 并调用 API 发布课程
 */
const submitPublishCourse = async () => {
  publishError.value = ''
  publishSuccess.value = ''
  if (!coverFile.value || !videoFile.value) {
    publishError.value = '请上传课程封面和视频'
    return
  }
  if (!publishForm.value.category) {
    publishError.value = '请选择课程分类'
    return
  }
  const formData = new FormData()
  Object.entries(publishForm.value).forEach(([key, value]) => {
    formData.append(key, String(value || '').trim())
  })
  formData.append('cover', coverFile.value)
  formData.append('video', videoFile.value)

  publishing.value = true
  try {
    await publishOnlineOpenCourse(formData)
    publishSuccess.value = '课程发布成功'
    resetPublishForm()
    await loadItems()
    window.setTimeout(() => {
      publishDialogOpen.value = false
      publishSuccess.value = ''
    }, 700)
  } catch (error) {
    publishError.value = error.message || '课程发布失败'
  } finally {
    publishing.value = false
  }
}

// 监听分类列表和路由参数变化，同步筛选条件
watch(
  [categories, () => route.query.category, () => route.query.keyword],
  ([categoryList, category, queryKeyword]) => {
    if (typeof category === 'string' && categoryList.includes(category)) {
      selectedCategory.value = category
    }

    if (typeof queryKeyword === 'string') {
      keyword.value = queryKeyword
    }
  },
  { immediate: true },
)
</script>

<template>
  <!-- 在线开放课程列表页面主容器 -->
  <main class="online-course-main">
    <!-- 页面顶部标题区域 -->
    <section class="online-course-hero" aria-labelledby="online-course-title">
      <div>
        <h1 id="online-course-title">在线开放课程</h1>
      </div>

      <!-- 搜索框 -->
      <div class="online-course-search online-course-hero-search" role="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索课程、讲师、学校或类别"
          aria-label="搜索课程、讲师、学校或类别"
        />
      </div>
      <!-- 教师发布课程按钮 -->
      <button
        v-if="isTeacher"
        class="online-course-publish-button"
        type="button"
        @click="openPublishDialog"
      >
        发布课程
      </button>
    </section>

    <!-- 课程分类筛选区域 -->
    <section class="online-course-tools" aria-label="课程筛选">
      <div class="online-category-list">
        <button
          v-for="category in categories"
          :key="category"
          type="button"
          :class="{ active: selectedCategory === category }"
          @click="selectedCategory = category"
        >
          {{ category }}
        </button>
      </div>
    </section>

    <!-- 课程列表区域 -->
    <section class="online-course-board" aria-label="在线开放课程列表">
      <div class="online-course-summary">
        <h2>{{ selectedCategory }}课程</h2>
        <span>共 {{ filteredCourses.length }} 门</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="academy-state">正在加载课程数据...</div>
      <!-- 错误状态 -->
      <div v-else-if="error" class="academy-state academy-state-error">
        <span>{{ error }}</span>
        <button type="button" @click="loadItems">重试</button>
      </div>
      <!-- 空状态 -->
      <div v-else-if="filteredCourses.length === 0" class="academy-state">暂无匹配课程</div>

      <!-- 课程卡片网格 -->
      <div v-else class="online-course-grid">
        <article v-for="course in pagedCourses" :key="course.id" class="online-course-card">
          <RouterLink :to="`/academy/open-courses/${encodeURIComponent(course.id)}`">
            <img
              :src="resolveCover(course)"
              :alt="course.name"
              loading="lazy"
              @error="useCoverFallback($event, course)"
            />
            <div class="online-course-card-body">
              <div class="online-course-card-meta">
                <span>{{ course.category }}</span>
                <strong>{{ course.participants }} 人参加</strong>
              </div>
              <h3>{{ course.name }}</h3>
              <dl>
                <div>
                  <dt>讲师</dt>
                  <dd>{{ course.teacher || '暂未提供' }}</dd>
                </div>
                <div>
                  <dt>开课时间</dt>
                  <dd>{{ course.startTime || '待定' }}</dd>
                </div>
              </dl>
              <p>{{ course.school }}</p>
            </div>
          </RouterLink>
        </article>
      </div>

      <!-- 分页组件 -->
      <el-pagination
        v-if="filteredCourses.length > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        class="academy-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[8, 12, 16, 24]"
        :total="totalItems"
      />
    </section>

    <!-- 发布课程弹窗 -->
    <div
      v-if="publishDialogOpen"
      class="online-course-publish-backdrop"
      role="presentation"
      @click.self="closePublishDialog"
    >
      <section class="online-course-publish-dialog" role="dialog" aria-modal="true" aria-labelledby="publish-course-title">
        <div class="online-course-publish-head">
          <div>
            <p>Teacher Course</p>
            <h2 id="publish-course-title">发布课程</h2>
          </div>
          <button type="button" :disabled="publishing" aria-label="关闭发布课程弹窗" @click="closePublishDialog">×</button>
        </div>

        <!-- 发布课程表单 -->
        <form class="online-course-publish-form" @submit.prevent="submitPublishCourse">
          <label>
            课程名称
            <input v-model="publishForm.courseName" type="text" maxlength="120" required />
          </label>
          <label>
            开课时间
            <input v-model="publishForm.startTime" type="text" maxlength="64" placeholder="例如：2026-09-01" required />
          </label>
          <label>
            课程分类
            <select v-model="publishForm.category" required>
              <option value="" disabled>请选择分类</option>
              <option v-for="category in categories.filter((item) => item !== '全部')" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
          </label>
          <label>
            学期安排
            <input v-model="publishForm.semesterPlan" type="text" maxlength="512" placeholder="例如：16 周，每周 2 学时" required />
          </label>
          <label>
            课程概述
            <textarea v-model="publishForm.courseOverview" rows="3" maxlength="1200" required></textarea>
          </label>
          <label>
            课程详情
            <textarea v-model="publishForm.courseDetail" rows="5" maxlength="4000" required></textarea>
          </label>
          <!-- 文件上传区域 -->
          <div class="online-course-upload-grid">
            <label>
              上传课程封面
              <input type="file" accept="image/png,image/jpeg,image/webp" required @change="handleCoverSelected" />
              <span>{{ coverFile?.name || '未选择文件' }}</span>
            </label>
            <label>
              上传课程视频
              <input type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime" required @change="handleVideoSelected" />
              <span>{{ videoFile?.name || '未选择文件' }}</span>
            </label>
          </div>
          <!-- 提示信息 -->
          <p v-if="publishError" class="online-course-publish-message is-error">{{ publishError }}</p>
          <p v-if="publishSuccess" class="online-course-publish-message is-success">{{ publishSuccess }}</p>
          <!-- 表单操作按钮 -->
          <div class="online-course-publish-actions">
            <button type="submit" :disabled="publishing">{{ publishing ? '发布中...' : '确认发布' }}</button>
            <button type="button" :disabled="publishing" @click="closePublishDialog">取消</button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
