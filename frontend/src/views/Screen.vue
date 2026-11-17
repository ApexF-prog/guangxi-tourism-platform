<template>
  <div class="screen">
    <ScreenHeader />
    <main class="screen-main">
      <aside class="side-col">
        <PanelBox title="热门景区门票价格对比" :subtitle="priceSubtitle">
          <PriceCompareChart />
        </PanelBox>
        <PanelBox title="月度旅游人次趋势" subtitle="单位：万人次 · 行业客流统计">
          <TrendChart />
        </PanelBox>
      </aside>

      <section class="center-col">
        <PanelBox title="广西景点热度分布" :subtitle="heatSubtitle">
          <GuangxiHeatMap />
        </PanelBox>
        <KpiStrip />
      </section>

      <aside class="side-col">
        <PanelBox title="在售商品业态构成" :subtitle="typeSubtitle">
          <TypePieChart />
        </PanelBox>
        <PanelBox title="重点城市客源结构" subtitle="单位：万人次 · 区内 / 区外">
          <SourceStackChart />
        </PanelBox>
      </aside>
    </main>

    <!-- 返回网站 -->
    <router-link to="/" class="screen-back">← 返回网站</router-link>
  </div>
</template>

<script setup>
/**
 * 广西旅游可视化大屏（移植自课程实训项目 guangxi-tourism-screen）。
 * 图表数据由后端 /api/stats/screen 聚合真实业务表提供，每 15 秒刷新；
 * 后端不可用时自动使用本地基线，保证大屏不空白。
 * 样式全部限定在 html.screen-mode 作用域内，不影响主站。
 */
import { onBeforeUnmount, onMounted, computed } from 'vue'
import ScreenHeader from '@screen/components/ScreenHeader.vue'
import PanelBox from '@screen/components/PanelBox.vue'
import KpiStrip from '@screen/components/KpiStrip.vue'
import GuangxiHeatMap from '@screen/components/charts/GuangxiHeatMap.vue'
import PriceCompareChart from '@screen/components/charts/PriceCompareChart.vue'
import TrendChart from '@screen/components/charts/TrendChart.vue'
import TypePieChart from '@screen/components/charts/TypePieChart.vue'
import SourceStackChart from '@screen/components/charts/SourceStackChart.vue'
import { data, loadScreenStats } from '@screen/data/tourism'
import '@screen/styles/screen.css'

const priceSubtitle = computed(() => {
  const arr = data.priceCompare
  const avg = arr.length ? (arr.reduce((s, d) => s + Number(d.value || 0), 0) / arr.length).toFixed(1) : '0'
  return `单位：元 · 均价 ${avg} 元`
})
const heatSubtitle = computed(() =>
  `${data.cityHeat.length} 市 · ${data.cityHeat.reduce((s, c) => s + (c.spots || 0), 0)} 个在监测景区`)
const typeSubtitle = computed(() => `共 ${data.typeShare.reduce((s, d) => s + Number(d.value || 0), 0)} 件在售商品`)

// 必须在 setup 阶段同步加上（不能等 onMounted）：
// 保证首帧渲染时 .screen 就有完整高度，否则 ECharts 初始化时容器高为 0 会报错
document.documentElement.classList.add('screen-mode')

let timer = null
onMounted(async () => {
  await loadScreenStats()
  timer = setInterval(loadScreenStats, 15000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.documentElement.classList.remove('screen-mode')
})
</script>

<style scoped>
.side-col > .panel:first-child { flex: 1.06; }
.side-col > .panel:last-child { flex: 0.94; }
.screen-back {
  position: absolute;
  right: 1.4rem;
  bottom: 1rem;
  z-index: 10;
  color: var(--text-dim);
  font-size: 0.75rem;
  text-decoration: none;
  padding: 0.3rem 0.7rem;
  border: 1px solid var(--line);
  border-radius: 0.2rem;
  background: rgba(10, 40, 68, 0.55);
}
.screen-back:hover { color: var(--cyan); border-color: var(--line-strong); }
</style>
