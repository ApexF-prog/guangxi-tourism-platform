import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'

/**
 * 图表生命周期封装：初始化 → setOption → 容器自适应 → 销毁
 * 对应教案中的"五步绘图法"，把 resize / dispose 这两步固定下来，
 * 每个图表组件只需关心 buildOption()。
 *
 * @param {Function} buildOption 返回 ECharts option 的函数
 * @param {Object}   options     { beforeInit?: () => void, refreshKey?: () => any }
 *                               refreshKey 变化时（后端统计刷新）重新 setOption
 */
export function useChart(buildOption, options = {}) {
  const chartRef = ref(null)
  const chart = shallowRef(null)
  let observer = null
  let stopWatch = null

  const resize = () => chart.value?.resize()

  onMounted(() => {
    if (!chartRef.value) return
    options.beforeInit?.()
    chart.value = echarts.init(chartRef.value, null, { renderer: 'canvas' })
    chart.value.setOption(buildOption())
    // 数据源来自后端时，刷新后重绘（notMerge 保证类目轴长度变化也能生效）
    if (options.refreshKey) {
      stopWatch = watch(options.refreshKey, () => {
        chart.value?.setOption(buildOption(), true)
      })
    }
    // 面板宽度变化（不是窗口变化）也能触发重绘，比如侧栏折叠、布局切换
    observer = new ResizeObserver(resize)
    observer.observe(chartRef.value)
    window.addEventListener('resize', resize)
  })

  onBeforeUnmount(() => {
    stopWatch?.()
    observer?.disconnect()
    window.removeEventListener('resize', resize)
    chart.value?.dispose()
    chart.value = null
  })

  return { chartRef, chart, resize }
}

export { echarts }
