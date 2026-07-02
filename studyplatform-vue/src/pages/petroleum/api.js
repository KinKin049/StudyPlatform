const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * 向后端保存前端仿真结果。
 * 后端只做持久化，所有仿真计算仍保留在前端组件内。
 */
export function postSimulationRecord(url, payload) {
  return fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}
