<script setup>
/**
 * 空间模型实验室目录页
 * 展示高等数学、大学物理和概率论三个学科的三维模型列表
 * 提供模型入口导航
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'
import {
  calculusModelOptions,
  physicsModelOptions,
  probabilityModelOptions,
  subjectOptions,
} from './spaceModelCatalog'

useLearningTimeTracker({
  moduleType: 'visualization',
  targetCode: 'space-models',
  targetTitle: '空间模型实验室',
})

/** 学科与模型配置映射 */
const subjectModels = {
  calculus: calculusModelOptions,
  physics: physicsModelOptions,
  probability: probabilityModelOptions,
}

/** 学科描述信息 */
const subjectNotes = {
  calculus: '曲面、空间曲线、二重积分、梯度场与切平面',
  physics: '量子轨道、电磁场、波动、热运动与刚体旋转',
  probability: '联合密度、分布函数、区间概率、相关性与收敛',
}

/**
 * 学科列表（含模型信息）
 * 将学科配置、描述和模型列表合并
 */
const subjects = computed(() => subjectOptions.map((subject) => ({
  ...subject,
  note: subjectNotes[subject.id],
  models: subjectModels[subject.id] ?? [],
})))

/** 总模型数量 */
const totalModelCount = computed(() => subjects.value.reduce((sum, subject) => sum + subject.models.length, 0))

/**
 * 生成模型路由链接
 * @param {string} subjectId - 学科标识
 * @param {string} modelId - 模型标识
 * @returns {Object} 路由配置对象
 */
const modelRoute = (subjectId, modelId) => ({
  path: '/visualization/space-3d',
  query: {
    subject: subjectId,
    model: modelId,
  },
})
</script>

<template>
  <main class="visual-page space-guide-page">
    <!-- 页面头部区域 -->
    <section class="space-guide-simple-hero">
      <RouterLink class="space-guide-back" to="/visualization">返回可视化</RouterLink>
      <p>3D Model Atlas</p>
      <h1>空间模型实验室</h1>
      <span>
        选择一个科目下的模型名称，直接进入对应的三维实验台。当前共 {{ subjects.length }} 个科目，
        {{ totalModelCount }} 个模型。
      </span>
    </section>

    <!-- 模型目录区域 -->
    <section class="space-guide-directory" aria-label="空间模型目录">
      <section v-for="subject in subjects" :key="subject.id" class="space-guide-group">
        <!-- 学科标题和描述 -->
        <div class="space-guide-group-head">
          <h2>{{ subject.label }}</h2>
          <span>{{ subject.note }}</span>
        </div>

        <!-- 模型导航列表 -->
        <nav :aria-label="`${subject.label}模型`" class="space-guide-link-list">
          <RouterLink
            v-for="model in subject.models"
            :key="model.id"
            :to="modelRoute(subject.id, model.id)"
          >
            <span>{{ model.name }}</span>
            <small>{{ model.formula }}</small>
          </RouterLink>
        </nav>
      </section>
    </section>
  </main>
</template>
