<script setup>
defineProps({
  reviews: {
    type: Array,
    default: () => [],
  },
  activeReplyId: {
    type: [Number, String],
    default: null,
  },
  replyDraft: {
    type: String,
    default: '',
  },
  submittingReply: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['start-reply', 'cancel-reply', 'update:reply-draft', 'submit-reply'])

const formatTime = (value) => (value ? String(value).replace('T', ' ') : '')

const roleLabel = (role) => {
  if (role === 'admin') return '管理员'
  if (role === 'teacher') return '教师'
  return '学生'
}

const flattenReplies = (review) => {
  const result = []
  const visit = (items, parent) => {
    for (const item of items || []) {
      result.push({
        ...item,
        replyToUserName: parent?.userName || '用户',
      })
      visit(item.replies, item)
    }
  }
  visit(review.replies, review)
  return result
}
</script>

<template>
  <div class="course-review-thread">
    <article v-for="review in reviews" :key="review.id" class="course-review-item">
      <div class="review-avatar">{{ review.userName?.slice(0, 1) || '用' }}</div>
      <div class="review-body">
        <header>
          <span class="review-author">
            <strong>{{ review.userName || '用户' }}</strong>
            <span class="review-role-badge">{{ roleLabel(review.userRoleType) }}</span>
          </span>
          <span class="review-stars">{{ '★'.repeat(review.rating || 5) }}{{ '☆'.repeat(5 - (review.rating || 5)) }}</span>
        </header>

        <p>{{ review.content }}</p>

        <div v-if="review.replyContent" class="course-review-reply">
          <strong>
            {{ review.replyUserName || '管理员' }} 回复
            <span>{{ roleLabel(review.replyUserRoleType || 'admin') }}</span>
          </strong>
          <p>{{ review.replyContent }}</p>
        </div>

        <footer>
          <time>{{ formatTime(review.createdAt) }}</time>
          <button type="button" @click="emit('start-reply', review)">回复</button>
        </footer>

        <form v-if="String(activeReplyId) === String(review.id)" class="course-reply-form" @submit.prevent="emit('submit-reply', review)">
          <textarea
            :value="replyDraft"
            rows="3"
            maxlength="500"
            placeholder="写下你的回复"
            @input="emit('update:reply-draft', $event.target.value)"
          ></textarea>
          <div>
            <button type="button" @click="emit('cancel-reply')">取消</button>
            <button type="submit" :disabled="submittingReply">
              {{ submittingReply ? '回复中...' : '发布回复' }}
            </button>
          </div>
        </form>

        <div v-if="review.replies?.length" class="course-review-replies">
          <article v-for="reply in flattenReplies(review)" :key="reply.id" class="course-reply-item">
            <div class="review-avatar review-avatar-small">{{ reply.userName?.slice(0, 1) || '用' }}</div>
            <div class="review-body">
              <header>
                <span class="review-author">
                  <strong>{{ reply.userName || '用户' }}</strong>
                  <span class="review-role-badge">{{ roleLabel(reply.userRoleType) }}</span>
                </span>
                <span class="review-reply-target">回复 @{{ reply.replyToUserName }}</span>
              </header>

              <p>{{ reply.content }}</p>

              <div v-if="reply.replyContent" class="course-review-reply">
                <strong>
                  {{ reply.replyUserName || '管理员' }} 回复
                  <span>{{ roleLabel(reply.replyUserRoleType || 'admin') }}</span>
                </strong>
                <p>{{ reply.replyContent }}</p>
              </div>

              <footer>
                <time>{{ formatTime(reply.createdAt) }}</time>
                <button type="button" @click="emit('start-reply', reply)">回复</button>
              </footer>

              <form v-if="String(activeReplyId) === String(reply.id)" class="course-reply-form" @submit.prevent="emit('submit-reply', reply)">
                <textarea
                  :value="replyDraft"
                  rows="3"
                  maxlength="500"
                  placeholder="写下你的回复"
                  @input="emit('update:reply-draft', $event.target.value)"
                ></textarea>
                <div>
                  <button type="button" @click="emit('cancel-reply')">取消</button>
                  <button type="submit" :disabled="submittingReply">
                    {{ submittingReply ? '回复中...' : '发布回复' }}
                  </button>
                </div>
              </form>
            </div>
          </article>
        </div>
      </div>
    </article>
  </div>
</template>
