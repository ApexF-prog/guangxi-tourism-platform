import { echarts } from '@screen/composables/useChart'

/** 大屏统一配色 */
export const C = {
  cyan: '#38e8ff', teal: '#2dd4bf', amber: '#f2c14e', pink: '#f472b6',
  violet: '#8b7cf6', text: '#f0f9ff', textDim: '#cbe4f7', textMute: '#aecbe4',
  axisLine: 'rgba(94, 214, 255, 0.25)', splitLine: 'rgba(94, 214, 255, 0.10)'
}

/** 折线/面积图的垂直渐变填充 */
export function areaGradient(color, from = 0.38, to = 0.02) {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: withAlpha(color, from) },
    { offset: 1, color: withAlpha(color, to) }
  ])
}

/** 横向柱状图的水平渐变 */
export function barGradient(from, to) {
  return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
    { offset: 0, color: from },
    { offset: 1, color: to }
  ])
}

function withAlpha(hex, alpha) {
  const n = hex.replace('#', '')
  const r = parseInt(n.slice(0, 2), 16)
  const g = parseInt(n.slice(2, 4), 16)
  const b = parseInt(n.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** 深色大屏统一的 tooltip 外观 */
export const tooltipStyle = {
  backgroundColor: 'rgba(6, 22, 40, 0.94)',
  borderColor: 'rgba(56, 189, 248, 0.45)',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: C.text, fontSize: 12 },
  extraCssText: 'box-shadow: 0 6px 24px rgba(0, 0, 0, .45); border-radius: 4px;'
}
