<template>
  <!-- 数据大屏：独立全屏布局，不显示主站导航/页脚 -->
  <template v-if="isScreen">
    <router-view />
  </template>

  <template v-else>
  <TechBackground />
  <TechHud />

  <header class="nav glass">
    <div class="nav-inner">
      <router-link to="/" class="logo">
        <span class="logo-badge">GX</span>
        <span class="logo-text">广西旅游网</span>
      </router-link>

      <!-- 城市切换 -->
      <el-select v-model="currentCity" class="city-select" style="width: 104px">
        <el-option v-for="c in CITIES" :key="c.key" :label="c.label" :value="c.key" />
      </el-select>

      <!-- 全站搜索：热搜榜 + 实时联想 -->
      <div class="search-box" @focusin="onSearchFocus" @focusout="onSearchBlur">
        <el-select v-model="searchType" class="search-type" style="width: 82px">
          <el-option label="景点" value="spot" />
          <el-option label="美食" value="food" />
          <el-option label="玩法" value="play" />
        </el-select>
        <input
          v-model="keyword"
          class="search-input"
          :placeholder="placeholder"
          @input="onSearchInput"
          @keyup.enter="doSearch"
        />
        <button class="btn-grad search-btn" @click="doSearch">搜索</button>
        <div v-if="showSuggest && suggestions.length" class="suggest-panel glass">
          <div class="suggest-title">{{ keyword.trim() ? '搜索建议' : '🔥 大家都在搜' }}</div>
          <div v-for="(s, i) in suggestions" :key="s.word || s" class="suggest-item"
               @mousedown.prevent="pickSuggest(s)">
            <span class="rank" :class="{ hot: i < 3 }">{{ i + 1 }}</span>
            <span class="sg-text">{{ s.word || s }}</span>
            <small v-if="s.city">{{ s.city }}</small>
            <small v-else-if="s.count">热度 {{ s.count }}</small>
          </div>
        </div>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/travel" class="nav-link">旅游资讯</router-link>
        <router-link :to="{ path: '/play', query: { city } }" class="nav-link">玩转{{ city }}</router-link>
        <router-link to="/service" class="nav-link">服务中心</router-link>
        <router-link to="/shops" class="nav-link">美食推荐</router-link>
        <router-link to="/tours" class="nav-link">跟团游</router-link>
      </nav>

      <div class="user-area">
        <router-link to="/vip" class="vip-entry" title="会员中心">👑 VIP</router-link>
        <template v-if="user">
          <el-dropdown trigger="hover" @command="onCommand">
            <span class="user-chip glass" :title="'当前登录：' + user.nickname">
              <img :src="avatarOf(user)" class="avatar" @error="onAvatarError" />
              <span class="user-name">{{ user.nickname }}</span>
              <span class="caret">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>已登录：{{ user.nickname }}</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin" divided>🛠 平台管理后台</el-dropdown-item>
                <el-dropdown-item v-if="isMerchant" command="merchant" :divided="!isAdmin">📦 商家后台</el-dropdown-item>
                <el-dropdown-item v-else-if="!isAdmin" command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="profile" divided>👤 个人中心</el-dropdown-item>
                <el-dropdown-item command="favorites">⭐ 我的收藏</el-dropdown-item>
                <el-dropdown-item command="vip">👑 会员中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <router-link v-else to="/login" class="login-btn btn-ghost">登录 / 注册</router-link>
      </div>
    </div>
  </header>

  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="$route.fullPath" />
    </transition>
  </router-view>

  <SiteFooter />
  <ServiceChat />
  </template>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { authUser, logout, avatarOf, DEFAULT_AVATAR } from './api.js'
import { CITIES, city, setCity } from './store/city.js'
import TechBackground from './components/TechBackground.vue'
import TechHud from './components/TechHud.vue'
import SiteFooter from './components/SiteFooter.vue'
import ServiceChat from './components/ServiceChat.vue'

const router = useRouter()
const route = useRoute()
// 响应式登录态：登录 / 切换商家 / 退出后，右上角立刻更新，不需要刷新页面
const user = authUser
const isMerchant = computed(() => user.value?.role === 'MERCHANT')
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isScreen = computed(() => route.path === '/screen')

function onAvatarError(e) { e.target.src = DEFAULT_AVATAR }

const currentCity = computed({
  get: () => city.value,
  set: v => {
    setCity(v)
    // 当前在玩法/资讯页时，切换城市自动刷新对应页面
    if (route.path === '/play' || route.path === '/travel') {
      router.replace({ path: route.path, query: { ...route.query, city: v } })
    }
  }
})

const keyword = ref('')
const searchType = ref('spot')
const placeholder = computed(() => ({
  spot: '搜索景点、城市…',
  food: '搜索美食、奶茶…',
  play: '搜索玩法，如 竹筏、夜游…'
}[searchType.value]))

// ===== 热搜榜 + 搜索联想（数据来自真实浏览足迹）=====
const suggestions = ref([])
const showSuggest = ref(false)
let suggestTimer = null

async function loadHotSearch() {
  try { suggestions.value = await request.get('/discovery/hot-search', { silent: true }) || [] } catch { /* 静默 */ }
}
function onSearchFocus() {
  showSuggest.value = true
  if (!keyword.value.trim()) loadHotSearch()
}
function onSearchBlur() {
  setTimeout(() => { showSuggest.value = false }, 160)
}
function onSearchInput() {
  clearTimeout(suggestTimer)
  const q = keyword.value.trim()
  if (!q) return loadHotSearch()
  suggestTimer = setTimeout(async () => {
    try { suggestions.value = await request.get('/discovery/suggest', { params: { q }, silent: true }) || [] }
    catch { suggestions.value = [] }
  }, 220)
}
function pickSuggest(s) {
  keyword.value = s.word || String(s)
  showSuggest.value = false
  doSearch()
}

function doSearch() {
  showSuggest.value = false
  const q = keyword.value.trim()
  if (searchType.value === 'spot') router.push({ path: '/spots', query: { keyword: q } })
  else if (searchType.value === 'food') router.push({ path: '/shops', query: { keyword: q } })
  else router.push({ path: '/play', query: { keyword: q, city: city.value } })
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    logout()
    location.href = '/'
  } else if (cmd === 'admin') {
    router.push('/admin')
  } else if (cmd === 'merchant') {
    router.push('/merchant')
  } else if (cmd === 'vip') {
    router.push('/vip')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'favorites') {
    router.push('/favorites')
  } else {
    router.push('/orders')
  }
}

onMounted(() => {
  if (route.query.denied === '1') {
    ElMessage.warning('该后台仅对应角色可访问，已返回首页')
    router.replace({ path: '/', query: {} })
  }
})
</script>

<style scoped>
.nav {
  position: sticky;
  top: 12px;
  z-index: 100;
  margin: 12px auto 0;
  max-width: 1240px;
  width: calc(100% - 40px);
  border-radius: 16px;
}
.nav-inner { display: flex; align-items: center; gap: 12px; padding: 10px 18px; }
.logo {
  display: flex; align-items: center; gap: 10px;
  font-weight: 700; font-size: 16px; color: var(--txt);
  text-decoration: none; white-space: nowrap;
}
.logo-badge {
  display: grid; place-items: center; width: 36px; height: 36px;
  border-radius: 10px; background: var(--grad); color: #04121f; font-weight: 800;
}
.city-select {
  flex-shrink: 0;
}
.city-select :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.07) !important;
  box-shadow: none !important;
  border: 1px solid var(--border-glass);
}
.search-box {
  position: relative;
  flex: 1;
  display: flex; align-items: center; gap: 8px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-glass);
  border-radius: 12px; padding: 6px 8px 6px 6px; min-width: 200px;
}
.suggest-panel {
  position: absolute; top: calc(100% + 8px); left: 0; right: 0; z-index: 200;
  border-radius: 12px; padding: 10px 8px; max-height: 320px; overflow-y: auto;
  background: rgba(9, 17, 32, 0.96); border: 1px solid var(--border-glass);
}
.suggest-title { color: var(--txt-sub); font-size: 12px; padding: 2px 10px 8px; }
.suggest-item {
  display: flex; align-items: center; gap: 10px; padding: 7px 10px;
  border-radius: 8px; cursor: pointer; font-size: 13.5px; color: var(--txt);
}
.suggest-item:hover { background: rgba(56, 189, 248, 0.14); }
.suggest-item .rank {
  display: grid; place-items: center; width: 20px; height: 20px; flex-shrink: 0;
  border-radius: 5px; background: rgba(255,255,255,.08); color: var(--txt-sub); font-size: 11px;
}
.suggest-item .rank.hot { background: rgba(248, 113, 113, .22); color: #fca5a5; }
.sg-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.suggest-item small { color: var(--txt-sub); font-size: 11.5px; flex-shrink: 0; }
.search-type { flex-shrink: 0; }
.search-box :deep(.el-select__wrapper) { background: transparent !important; box-shadow: none !important; }
.search-input {
  flex: 1; min-width: 0; background: transparent; border: none; outline: none;
  color: var(--txt); font-size: 14px;
}
.search-btn { flex-shrink: 0; padding: 7px 16px; }
.nav-links { display: flex; gap: 2px; flex-shrink: 0; }
.nav-link {
  color: var(--txt-sub); text-decoration: none; padding: 8px 10px;
  border-radius: 10px; font-size: 13.5px; white-space: nowrap;
}
.nav-link:hover, .nav-link.router-link-active { color: var(--txt); background: rgba(255, 255, 255, 0.07); }
.user-area { display: flex; align-items: center; gap: 8px; }
.vip-entry {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 7px 13px; border-radius: 12px; font-size: 13px; font-weight: 700;
  color: #f59e0b; text-decoration: none;
  background: linear-gradient(120deg, rgba(251, 191, 36, 0.14), rgba(245, 158, 11, 0.1));
  border: 1px solid rgba(251, 191, 36, 0.4);
  white-space: nowrap;
}
.vip-entry:hover { box-shadow: 0 0 14px rgba(251, 191, 36, 0.35); }
.user-chip {
  display: flex; align-items: center; gap: 8px; padding: 6px 12px;
  border-radius: 12px; cursor: pointer; font-size: 14px; color: var(--txt); outline: none;
}
.avatar { width: 26px; height: 26px; border-radius: 50%; object-fit: cover; background: rgba(255,255,255,0.1); }
.user-name { max-width: 96px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.caret { font-size: 11px; color: var(--txt-sub); margin-left: 2px; }
.login-btn { padding: 8px 16px; font-size: 14px; text-decoration: none; }
@media (max-width: 1080px) {
  .nav-links { display: none; }
}
@media (max-width: 760px) {
  .logo-text { display: none; }
  .city-select { width: 92px !important; }
}
</style>
