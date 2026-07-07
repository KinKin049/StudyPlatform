/**
 * 阶梯跳跃游戏配置文件
 * 定义游戏的场景尺寸、物理参数、关卡布局、道具配置等常量
 */

/** 游戏资源基础路径 */
export const assetBase = '/games/ladder-jump'

/** 舞台宽度 */
export const stageWidth = 1800
/** 舞台高度 */
export const stageHeight = 760
/** 玩家尺寸 */
export const playerSize = { width: 114, height: 135 }
/** 重力加速度 */
export const gravity = 0.78
/** 移动速度 */
export const moveSpeed = 6.2
/** 跳跃速度 */
export const jumpSpeed = 13.8
/** 最大跳跃次数 */
export const maxJumpCount = 3
/** 题目起始 X 坐标 */
export const questionStartX = 520
/** 题目间距 */
export const questionGap = 2100
/** 地面 Y 坐标 */
export const groundY = 660
/** 答题平台宽度 */
export const answerPlatformWidth = 990
/** 确认按钮偏移量 */
export const confirmOffset = 430
/** 已答题左侧内边距 */
export const answeredLeftPadding = 240
/** 每题行进金币数量 */
export const travelCoinCountPerQuestion = 7
/** 起始金币间距偏移 */
export const travelCoinBetweenStartOffset = questionGap - 220
/** 行进金币区域宽度 */
export const travelCoinBetweenWidth = 520
/** 行进金币所在行的 Y 坐标数组 */
export const travelCoinLanes = [groundY - 92, 520 - 64, 405 - 64]

/**
 * 平台布局配置
 * 定义不同高度平台的位置偏移
 */
export const platformLayouts = [
  { xOffset: 400, y: 520 },
  { xOffset: 640, y: 405 },
  { xOffset: 880, y: 290 },
  { xOffset: 1120, y: 175 },
]

/** 排除的题库分类编码集合 */
export const excludedQuestionBankCategoryCodes = new Set(['english'])
/** 排除的题库集编码集合 */
export const excludedQuestionBankSetCodes = new Set(['ncre'])

/**
 * 默认题目列表
 * 当无法从服务端获取题目时使用的备用题目
 */
export const defaultQuestions = [
  {
    id: 1,
    question: 'Java 中用于声明类继承关系的关键字是？',
    options: ['extends', 'implements', 'instanceof'],
    answerIndex: 0,
    explanation: 'extends 用于声明一个类继承另一个类。',
  },
  {
    id: 2,
    question: 'Vue 3 组合式 API 中用于创建响应式引用的是？',
    options: ['ref', 'map', 'bind'],
    answerIndex: 0,
    explanation: 'ref 可以创建一个响应式引用值。',
  },
  {
    id: 3,
    question: 'HTTP 状态码 404 通常表示？',
    options: ['请求成功', '资源不存在', '服务器重启'],
    answerIndex: 1,
    explanation: '404 表示客户端请求的资源没有找到。',
  },
]

/**
 * 初始车辆配置
 * 定义游戏初始时场景中的车辆信息
 */
export const initialCars = [
  { id: 'car-taxi', file: 'taxi.png', x: 260, bottom: 4, direction: 1, speed: 2.4 },
  { id: 'car-red', file: 'red.png', x: 980, bottom: 6, direction: -1, speed: 1.9 },
  { id: 'car-blue', file: 'blue.png', x: 1540, bottom: 5, direction: 1, speed: 2.1 },
  { id: 'car-white', file: 'white.png', x: 2280, bottom: 6, direction: -1, speed: 2.7 },
]

/**
 * 场景图层配置
 * 定义背景图层的层级顺序和样式类名
 */
export const sceneLayers = [
  { key: 'sky', className: 'ladder-bg-layer ladder-bg-sky' },
  { key: 'cloud', className: 'ladder-bg-layer ladder-bg-cloud' },
  { key: 'far-house', className: 'ladder-bg-layer ladder-bg-far' },
  { key: 'mid-house', className: 'ladder-bg-layer ladder-bg-mid' },
  { key: 'house', className: 'ladder-bg-layer ladder-bg-house' },
  { key: 'tree', className: 'ladder-bg-layer ladder-bg-tree' },
  { key: 'lamp', className: 'ladder-bg-layer ladder-bg-lamp' },
]
