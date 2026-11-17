import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import './styles/global.css'
import { initVisualFx } from './utils/fx.js'
import { currentUser, gotoLogin } from './api.js'
import progress from './utils/progress.js'
import { installRuntimeGuard, notify } from './utils/guard.js'

// 路由级懒加载：22 个视图不再一次性打进主包。
// 首屏只下载首页 + 公共库；ECharts 只在大屏 / 商家收入页才加载。
const Home = () => import('./views/Home.vue')
const Spots = () => import('./views/Spots.vue')
const SpotDetail = () => import('./views/SpotDetail.vue')
const Shops = () => import('./views/Shops.vue')
const ShopDetail = () => import('./views/ShopDetail.vue')
const Pay = () => import('./views/Pay.vue')
const PayReturn = () => import('./views/PayReturn.vue')
const Orders = () => import('./views/Orders.vue')
const Login = () => import('./views/Login.vue')
const Travel = () => import('./views/Travel.vue')
const Play = () => import('./views/Play.vue')
const Service = () => import('./views/Service.vue')
const Vip = () => import('./views/Vip.vue')
const Tours = () => import('./views/Tours.vue')
const Screen = () => import('./views/Screen.vue')
const Profile = () => import('./views/Profile.vue')
const NotFound = () => import('./views/NotFound.vue')
const MerchantLayout = () => import('./views/merchant/MerchantLayout.vue')
const Dashboard = () => import('./views/merchant/Dashboard.vue')
const Products = () => import('./views/merchant/Products.vue')
const MerchantOrders = () => import('./views/merchant/MerchantOrders.vue')
const Income = () => import('./views/merchant/Income.vue')
const Admin = () => import('./views/admin/Admin.vue')

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: Home, meta: { title: '首页' } },
    { path: '/travel', component: Travel, meta: { title: '广西旅行' } },
    { path: '/play', component: Play, meta: { title: '玩转广西' } },
    { path: '/service', component: Service, meta: { title: '旅游服务' } },
    { path: '/spots', component: Spots, meta: { title: '景点大全' } },
    { path: '/spots/:id', component: SpotDetail, meta: { title: '景点详情' } },
    { path: '/shops', component: Shops, meta: { title: '特色美食' } },
    { path: '/shops/:id', component: ShopDetail, meta: { title: '店铺详情' } },
    { path: '/pay/:orderNo', component: Pay, meta: { title: '收银台', requiresAuth: true } },
    { path: '/pay/return', component: PayReturn, meta: { title: '支付结果' } },
    { path: '/orders', component: Orders, meta: { title: '我的订单', requiresAuth: true } },
    { path: '/login', component: Login, meta: { title: '登录' } },
    { path: '/vip', component: Vip, meta: { title: 'VIP 会员' } },
    { path: '/tours', component: Tours, meta: { title: '跟团游' } },
    { path: '/screen', component: Screen, meta: { title: '数据大屏' } },
    { path: '/profile', component: Profile, meta: { title: '个人中心', requiresAuth: true } },
    { path: '/favorites', component: Profile, meta: { title: '想去清单', requiresAuth: true } },
    { path: '/admin', component: Admin, meta: { title: '平台后台', requiresAuth: true, role: 'ADMIN' } },
    {
      path: '/merchant',
      component: MerchantLayout,
      meta: { title: '商家后台', requiresAuth: true, role: 'MERCHANT' },
      children: [
        { path: '', component: Dashboard, meta: { title: '经营概览' } },
        { path: 'products', component: Products, meta: { title: '商品管理' } },
        { path: 'orders', component: MerchantOrders, meta: { title: '订单管理' } },
        { path: 'income', component: Income, meta: { title: '收入报表' } }
      ]
    },
    // 兜底 404：地址打错或旧链接不会再白屏
    { path: '/:pathMatch(.*)*', component: NotFound, meta: { title: '页面不存在' } }
  ]
})

/**
 * 三角色路由门禁（体验层，真正的权限在后端 RoleInterceptor）：
 * 游客 → 看不到商家后台 / 平台后台；商家 → 只能进 /merchant；管理员 → 只能进 /admin。
 * 未登录访问任何需登录页面，一律跳登录页并带上原地址，登录后自动回来。
 */
router.beforeEach((to, from, next) => {
  // 懒加载后首次进入某页会多一次请求，进度条给出即时反馈
  progress.start()
  const user = currentUser()
  if (to.meta.requiresAuth && !user) {
    ElMessage.warning('请先登录')
    progress.done()
    gotoLogin(to.fullPath)
    return next(false)
  }
  if (to.meta.role && user && user.role !== to.meta.role) {
    ElMessage.error('当前账号无权访问该后台，请切换对应角色登录')
    progress.done()
    return next({ path: '/', query: { denied: '1' } })
  }
  next()
})

/**
 * 导航收尾：关进度条 + 按路由 meta 更新标签页标题。
 * 多标签浏览、收藏、分享时用户能一眼分清页面。
 */
router.afterEach((to) => {
  progress.done()
  const t = to.meta && to.meta.title
  document.title = t ? t + ' · 广西旅游网' : '广西旅游网 · 发现山水之美'
})

/**
 * 懒加载分片下载失败（校园网常见）：自动整页重试一次，避免停在空白页。
 * sessionStorage 记录 1 分钟内的重试次数，最多 2 次，防止死循环刷新。
 */
const RELOAD_KEY = 'gx_chunk_reload'
function allowReload() {
  try {
    const now = Date.now()
    const raw = JSON.parse(sessionStorage.getItem(RELOAD_KEY) || '[]')
    const recent = (Array.isArray(raw) ? raw : []).filter(t => now - t < 60000)
    if (recent.length >= 2) return false
    recent.push(now)
    sessionStorage.setItem(RELOAD_KEY, JSON.stringify(recent))
    return true
  } catch (e) { return false }
}

router.onError((err) => {
  progress.done()
  const msg = String((err && err.message) || err)
  const chunkFail = /dynamically imported module|ChunkLoadError|Loading chunk|Importing a module script failed/i.test(msg)
  if (chunkFail && allowReload()) {
    notify('页面资源加载失败，正在自动重试…', 'warn')
    setTimeout(() => location.reload(), 600)
    return
  }
  notify('页面加载失败，请检查网络后重试', 'error', [
    { label: '重试', onClick: () => location.reload() },
    { label: '回首页', onClick: () => { location.href = '/' } },
  ])
  console.error('[GX] 路由加载失败', err)
})

const app = createApp(App)
installRuntimeGuard(app)
app.use(router)
// 中文语言包：日期选择器、分页、空状态等全部中文
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
initVisualFx()
