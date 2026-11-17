<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import { data } from '@screen/data/tourism'
import { useChart } from '@screen/composables/useChart'
import { C, tooltipStyle } from '@screen/charts/theme'

const { chartRef } = useChart(() => ({
  grid: { left: 2, right: 10, top: 40, bottom: 2, containLabel: true },

  legend: {
    top: 0,
    right: 0,
    itemWidth: 9,
    itemHeight: 9,
    itemGap: 12,
    icon: 'roundRect',
    textStyle: { color: C.textDim, fontSize: 11 },
    data: ['区内游客', '区外游客']
  },

  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(56, 189, 248, 0.07)' } },
    valueFormatter: (v) => `${v} 万人次`
  },

  xAxis: {
    type: 'category',
    data: data.sourceStructure.map((d) => d.city),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: C.axisLine } },
    axisLabel: { color: C.textDim, fontSize: 11 }
  },

  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: C.textMute, fontSize: 10 },
    splitLine: { lineStyle: { color: C.splitLine, type: 'dashed' } }
  },

  series: [
    {
      name: '区内游客',
      type: 'bar',
      stack: '客源总量',
      barWidth: 14,
      itemStyle: { color: 'rgba(56, 232, 255, 0.85)', borderRadius: [0, 0, 2, 2] },
      data: data.sourceStructure.map((d) => d.inside)
    },
    {
      name: '区外游客',
      type: 'bar',
      stack: '客源总量',
      barWidth: 14,
      itemStyle: { color: 'rgba(242, 193, 78, 0.9)', borderRadius: [3, 3, 0, 0] },
      label: {
        show: true,
        position: 'top',
        color: C.textDim,
        fontSize: 10,
        formatter: (p) => {
          const row = data.sourceStructure[p.dataIndex]
          return row ? (row.inside + row.outside).toLocaleString() : ''
        }
      },
      data: data.sourceStructure.map((d) => d.outside)
    }
  ]
}), { refreshKey: () => data.version })
</script>
