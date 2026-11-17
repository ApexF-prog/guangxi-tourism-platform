/**
 * 可视化大屏数据源。
 *
 * 优先读取后端 GET /api/stats/screen：订单、交易额、门票价格、商品业态、
 * 景区点位全部由 MySQL 业务表 SQL 聚合；城市客流来自 t_visit_stat 事实表。
 * 每 15 秒刷新一次，游客下一单后大屏 KPI 与当月趋势会真实变化。
 *
 * 接口不可用时（例如纯静态演示环境）自动退回下面的基线数据，
 * 保证大屏不会因为后端未启动而出现空白图表。
 */
import { reactive, computed } from 'vue'
import request from '@/api.js'

/** 14 市基线热度：value = 年接待游客量（万人次），spots = 在监测景区数 */
const BASE_CITY_HEAT = [
  { name: '桂林市', value: 12800, spots: 46 },
  { name: '南宁市', value: 11200, spots: 38 },
  { name: '柳州市', value: 7600, spots: 26 },
  { name: '北海市', value: 6800, spots: 22 },
  { name: '玉林市', value: 5200, spots: 18 },
  { name: '崇左市', value: 4600, spots: 19 },
  { name: '百色市', value: 4200, spots: 20 },
  { name: '贺州市', value: 3800, spots: 16 },
  { name: '河池市', value: 3600, spots: 17 },
  { name: '钦州市', value: 3200, spots: 14 },
  { name: '贵港市', value: 3000, spots: 13 },
  { name: '梧州市', value: 2800, spots: 15 },
  { name: '防城港市', value: 2600, spots: 12 },
  { name: '来宾市', value: 2200, spots: 11 }
]

/**
 * 景区经纬度字典：数据库里没有坐标字段（属于静态地理字典），
 * 接口返回的真实景点按名称匹配坐标后落到地图上。
 */
const BASE_SCENIC_SPOTS = [
  { name: '漓江风景区', city: '桂林市', coord: [110.45, 25.02], heat: 96, price: 215, star: '5A' },
  { name: '阳朔西街', city: '桂林市', coord: [110.49, 24.78], heat: 92, price: 0, star: '5A' },
  { name: '涠洲岛', city: '北海市', coord: [109.12, 21.03], heat: 90, price: 98, star: '5A' },
  { name: '德天瀑布', city: '崇左市', coord: [106.72, 22.85], heat: 89, price: 115, star: '5A' },
  { name: '象鼻山', city: '桂林市', coord: [110.3, 25.26], heat: 88, price: 55, star: '5A' },
  { name: '北海银滩', city: '北海市', coord: [109.15, 21.42], heat: 86, price: 0, star: '4A' },
  { name: '两江四湖', city: '桂林市', coord: [110.29, 25.28], heat: 85, price: 145, star: '5A' },
  { name: '龙脊梯田', city: '桂林市', coord: [110.12, 25.75], heat: 84, price: 80, star: '4A' },
  { name: '青秀山', city: '南宁市', coord: [108.39, 22.79], heat: 82, price: 20, star: '5A' },
  { name: '黄姚古镇', city: '贺州市', coord: [111.2, 24.25], heat: 78, price: 100, star: '5A' },
  { name: '明仕田园', city: '崇左市', coord: [107.0, 22.86], heat: 76, price: 0, star: '4A' },
  { name: '通灵大峡谷', city: '百色市', coord: [106.55, 23.05], heat: 74, price: 115, star: '4A' },
  { name: '巴马长寿村', city: '河池市', coord: [107.25, 24.15], heat: 72, price: 0, star: '4A' },
  { name: '银子岩', city: '桂林市', coord: [110.28, 24.65], heat: 70, price: 65, star: '4A' },
  { name: '乐业天坑群', city: '百色市', coord: [106.55, 24.78], heat: 68, price: 90, star: '4A' },
  { name: '程阳八寨', city: '柳州市', coord: [109.65, 25.9], heat: 66, price: 60, star: '4A' },
  { name: '三娘湾', city: '钦州市', coord: [108.75, 21.65], heat: 60, price: 0, star: '4A' },
  { name: '桂平西山', city: '贵港市', coord: [110.08, 23.4], heat: 58, price: 60, star: '4A' }
]
const SPOT_COORDS = Object.fromEntries(BASE_SCENIC_SPOTS.map(s => [s.name, s.coord]))

const BASE_PRICE_COMPARE = BASE_SCENIC_SPOTS
  .filter(s => s.price > 0)
  .sort((a, b) => b.price - a.price)
  .map(s => ({ name: s.name, value: s.price, city: s.city }))

/** 全部图表共用的响应式数据盒；version 变化时图表重新 setOption */
export const data = reactive({
  cityHeat: BASE_CITY_HEAT,
  scenicSpots: BASE_SCENIC_SPOTS,
  priceCompare: BASE_PRICE_COMPARE,
  months: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
  monthlyTrend: {
    '2026年': [4920, 6270, 5510, 5780, 5880, 5690, 7190, 7740, 6330, 6850, 5920, 5520],
    '2025年': [4490, 5730, 5030, 5280, 5370, 5200, 6570, 7070, 5780, 6260, 5410, 5040]
  },
  typeShare: [
    { name: '自然山水', value: 31 }, { name: '滨海度假', value: 19 },
    { name: '古镇民俗', value: 16 }, { name: '康养度假', value: 14 },
    { name: '主题乐园', value: 11 }, { name: '红色研学', value: 9 }
  ],
  sourceStructure: [
    { city: '桂林', inside: 7420, outside: 5380 },
    { city: '南宁', inside: 6940, outside: 4260 },
    { city: '柳州', inside: 4860, outside: 2740 },
    { city: '北海', inside: 3550, outside: 3250 },
    { city: '玉林', inside: 3480, outside: 1720 },
    { city: '崇左', inside: 2660, outside: 1940 },
    { city: '百色', inside: 2520, outside: 1680 }
  ],
  kpiList: [
    { label: '年度接待游客', value: '7.36', unit: '亿人次', tone: 'cyan' },
    { label: '旅游综合收入', value: '10240', unit: '亿元', tone: 'amber' },
    { label: '在监测景区', value: '186', unit: '个', tone: 'teal' },
    { label: '同比增长', value: '+9.5', unit: '%', tone: 'amber' }
  ],
  version: 0
})

/** 接口实时状态，供大屏顶部状态徽标显示 */
export const meta = reactive({
  live: false,
  updatedAt: '--:--',
  orderCount: 0,
  todayOrders: 0,
  revenue: 0,
  seedYear: 2026
})

const CITY_CENTER = {
  南宁市: [108.32, 22.82], 柳州市: [109.41, 24.31], 桂林市: [110.3, 25.27],
  梧州市: [111.3, 23.47], 北海市: [109.12, 21.47], 防城港市: [108.35, 21.61],
  钦州市: [108.62, 21.97], 贵港市: [109.6, 23.09], 玉林市: [110.15, 22.63],
  百色市: [106.62, 23.9], 贺州市: [111.55, 24.41], 河池市: [108.06, 24.7],
  来宾市: [109.23, 23.73], 崇左市: [107.35, 22.4]
}

export const cityIndex = computed(() => Object.fromEntries(data.cityHeat.map(c => [c.name, c])))
export const totalHeat = computed(() => data.cityHeat.reduce((s, c) => s + c.value, 0))
export const flowLines = computed(() => data.cityHeat
  .filter(c => c.name !== '南宁市' && CITY_CENTER[c.name])
  .map(c => ({
    fromName: '南宁市', toName: c.name,
    coords: [CITY_CENTER['南宁市'], CITY_CENTER[c.name]]
  })))

/** 拉取后端统计：成功返回 true，失败静默保留基线 */
export async function loadScreenStats() {
  try {
    const d = await request.get('/stats/screen', { silent: true })
    if (!d) return false
    if (d.kpiList?.length) data.kpiList = d.kpiList
    if (d.priceCompare?.length) data.priceCompare = d.priceCompare
    if (d.typeShare?.length) data.typeShare = d.typeShare
    if (d.cityHeat?.length) data.cityHeat = d.cityHeat
    if (d.sourceStructure?.length) data.sourceStructure = d.sourceStructure
    if (d.monthlyTrend) data.monthlyTrend = d.monthlyTrend
    if (d.months?.length) data.months = d.months
    if (d.scenicSpots?.length) {
      data.scenicSpots = d.scenicSpots.map(s => ({
        ...s,
        coord: SPOT_COORDS[s.name] || CITY_CENTER[s.city] || [108.32, 22.82]
      }))
    }
    Object.assign(meta, d.meta || {}, { live: true })
    data.version++
    return true
  } catch {
    meta.live = false
    return false
  }
}
