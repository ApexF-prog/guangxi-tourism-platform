<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import { computed } from 'vue'
import { data } from '@screen/data/tourism'
import { useChart } from '@screen/composables/useChart'
import { C, areaGradient, tooltipStyle } from '@screen/charts/theme'

const years = computed(() => Object.keys(data.monthlyTrend))
const curYear = computed(() => years.value[0] || '2026年')
const prevYear = computed(() => years.value[1] || '2025年')

const { chartRef } = useChart(() => ({
  grid: { left: 2, right: 16, top: 46, bottom: 2, containLabel: true },

  legend: {
    top: 2,
    right: 0,
    itemWidth: 10,
    itemHeight: 6,
    itemGap: 14,
    textStyle: { color: C.textDim, fontSize: 11 },
    data: years.value
  },

  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'line', lineStyle: { color: 'rgba(155, 246, 255, 0.45)' } },
    valueFormatter: (v) => `${v} 万人次`
  },

  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: data.months,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: C.axisLine } },
    axisLabel: { color: C.textDim, fontSize: 10, interval: 1 }
  },

  yAxis: {
    type: 'value',
    name: '万人次',
    nameTextStyle: { color: C.textMute, fontSize: 10, align: 'right' },
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: C.textMute, fontSize: 10 },
    splitLine: { lineStyle: { color: C.splitLine, type: 'dashed' } }
  },

  dataZoom: [{ type: 'inside', start: 0, end: 100, zoomOnMouseWheel: false, moveOnMouseWheel: false }],

  series: [
    {
      name: curYear.value,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: {
        width: 2,
        color: C.cyan,
        shadowColor: 'rgba(56, 232, 255, 0.65)',
        shadowBlur: 10,
        shadowOffsetY: 4
      },
      itemStyle: { color: C.cyan },
      areaStyle: { color: areaGradient(C.cyan, 0.34, 0.01) },
      data: data.monthlyTrend[curYear.value],
      markPoint: {
        symbol: 'roundRect',
        symbolSize: [46, 18],
        symbolOffset: [0, -16],
        itemStyle: { color: 'rgba(242, 193, 78, 0.92)', borderRadius: 9 },
        label: {
          color: '#08192b',
          fontSize: 9,
          fontWeight: 'bold',
          position: 'inside',
          formatter: (p) => (p.value / 10000).toFixed(2) + '万'
        },
        data: [{ type: 'max', name: '峰值' }]
      }
    },
    {
      name: prevYear.value,
      type: 'line',
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 1.5, color: C.amber, type: 'dashed', opacity: 0.9 },
      itemStyle: { color: C.amber },
      areaStyle: { color: areaGradient(C.amber, 0.14, 0.01) },
      data: data.monthlyTrend[prevYear.value]
    }
  ]
}), { refreshKey: () => data.version })
</script>
