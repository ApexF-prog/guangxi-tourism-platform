<template>
  <div class="income">
    <div class="head-row">
      <div>
        <h2 class="page-title">💰 我的收入</h2>
        <p class="page-sub">只统计本商家名下已支付订单 · 与其它商家及平台数据完全隔离</p>
      </div>
      <el-button @click="load">刷新数据</el-button>
    </div>

    <div class="stat-cards">
      <div v-for="s in cards" :key="s.label" class="stat-card glass">
        <span>{{ s.label }}</span><b class="grad-text">{{ s.value }}</b>
        <small>{{ s.hint }}</small>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-box glass">
        <div class="box-title">近 7 日营业额趋势</div>
        <div ref="lineEl" class="chart"></div>
      </div>
      <div class="chart-box glass">
        <div class="box-title">各店铺营收对比</div>
        <div ref="barEl" class="chart"></div>
      </div>
    </div>

    <div class="table-box glass">
      <div class="box-title">商品收入贡献 Top 8</div>
      <el-table :data="byProduct" size="large" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="productName" label="商品名称" min-width="220" />
        <el-table-column prop="qty" label="销量（份）" width="120" />
        <el-table-column label="收入" width="140">
          <template #default="{ row }"><span class="money">¥{{ Number(row.amount || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="占比" min-width="180">
          <template #default="{ row }">
            <div class="bar-wrap"><div class="bar-fill" :style="{ width: pct(row.amount) + '%' }"></div></div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!byProduct.length" description="暂无已支付订单" :image-size="70" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../api.js'

const summary = ref({})
const trend = ref([])
const byShop = ref([])
const byProduct = ref([])
const lineEl = ref(null)
const barEl = ref(null)
let lineChart = null, barChart = null

const cards = computed(() => [
  { label: '累计营业额', value: '¥' + money(summary.value.totalAmount), hint: '全部已支付订单' },
  { label: '今日营业额', value: '¥' + money(summary.value.todayAmount), hint: summary.value.todayOrders + ' 笔今日订单' },
  { label: '已支付订单', value: summary.value.paidOrders ?? 0, hint: '累计成交笔数' },
  { label: '客单价', value: '¥' + money(summary.value.avgAmount), hint: '平均每笔金额' }
])

function money(v) { return Number(v || 0).toFixed(2) }
function pct(v) {
  const max = Math.max(...byProduct.value.map(x => Number(x.amount || 0)), 1)
  return Math.round(Number(v || 0) / max * 100)
}

const axis = { axisLine: { lineStyle: { color: 'rgba(255,255,255,.18)' } }, axisLabel: { color: '#9fb0c8', fontSize: 11 } }

function renderLine() {
  if (!lineEl.value) return
  lineChart = lineChart || echarts.init(lineEl.value)
  lineChart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 48, right: 18, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: v => '¥' + Number(v || 0).toFixed(2) },
    xAxis: { type: 'category', data: trend.value.map(t => t.label), ...axis },
    yAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: 'rgba(255,255,255,.07)' } } },
    series: [{
      name: '营业额', type: 'line', smooth: true, symbolSize: 7,
      data: trend.value.map(t => Number(t.amount || 0)),
      lineStyle: { width: 3, color: '#38bdf8' },
      itemStyle: { color: '#38bdf8' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(56,189,248,.35)' }, { offset: 1, color: 'rgba(56,189,248,0)' }] } }
    }]
  }, true)
}

function renderBar() {
  if (!barEl.value) return
  barChart = barChart || echarts.init(barEl.value)
  barChart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 48, right: 18, top: 24, bottom: 46 },
    tooltip: { trigger: 'axis', valueFormatter: v => '¥' + Number(v || 0).toFixed(2) },
    xAxis: { type: 'category', data: byShop.value.map(s => s.shopName), ...axis,
      axisLabel: { color: '#9fb0c8', fontSize: 11, interval: 0, rotate: byShop.value.length > 3 ? 20 : 0 } },
    yAxis: { type: 'value', ...axis, splitLine: { lineStyle: { color: 'rgba(255,255,255,.07)' } } },
    series: [{
      name: '营收', type: 'bar', barMaxWidth: 42,
      data: byShop.value.map(s => Number(s.amount || 0)),
      itemStyle: { borderRadius: [6, 6, 0, 0],
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: '#a78bfa' }, { offset: 1, color: '#38bdf8' }] } }
    }]
  }, true)
}

function resize() { lineChart?.resize(); barChart?.resize() }

async function load() {
  const d = await request.get('/merchant/income')
  summary.value = d.summary || {}
  trend.value = d.trend || []
  byShop.value = d.byShop || []
  byProduct.value = d.byProduct || []
  await nextTick()
  renderLine(); renderBar()
}

onMounted(() => { load(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  lineChart?.dispose(); barChart?.dispose()
})
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 18px 0 20px; }
.stat-card { padding: 16px 18px; border-radius: 12px; }
.stat-card span { display: block; color: var(--txt-sub); font-size: 12.5px; }
.stat-card b { display: block; font-size: 24px; margin: 6px 0; }
.stat-card small { color: var(--txt-sub); font-size: 11.5px; }
.chart-row { display: grid; grid-template-columns: 1.35fr 1fr; gap: 14px; margin-bottom: 14px; }
.chart-box, .table-box { padding: 16px 18px; border-radius: 12px; }
.box-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.chart { height: 240px; width: 100%; }
.money { color: #4ade80; font-weight: 600; }
.bar-wrap { height: 8px; background: rgba(255,255,255,.08); border-radius: 6px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 6px; background: linear-gradient(90deg, #38bdf8, #a78bfa); }
@media (max-width: 1000px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
