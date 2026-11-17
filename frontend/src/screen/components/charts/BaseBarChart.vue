<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import { useChart } from '@screen/composables/useChart'
import { C, tooltipStyle } from '@screen/charts/theme'
import { months } from '@screen/data/tourism'

const { chartRef } = useChart(() => ({
  grid: { left: 8, right: 12, top: 16, bottom: 4, containLabel: true },
  tooltip: { ...tooltipStyle, trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: months,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: C.axisLine } },
    axisLabel: { color: C.textDim, fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: C.textMute, fontSize: 10 },
    splitLine: { lineStyle: { color: C.splitLine } }
  },
  series: [{ type: 'bar', barWidth: 12, itemStyle: { color: C.cyan }, data: [4920, 6270, 5510, 5780, 5880, 5690, 7190, 7740, 6330, 6850, 5920, 5520] }]
}))
</script>
