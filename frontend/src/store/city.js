import { ref, computed, watch } from 'vue'

/** 支持的城市（含周边景区，如桂林组包含阳朔、龙胜） */
export const CITIES = [
  { key: '桂林', group: '桂林,阳朔,龙胜', label: '桂林', desc: '山水甲天下' },
  { key: '柳州', group: '柳州,三江', label: '柳州', desc: '百里柳江 · 螺蛳粉之都' },
  { key: '南宁', group: '南宁', label: '南宁', desc: '绿城 · 东盟门户' },
  { key: '北海', group: '北海', label: '北海', desc: '银滩 · 涠洲岛' }
]

const KEY = 'gx_city'
const saved = localStorage.getItem(KEY)
export const city = ref(CITIES.some(c => c.key === saved) ? saved : '桂林')

watch(city, v => {
  localStorage.setItem(KEY, v)
  document.documentElement.dataset.city = v
})

/** 当前城市对应的数据库城市分组，用于接口过滤 */
export const cityGroup = computed(() => CITIES.find(c => c.key === city.value)?.group || '')

export function setCity(v) {
  if (CITIES.some(c => c.key === v)) city.value = v
}
