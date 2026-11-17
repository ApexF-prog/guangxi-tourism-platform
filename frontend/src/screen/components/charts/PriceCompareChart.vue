<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import { computed } from 'vue'
import { data } from '@screen/data/tourism'
import { useChart } from '@screen/composables/useChart'
import { C, barGradient, tooltipStyle } from '@screen/charts/theme'

const palette = [
  ['#ffd479', '#f2a33c'],
  ['#8ef2e4', '#26b7a6'],
  ['#a8d8ff', '#3f9de0'],
  ['#c3b5ff', '#7a6bea']
]

// 后端返回的是 scenic_spot 真实票价，坐标轴上限随数据自适应
const axisMax = computed(() => {
  const max = Math.max(100, ...data.priceCompare.map(d => Number(d.value) || 0))
  return Math.ceil(max * 1.15 / 50) * 50
})

const { chartRef } = useChart(() => ({
  grid: { left: 2, right: 52, top: 8, bottom: 2, containLabel: true },

  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(56, 189, 248, 0.07)' } },
    formatter: (params) => {
      const p = params[0]
      const row = data.priceCompare[p.dataIndex] || {}
      return `<b style="color:${C.cyan}">${p.name}</b><br/>所在城市：${row.city || '广西'}<br/>门票价格：<b style="color:${C.amber}">${p.value} 元</b>`
    }
  },

  xAxis: {
    type: 'value', max: axisMax.value, splitNumber: 4,
    axisLine: { show: false }, axisTick: { show: false },
    axisLabel: { color: C.textMute, fontSize: 10 },
    splitLine: { lineStyle: { color: C.splitLine, type: 'dashed' } }
  },

  yAxis: {
    type: 'category',
    inverse: true,
    data: data.priceCompare.map((d) => d.name),
    axisLine: { show: false }, axisTick: { show: false },
    axisLabel: { color: C.textDim, fontSize: 11, margin: 10 }
  },

  series: [
    {
      name: '门票价格',
      type: 'bar',
      data: data.priceCompare.map((d) => d.value),
      barWidth: 11,
      showBackground: true,
      backgroundStyle: { color: 'rgba(56, 189, 248, 0.06)', borderRadius: 6 },
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: (params) => {
          const [from, to] = palette[params.dataIndex % palette.length]
          return barGradient(from, to)
        }
      },
      label: {
        show: true, position: 'right', color: '#ffffff', fontSize: 10,
        fontFamily: 'Bahnschrift, Arial', formatter: '{c} 元'
      },
      markLine: {
        silent: true, symbol: 'none',
        data: [{ type: 'average', name: '平均票价' }],
        lineStyle: { color: 'rgba(242, 193, 78, 0.65)', type: 'dashed', width: 1 },
        label: { show: false }
      }
    }
  ]
}), { refreshKey: () => data.version })
</script>
