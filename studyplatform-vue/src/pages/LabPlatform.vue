<script setup>
const openOjPlatform = () => {
  window.location.href = '/oj.html'
}

const openWellLogSimulation = () => {
  window.history.pushState({}, '', '/lab/well-log')
  window.dispatchEvent(new Event('popstate'))
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
            <h2>石油仿真</h2>
          </div>
        </div>

        <p class="lab-panel-copy">
          面向油藏认知、钻采流程、管网运行和生产调度的虚拟实验模块。该模块尚未实现，当前仅作为实验平台入口占位展示。
        </p>

        <div class="lab-feature-grid">
          <section>
            <h3>场景仿真</h3>
            <p>计划提供油藏剖面、井场设备和生产流程的可视化实验场景。</p>
          </section>
          <section>
            <h3>参数实验</h3>
            <p>计划支持压力、流量、含水率等参数调节，观察生产状态变化。</p>
          </section>
          <section>
            <h3>实验报告</h3>
            <p>计划记录实验过程、关键数据和操作结论，服务课程考核。</p>
          </section>
        </div>

        <button class="lab-action" type="button" @click="openWellLogSimulation">
          进入测井仿真平台
        </button>
      </article>
    </section>
  </main>
</template>
