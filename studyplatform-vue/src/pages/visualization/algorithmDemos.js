export const algorithmDemos = [
  {
    id: 'single-linked-list-reverse',
    title: '单链表逆置动画',
    file: 'single-linked-list-reverse-animation(1).html',
    description: '展示单链表指针调整与逆置过程。',
  },
  {
    id: 'binary-insertion-sort',
    title: '折半插入排序过程',
    file: '折半插入排序过程可视化(1).html',
    description: '观察折半查找插入位置和元素移动过程。',
  },
  {
    id: 'kmp-next',
    title: 'KMP next 数组',
    file: 'KMP算法next数组可视化(1).html',
    description: '拆解 next 数组构造过程。',
  },
  {
    id: 'kmp-nextval',
    title: 'KMP nextval 数组',
    file: 'KMP算法nextval数组可视化(1).html',
    description: '对比 nextval 优化逻辑。',
  },
  {
    id: 'expression-tree',
    title: '中缀表达式构建表达式树',
    file: '中缀表达式构建表达式二叉树可视化动画(1).html',
    description: '演示表达式转换为二叉树的过程。',
  },
  {
    id: 'infix-to-prefix',
    title: '双栈法中缀转前缀',
    file: '双栈法中缀转前缀可视化动画(1).html',
    description: '展示操作数栈和运算符栈协同变化。',
  },
]

export const getAlgorithmAssetPath = (file) => `/visualization/data-structure/${encodeURIComponent(file)}`
