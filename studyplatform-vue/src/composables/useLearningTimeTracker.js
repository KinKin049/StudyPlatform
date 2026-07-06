import { onBeforeUnmount, onMounted, unref, watch } from 'vue'
import { recordProfileLearningTime } from '../api/profile'

const DEFAULT_FLUSH_INTERVAL_MS = 60000
const MIN_FLUSH_SECONDS = 2

const resolveValue = (value) => (typeof value === 'function' ? value() : unref(value))

export function useLearningTimeTracker(options = {}) {
  let started = false
  let counting = false
  let lastStartedAt = 0
  let pendingMs = 0
  let flushTimer = 0

  const canCount = () => typeof document === 'undefined' || !document.hidden

  const collectElapsed = () => {
    if (!counting) return
    const now = Date.now()
    pendingMs += Math.max(0, now - lastStartedAt)
    lastStartedAt = now
  }

  const beginCounting = () => {
    if (!started || counting || !canCount()) return
    counting = true
    lastStartedAt = Date.now()
  }

  const pauseCounting = () => {
    collectElapsed()
    counting = false
    lastStartedAt = 0
  }

  const buildPayload = (seconds) => ({
    moduleType: resolveValue(options.moduleType) || 'general',
    targetCode: resolveValue(options.targetCode) || '',
    targetTitle: resolveValue(options.targetTitle) || '',
    durationSeconds: seconds,
  })

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

  const start = () => {
    if (started) return
    started = true
    beginCounting()
  }

  const stop = () => {
    if (!started && pendingMs <= 0) return
    pauseCounting()
    started = false
    void flush({ keepalive: true, minSeconds: 1 })
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      pauseCounting()
      void flush({ keepalive: true })
      return
    }
    beginCounting()
  }

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

export function useVideoLearningTimeTracker(videoRef, options = {}) {
  const tracker = useLearningTimeTracker({
    ...options,
    autoStart: false,
  })

  let activeVideo = null

  const startPlaybackTracking = () => tracker.start()
  const stopPlaybackTracking = () => tracker.stop()

  const detachVideo = (video) => {
    if (!video) return
    video.removeEventListener('play', startPlaybackTracking)
    video.removeEventListener('playing', startPlaybackTracking)
    video.removeEventListener('pause', stopPlaybackTracking)
    video.removeEventListener('ended', stopPlaybackTracking)
    video.removeEventListener('waiting', stopPlaybackTracking)
    video.removeEventListener('emptied', stopPlaybackTracking)
  }

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
