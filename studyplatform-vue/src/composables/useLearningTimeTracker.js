/**
 * 学习时间追踪组合式函数
 * 提供学习时长统计、定时上报、页面可见性监听等功能
 */
import { onBeforeUnmount, onMounted, unref, watch } from 'vue'
import { recordProfileLearningTime } from '../api/profile'

/** 默认上报间隔（毫秒） */
const DEFAULT_FLUSH_INTERVAL_MS = 60000
/** 最小上报时长（秒） */
const MIN_FLUSH_SECONDS = 2

/**
 * 解析值，支持函数和响应式引用
 * @param {*} value - 待解析的值
 * @returns {*} 解析后的实际值
 */
const resolveValue = (value) => (typeof value === 'function' ? value() : unref(value))

/**
 * 创建学习时间追踪器
 * @param {Object} options - 配置选项
 * @param {string|function|ref} options.moduleType - 模块类型
 * @param {string|function|ref} options.targetCode - 目标编码
 * @param {string|function|ref} options.targetTitle - 目标标题
 * @param {boolean} options.autoStart - 是否自动开始追踪
 * @param {number} options.flushIntervalMs - 上报间隔（毫秒）
 * @returns {Object} 追踪器实例
 */
export function useLearningTimeTracker(options = {}) {
  /** 是否已启动 */
  let started = false
  /** 是否正在计时 */
  let counting = false
  /** 上次开始计时的时间戳 */
  let lastStartedAt = 0
  /** 待上报的毫秒数 */
  let pendingMs = 0
  /** 定时上报定时器 */
  let flushTimer = 0

  /**
   * 判断是否可以计时
   * 页面不可见时不计入学习时间
   * @returns {boolean} 是否可计时
   */
  const canCount = () => typeof document === 'undefined' || !document.hidden

  /**
   * 收集已流逝的时间
   * 将从上次开始到现在的时间累加至待上报毫秒数
   */
  const collectElapsed = () => {
    if (!counting) return
    const now = Date.now()
    pendingMs += Math.max(0, now - lastStartedAt)
    lastStartedAt = now
  }

  /**
   * 开始计时
   * 只有在已启动、未计时且页面可见时才开始
   */
  const beginCounting = () => {
    if (!started || counting || !canCount()) return
    counting = true
    lastStartedAt = Date.now()
  }

  /**
   * 暂停计时
   * 收集当前已流逝时间并停止计时
   */
  const pauseCounting = () => {
    collectElapsed()
    counting = false
    lastStartedAt = 0
  }

  /**
   * 构建上报数据载荷
   * @param {number} seconds - 学习时长（秒）
   * @returns {Object} 上报数据对象
   */
  const buildPayload = (seconds) => ({
    moduleType: resolveValue(options.moduleType) || 'general',
    targetCode: resolveValue(options.targetCode) || '',
    targetTitle: resolveValue(options.targetTitle) || '',
    durationSeconds: seconds,
  })

  /**
   * 上报学习时间
   * 将待上报的毫秒数转换为秒，满足最小时长要求时调用接口上报
   * @param {Object} params - 上报参数
   * @param {boolean} params.keepalive - 是否保持连接
   * @param {number} params.minSeconds - 最小上报时长（秒）
   */
  const flush = async ({ keepalive = false, minSeconds = MIN_FLUSH_SECONDS } = {}) => {
    collectElapsed()
    const seconds = Math.floor(pendingMs / 1000)
    if (seconds < minSeconds) return

    pendingMs -= seconds * 1000
    try {
      await recordProfileLearningTime(buildPayload(seconds), { keepalive })
    } catch (error) {
      pendingMs += seconds * 1000
      console.warn('failed to record learning time:', error)
    }
  }

  /**
   * 启动追踪器
   * 标记已启动并开始计时
   */
  const start = () => {
    if (started) return
    started = true
    beginCounting()
  }

  /**
   * 停止追踪器
   * 暂停计时、标记未启动并执行最终上报
   */
  const stop = () => {
    if (!started && pendingMs <= 0) return
    pauseCounting()
    started = false
    void flush({ keepalive: true, minSeconds: 1 })
  }

  /**
   * 处理页面可见性变化
   * 页面隐藏时暂停计时并上报，页面显示时恢复计时
   */
  const handleVisibilityChange = () => {
    if (document.hidden) {
      pauseCounting()
      void flush({ keepalive: true })
      return
    }
    beginCounting()
  }

  /**
   * 处理页面卸载前事件
   * 暂停计时并执行最终上报
   */
  const handleBeforeUnload = () => {
    pauseCounting()
    void flush({ keepalive: true, minSeconds: 1 })
  }

  onMounted(() => {
    if (options.autoStart !== false) {
      start()
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('beforeunload', handleBeforeUnload)
    flushTimer = window.setInterval(() => {
      void flush()
    }, Number(options.flushIntervalMs) || DEFAULT_FLUSH_INTERVAL_MS)
  })

  onBeforeUnmount(() => {
    stop()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('beforeunload', handleBeforeUnload)
    window.clearInterval(flushTimer)
  })

  return {
    start,
    stop,
    flush,
  }
}

/**
 * 创建视频学习时间追踪器
 * 基于学习时间追踪器，监听视频播放状态自动启停计时
 * @param {ref} videoRef - 视频元素引用
 * @param {Object} options - 配置选项，同 useLearningTimeTracker
 * @returns {Object} 追踪器实例
 */
export function useVideoLearningTimeTracker(videoRef, options = {}) {
  const tracker = useLearningTimeTracker({
    ...options,
    autoStart: false,
  })

  /** 当前活跃的视频元素 */
  let activeVideo = null

  /** 视频播放时开始追踪 */
  const startPlaybackTracking = () => tracker.start()
  /** 视频暂停/结束时停止追踪 */
  const stopPlaybackTracking = () => tracker.stop()

  /**
   * 移除视频事件监听
   * @param {HTMLVideoElement} video - 视频元素
   */
  const detachVideo = (video) => {
    if (!video) return
    video.removeEventListener('play', startPlaybackTracking)
    video.removeEventListener('playing', startPlaybackTracking)
    video.removeEventListener('pause', stopPlaybackTracking)
    video.removeEventListener('ended', stopPlaybackTracking)
    video.removeEventListener('waiting', stopPlaybackTracking)
    video.removeEventListener('emptied', stopPlaybackTracking)
  }

  /**
   * 添加视频事件监听
   * @param {HTMLVideoElement} video - 视频元素
   */
  const attachVideo = (video) => {
    if (!video) return
    video.addEventListener('play', startPlaybackTracking)
    video.addEventListener('playing', startPlaybackTracking)
    video.addEventListener('pause', stopPlaybackTracking)
    video.addEventListener('ended', stopPlaybackTracking)
    video.addEventListener('waiting', stopPlaybackTracking)
    video.addEventListener('emptied', stopPlaybackTracking)
    if (!video.paused && !video.ended) {
      tracker.start()
    }
  }

  /** 监听视频引用变化，自动切换追踪目标 */
  watch(
    () => unref(videoRef),
    (nextVideo) => {
      if (activeVideo === nextVideo) return
      detachVideo(activeVideo)
      tracker.stop()
      activeVideo = nextVideo
      attachVideo(activeVideo)
    },
    { flush: 'post' },
  )

  onBeforeUnmount(() => {
    detachVideo(activeVideo)
    tracker.stop()
  })

  return tracker
}
