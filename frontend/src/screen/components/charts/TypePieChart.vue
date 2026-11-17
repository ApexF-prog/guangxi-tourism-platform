<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import { computed } from 'vue'
import { data } from '@screen/data/tourism'
import { useChart } from '@screen/composables/useChart'
import { C, tooltipStyle } from '@screen/charts/theme'

const colors = ['#38e8ff', '#2dd4bf', '#f2c14e', '#8b7cf6', '#f472b6', '#5b9df5']
const totalMap = computed(() => Object.fromEntries(data.typeShare.map((d) => [d.name, d.value])))
const typeCount = computed(() => data.typeShare.length)

const { chartRef } = useChart(() => ({
  color: colors,

  tooltip: {
    ...tooltipStyle,
    trigger: 'item',
    formatter: '{b}<br/>在售商品：<b>{c} 件</b>'
  },

  legend: {
    bottom: 0,
    left: 'center',
    width: '96%',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 10,
    icon: 'roundRect',
    textStyle: { color: C.textDim, fontSize: 10 },
    formatter: (name) => `${name} ${totalMap.value[name] ?? 0}`
  },

  graphic: [
    {
      type: 'text',
      left: 'center',
      top: '30%',
      silent: true,
      style: { text: '平台业态', fill: C.textMute, font: '11px "PingFang SC", "Microsoft YaHei"' }
    },
    {
      type: 'text',
      left: 'center',
      top: '38%',
      silent: true,
      style: { text: `${typeCount.value} 大类`, fill: '#ffffff', font: 'bold 18px Bahnschrift, Arial' }
    }
  ],

  series: [
    {
      name: '商品业态',
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      padAngle: 2,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#06182b',
        borderWidth: 1
      },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        itemStyle: { shadowBlur: 18, shadowColor: 'rgba(56, 232, 255, 0.55)' }
      },
      data: data.typeShare
    }
  ]
}), { refreshKey: () => data.version })
</script>
