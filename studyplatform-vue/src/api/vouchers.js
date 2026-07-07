import { request } from './request'

export const VOUCHER_KEYS = {
  TYPE_WARRIOR_SKILL_REFRESH: 'type-warrior-skill-refresh',
  GAME_REVIVE: 'game-revive',
  TEXTBOOK_80_15: 'coupon-textbook-80-15',
}

export const fetchUserVouchers = () => request('/api/rewards/vouchers')

export const fetchVoucherItems = () => request('/api/rewards/vouchers/items')

export const exchangeVoucher = (voucherKey) =>
  request('/api/rewards/vouchers/exchange', {
    method: 'POST',
    body: JSON.stringify({ voucherKey }),
  })

export const useVoucher = (voucherKey) =>
  request('/api/rewards/vouchers/use', {
    method: 'POST',
    body: JSON.stringify({ voucherKey }),
  })
