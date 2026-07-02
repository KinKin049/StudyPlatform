<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const expression = ref('sin(x) + 0.2 * x')
const xMin = ref(-10)
const xMax = ref(10)
const samples = ref(400)
const error = ref('')
const chartRef = ref(null)
let chart = null

const functionNames = ['sin', 'cos', 'tan', 'sqrt', 'abs', 'log', 'exp', 'pow', 'floor', 'ceil', 'round']

const compiledFunction = computed(() => {
  error.value = ''
  const normalized = expression.value
    .replace(/\^/g, '**')
    .replace(/\bpi\b/gi, 'Math.PI')
    .replace(/\be\b/g, 'Math.E')
    .replace(/\b(sin|cos|tan|sqrt|abs|log|exp|floor|ceil|round)\b/g, 'Math.$1')
    .replace(/\bpow\b/g, 'Math.pow')

  if (!/^[\d\s+\-*/%().,xMathPIEabcdefghijklmnopqrstuvwxyz_]*$/i.test(normalized)) {
    error.value = '表达式包含不支持的字符'
    return null
  }

  try {
    return new Function('x', `"use strict"; return ${normalized}`)
  } catch {
    error.value = '表达式无法解析'
    return null
  }
})

const presets = [
  'sin(x)',
  'cos(x)',
  'x^2 - 4',
  'sqrt(abs(x))',
  'exp(-0.1*x*x) * sin(3*x)',
  'log(abs(x)+1)',
]

function buildData() {
  const fn = compiledFunction.value
  if (!fn) return []

  const min = Number(xMin.value)
  const max = Number(xMax.value)
  const count = Math.max(60, Math.min(Number(samples.value), 1200))
  const step = (max - min) / count

  if (!Number.isFinite(min) || !Number.isFinite(max) || min >= max) {
    error.value = '请输入有效的 x 范围'
    return []
  }

  const data = []
  for (let i = 0; i <= count; i += 1) {
    const x = min + step * i
    const y = fn(x)
    data.push([Number(x.toFixed(4)), Number.isFinite(y) ? Number(y.toFixed(6)) : null])
  }
  return data
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const data = buildData()
  chart.setOption({
    animation: false,
    grid: { top: 42, right: 28, bottom: 48, left: 58 },
    tooltip: { trigger: 'axis' },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 22, bottom: 12 }],
    xAxis: { type: 'value', name: 'x', splitLine: { lineStyle: { color: '#e8f0f4' } } },
    yAxis: { type: 'value', name: 'y', splitLine: { lineStyle: { color: '#e8f0f4' } } },
    series: [
      {
        name: `y = ${expression.value}`,
        type: 'line',
        showSymbol: false,
        connectNulls: false,
        lineStyle: { width: 3, color: '#16a085' },
        data,
      },
    ],
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

watch([expression, xMin, xMax, samples], renderChart)
</script>

<template>
  <main class="visual-page function-lab-page">
    <section class="visual-hero compact">
      <p class="visual-kicker">2D Math</p>
      <h1>函数图像实验室</h1>
      <p>输入关于 x 的函数表达式，实时绘制二维曲线。支持 {{ functionNames.join(' / ') }} 等常用函数。</p>
    </section>

    <section class="function-lab-layout">
      <aside class="function-control-panel">
        <label>
          <span>函数表达式</span>
          <input v-model="expression" type="text" />
        </label>
        <div class="function-range-grid">
          <label>
            <span>x 最小值</span>
            <input v-model.number="xMin" type="number" />
          </label>
          <label>
            <span>x 最大值</span>
            <input v-model.number="xMax" type="number" />
          </label>
        </div>
        <label>
          <span>采样点数</span>
          <input v-model.number="samples" type="number" min="60" max="1200" />
        </label>
        <div class="function-presets">
          <button v-for="item in presets" :key="item" type="button" @click="expression = item">
            {{ item }}
          </button>
        </div>
        <p v-if="error" class="function-error">{{ error }}</p>
      </aside>

      <section class="function-chart-panel">
        <div ref="chartRef" class="function-chart"></div>
      </section>
    </section>
  </main>
</template>
