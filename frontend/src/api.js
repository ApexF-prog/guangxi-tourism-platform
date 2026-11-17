import axios from 'axios'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

const USER_KEY = 'gxtourism_user'

/** 全部内置演示账号统一密码（仅本地演示环境） */
export const DEMO_PASSWORD = '123456'
export const DEFAULT_AVATAR = '/images/avatar-default.svg'

export function currentUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** 响应式登录态：登录 / 退出 / 切换商家后，右上角与侧边栏立即同步 */
export const authUser = ref(currentUser())

/** 头像兜底：外链或空头像一律用本地默认图，避免空白 */
export function avatarOf(user) {
  const a = user?.avatar
  return a && !a.startsWith('http') && a.trim() ? a : DEFAULT_AVATAR
}

export function saveUser(data) {
  // 兼容 {token, user:{...}} 与纯 user 两种入参，统一拍平存储，
  // 保证 currentUser() 直接就是用户对象（含 token）
  const flat = data && data.user && data.token ? { ...data.user, token: data.token } : data
  localStorage.setItem(USER_KEY, JSON.stringify(flat))
  authUser.value = flat
  window.dispatchEvent(new Event('auth-changed'))
}

export function logout() {
  localStorage.removeItem(USER_KEY)
  authUser.value = null
  window.dispatchEvent(new Event('auth-changed'))
}

/**
 * 需要登录时统一跳登录页，并带上原地址，登录成功后自动回到刚才的页面。
 * 所有「购买 / 预订 / 报名 / 收藏 / 评价」入口都应调用这个函数，行为完全一致。
 */
export function gotoLogin(from) {
  const target = from || (location.pathname + location.search)
  const redirect = encodeURIComponent(target)
  location.href = '/login?redirect=' + redirect
}

/** 未登录点击业务按钮时的统一守卫，返回 true 表示已拦截 */
export function requireLogin(from) {
  if (currentUser()) return false
  ElMessage.warning('请先登录，登录后自动回到当前页面')
  gotoLogin(from)
  return true
}

/** 静默记录浏览足迹，作为个性化推荐的数据源；未登录或失败时完全不影响页面 */
export function recordBrowse(itemType, itemId, city = '') {
  if (!currentUser()) return
  request.post('/user/browse', { itemType, itemId, city }, { silent: true }).catch(() => {})
}

// 多标签页之间同步登录态
window.addEventListener('storage', e => {
  if (e.key === USER_KEY) authUser.value = currentUser()
})

request.interceptors.request.use(config => {
  const user = currentUser()
  if (user?.token) config.headers.Authorization = `Bearer ${user.token}`
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res && res.code === 200) return res.data
    const error = new Error(res?.msg || '请求失败')
    error.business = true
    if (!response.config?.silent) ElMessage.error(error.message)
    return Promise.reject(error)
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      if (!location.pathname.startsWith('/login')) {
        ElMessage.warning('请先登录')
        gotoLogin()
      }
    } else if (status === 403) {
      // 后端角色门禁（RoleInterceptor）拒绝：前端一律退回首页，避免停留在无权限的后台
      ElMessage.error(error.response?.data?.msg || '当前账号无权访问该后台')
      if (location.pathname.startsWith('/admin') || location.pathname.startsWith('/merchant')) {
        setTimeout(() => { location.href = '/?denied=1' }, 600)
      }
    }
    return Promise.reject(error)
  }
)

export default request
