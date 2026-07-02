import { request } from '../../api/request'

/**
 * 向后端保存前端仿真结果。
 * 后端只做持久化，所有仿真计算仍保留在前端组件内。
 */
export function postSimulationRecord(url, payload) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
