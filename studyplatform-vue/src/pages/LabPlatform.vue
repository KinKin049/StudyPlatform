<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const openOjPlatform = () => {
  router.push('/lab/oj')
}

const openPetroleumSimulation = () => {
  window.history.pushState({}, '', '/lab/petroleum')
  window.dispatchEvent(new Event('popstate'))
const openWellLogSimulation = () => {
  router.push('/lab/well-log')
}
</script>

<template>
  <main class="lab-page" aria-labelledby="lab-title">
    <section class="lab-hero">
      <p class="lab-kicker">Experimental Platform</p>
      <h1 id="lab-title">实验平台</h1>
      <p>
        面向课程实践、工程训练和虚拟仿真的统一入口。当前已接入在线编程平台，石油仿真模块处于规划阶段。
      </p>
    </section>

    <section class="lab-columns" aria-label="实验平台入口">
      <article class="lab-panel lab-panel-primary">
        <div class="lab-panel-header">
          <span class="lab-panel-index">01</span>
          <div>
            <p>Online Judge</p>
            <h2>在线编程平台</h2>
          </div>
        </div>

        <p class="lab-panel-copy">
          支持题库浏览、代码提交、测试点判题和提交结果追踪，适合算法训练、程序设计实验和课程作业验收。
        </p>

        <div class="lab-feature-grid">
          <section>
            <h3>题库训练</h3>
            <p>内置 A+B、数组最大值、斐波那契等示例题，可继续扩展课程题库。</p>
          </section>
          <section>
            <h3>C++ 判题</h3>
            <p>通过独立 judge-sandbox 编译运行，后端只负责提交和结果流转。</p>
          </section>
          <section>
            <h3>结果追踪</h3>
            <p>展示提交状态、得分、运行时间和测试点详情，便于定位错误。</p>
          </section>
        </div>

        <button class="lab-action" type="button" @click="openOjPlatform">
          进入在线编程平台
        </button>
      </article>

      <article class="lab-panel lab-panel-secondary">
        <div class="lab-panel-header">
          <span class="lab-panel-index">02</span>
          <div>
            <p>Petroleum Simulation</p>
            <h2>石油气仿真</h2>
          </div>
        </div>

        <p class="lab-panel-copy">
          面向油藏认知、钻采流程、测井解释和生产调度的虚拟实验模块。当前已接入测井曲线仿真与采油生产仿真。
        </p>

        <div class="lab-feature-grid">
          <section>
            <h3>测井解释</h3>
            <p>提供岩心柱状图、GR、RT、AC 四道联动剖面和层位自动识别。</p>
          </section>
          <section>
            <h3>采油生产</h3>
            <p>支持抽油机、油藏动态、注水开发的前端实时仿真。</p>
          </section>
          <section>
            <h3>实验报告</h3>
            <p>可保存关键参数和仿真结果，后端仅负责记录持久化。</p>
          </section>
        </div>

        <button class="lab-action" type="button" @click="openPetroleumSimulation">
          进入石油气仿真平台
        </button>
      </article>
    </section>
  </main>
</template>
