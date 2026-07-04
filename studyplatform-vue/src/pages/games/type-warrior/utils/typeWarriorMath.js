/**
 * Utility helpers used by combat, matching, and animation code.
 */
export function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

export function normalizeWord(value) {
  return value.toLowerCase().replace(/[^a-z]/g, '')
}

export function getSuffixMatchLength(buffer, target) {
  const maxLength = Math.min(buffer.length, target.length)
  for (let length = maxLength; length >= 1; length -= 1) {
    if (buffer.endsWith(target.slice(0, length))) {
      return length
    }
  }
  return 0
}

export function getDistance(x1, y1, x2, y2) {
  return Math.hypot(x2 - x1, y2 - y1)
}

export function randomFrom(list) {
  return list[Math.floor(Math.random() * list.length)]
}

export function pickRandomItems(list, count) {
  const copy = [...list]
  for (let index = copy.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1))
    ;[copy[index], copy[swapIndex]] = [copy[swapIndex], copy[index]]
  }
  return copy.slice(0, Math.max(0, count))
}
