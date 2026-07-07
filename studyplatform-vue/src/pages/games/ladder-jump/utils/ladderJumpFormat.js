/**
 * 阶梯跳跃游戏格式化工具函数
 * 提供时间格式化、随机数生成等工具方法
 */

/**
 * 格式化时长为 MM:SS 格式
 * @param {number} durationMs - 时长（毫秒）
 * @returns {string} 格式化后的时间字符串，如 "01:23"
 */
export function formatDuration(durationMs) {
  const totalSeconds = Math.max(0, Math.floor(durationMs / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

/**
 * 格式化平均时长
 * @param {number} durationMs - 时长（毫秒）
 * @returns {string} 格式化后的平均时长，如 "4.2 秒"
 */
export function formatAverageDuration(durationMs) {
  return `${(durationMs / 1000).toFixed(1)} 秒`
}

/**
 * 基于种子的伪随机数生成器
 * 使用正弦函数生成确定性的随机数
 * @param {number} seed - 随机种子
 * @returns {number} 0 到 1 之间的随机数
 */
export function seededRandom(seed) {
  const value = Math.sin(seed) * 10000
  return value - Math.floor(value)
}
