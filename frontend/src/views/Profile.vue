<template>
  <main class="page profile-page">
    <div class="head-row">
      <div>
        <h2 class="page-title">👤 个人中心</h2>
        <p class="page-sub">资料管理 · 我的收藏 · 浏览足迹 · 投诉工单</p>
      </div>
      <el-tag :type="roleType" effect="dark" round size="large">{{ roleText }}</el-tag>
    </div>

    <!-- 用户概览 -->
    <div class="profile-hero glass">
      <img :src="avatarOf(form.user)" class="big-avatar" @error="onAvatarError" />
      <div class="hero-main">
        <h3>{{ form.user?.nickname || '游客' }}</h3>
        <p>账号 {{ form.user?.username }} · {{ form.user?.vipLevel ? '👑 VIP 会员' : '普通用户' }}</p>
        <p class="hero-hint">{{ form.user?.email || '未填写邮箱' }} · {{ form.user?.phone || '未填写手机号' }}</p>
      </div>
      <div class="stat-grid">
        <div v-for="s in stats" :key="s.label" class="stat glass">
          <span>{{ s.label }}</span><b class="grad-text">{{ s.value }}</b>
        </div>
      </div>
    </div>

    <el-tabs v-model="tab" class="profile-tabs" @tab-change="onTab">
      <el-tab-pane label="个人资料" name="info" />
      <el-tab-pane label="我的收藏" name="favorites" />
      <el-tab-pane label="浏览足迹" name="browse" />
      <el-tab-pane label="投诉工单" name="complaints" />
    </el-tabs>

    <!-- 个人资料 -->
    <div v-if="tab === 'info'" class="panel glass">
      <div class="panel-title">基本信息</div>
      <el-form label-width="100px" class="form">
        <el-form-item label="昵称"><el-input v-model="profile.nickname" maxlength="32" /></el-form-item>
        <el-form-item label="头像地址"><el-input v-model="profile.avatar" placeholder="/images/avatar-default.svg" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profile.phone" maxlength="20" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profile.email" maxlength="64" /></el-form-item>
        <el-form-item v-if="isMerchant" label="主体名称"><el-input v-model="profile.merchantName" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveProfile">保存资料</el-button></el-form-item>
      </el-form>
      <div class="panel-title">修改密码</div>
      <el-form label-width="100px" class="form">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item><el-button @click="changePwd">修改密码</el-button></el-form-item>
      </el-form>
    </div>

    <!-- 我的收藏 -->
    <div v-else-if="tab === 'favorites'" class="panel glass">
      <div class="panel-title">我的收藏 / 想去 <small>共 {{ favorites.length }} 项</small></div>
      <div v-if="favorites.length" class="card-grid">
        <div v-for="f in favorites" :key="f.id" class="mini-card glass">
          <img :src="f.image || DEFAULT_AVATAR" class="mini-img" />
          <div class="mini-body">
            <b>{{ f.title }}</b>
            <small>{{ f.city || '广西' }} · ¥{{ f.price }}</small>
            <div class="mini-ops">
              <router-link :to="f.link" class="lnk">查看</router-link>
              <button class="btn-ghost tiny" @click="unfav(f)">取消收藏</button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有收藏，去挑几个想去的地方吧">
        <router-link to="/spots" class="lnk">浏览景点 →</router-link>
      </el-empty>
    </div>

    <!-- 浏览足迹 -->
    <div v-else-if="tab === 'browse'" class="panel glass">
      <div class="panel-title">最近浏览 <small>推荐算法会参考这些记录</small></div>
      <div v-if="browse.length" class="browse-list">
        <router-link v-for="b in browse" :key="b.id" :to="b.link" class="browse-item glass">
          <img :src="b.image || DEFAULT_AVATAR" />
          <div><b>{{ b.title }}</b><small>{{ b.city || '广西' }} · {{ typeText(b.itemType) }} · {{ fmt(b.createdAt) }}</small></div>
          <span class="arrow">→</span>
        </router-link>
      </div>
      <el-empty v-else description="暂无浏览记录" />
    </div>

    <!-- 投诉工单 -->
    <div v-else class="panel glass">
      <div class="panel-title">我的投诉工单 <small>平台管理员会处理并回复</small></div>
      <div class="complaint-form">
        <el-select v-model="newComplaint.category" style="width: 150px">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="newComplaint.targetName" placeholder="涉及对象（店铺/线路/订单）" style="width: 220px" />
        <el-input v-model="newComplaint.content" placeholder="请描述遇到的问题" style="flex: 1; min-width: 220px" maxlength="500" />
        <el-button type="primary" @click="submitComplaint">提交工单</el-button>
      </div>
      <div v-if="complaints.length" class="complaint-list">
        <div v-for="c in complaints" :key="c.id" class="complaint-item glass">
          <div class="ci-head">
            <el-tag :type="statusType(c.status)" effect="dark" round size="small">{{ statusText(c.status) }}</el-tag>
            <b>{{ c.category }} · {{ c.targetName || '平台' }}</b>
            <small>{{ fmt(c.createdAt) }}</small>
          </div>
          <p class="ci-content">{{ c.content }}</p>
          <p v-if="c.reply" class="ci-reply">平台回复：{{ c.reply }}</p>
        </div>
      </div>
      <el-empty v-else description="还没有工单" :image-size="70" />
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { authUser, avatarOf, DEFAULT_AVATAR } from '../api.js'

const route = useRoute()
const router = useRouter()
const CATEGORIES = ['服务咨询', '商品问题', '退款', '商家投诉', '系统问题', '其他']
const user = authUser
const tab = ref(route.path === '/favorites' ? 'favorites' : 'info')
const form = reactive({ user: {} })
const profile = reactive({ nickname: '', avatar: '', phone: '', email: '', merchantName: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })
const favorites = ref([])
const browse = ref([])
const complaints = ref([])
const newComplaint = reactive({ category: '服务咨询', targetName: '', content: '' })

const isMerchant = computed(() => user.value?.role === 'MERCHANT')
const roleText = computed(() => ({ CUSTOMER: '游客', MERCHANT: '商家', ADMIN: '管理员' }[user.value?.role] || '游客'))
const roleType = computed(() => ({ CUSTOMER: 'success', MERCHANT: 'warning', ADMIN: 'danger' }[user.value?.role] || 'success'))
const stats = computed(() => [
  { label: '累计订单', value: form.orderCount ?? 0 },
  { label: '已支付', value: form.paidOrders ?? 0 },
  { label: '累计消费', value: '¥' + (form.totalSpent ?? 0) },
  { label: '收藏', value: form.favoriteCount ?? 0 },
  { label: '足迹', value: form.browseCount ?? 0 },
  { label: '工单', value: form.complaintCount ?? 0 }
])

function onAvatarError(e) { e.target.src = DEFAULT_AVATAR }
function fmt(t) { return t ? String(t).slice(0, 16).replace('T', ' ') : '' }
function typeText(t) { return { SPOT: '景点', PRODUCT: '商品', TOUR: '线路', SHOP: '店铺' }[t] || '内容' }
function statusText(s) { return { PENDING: '待受理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' }[s] || s }
function statusType(s) { return { PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'success', REJECTED: 'info' }[s] || 'info' }

async function loadProfile() {
  const d = await request.get('/user/profile')
  Object.assign(form, d)
  form.user = d.user || {}
  Object.assign(profile, {
    nickname: d.user?.nickname || '', avatar: d.user?.avatar || '',
    phone: d.user?.phone || '', email: d.user?.email || '',
    merchantName: d.user?.merchantName || ''
  })
}
async function loadFavorites() { favorites.value = await request.get('/user/favorites') || [] }
async function loadBrowse() { browse.value = await request.get('/user/browse') || [] }
async function loadComplaints() { complaints.value = await request.get('/user/complaints') || [] }

async function saveProfile() {
  const u = await request.put('/user/profile', profile)
  ElMessage.success('资料已保存')
  // 同步右上角登录态
  const flat = { ...(authUser.value || {}), ...u }
  localStorage.setItem('gxtourism_user', JSON.stringify(flat))
  authUser.value = flat
  form.user = u
}
async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  await request.put('/user/password', pwd)
  ElMessage.success('密码已修改，下次登录请用新密码')
  pwd.oldPassword = ''; pwd.newPassword = ''
}
async function unfav(f) {
  await request.post('/user/favorites/toggle', { itemType: f.itemType, itemId: f.itemId })
  ElMessage.success('已取消收藏')
  loadFavorites(); loadProfile()
}
async function submitComplaint() {
  if (!newComplaint.content.trim()) return ElMessage.warning('请描述遇到的问题')
  await request.post('/user/complaints', newComplaint)
  ElMessage.success('工单已提交，平台管理员会尽快处理')
  newComplaint.content = ''; newComplaint.targetName = ''
  loadComplaints(); loadProfile()
}

function onTab(name) {
  if (name === 'favorites') loadFavorites()
  else if (name === 'browse') loadBrowse()
  else if (name === 'complaints') loadComplaints()
}
watch(() => route.path, p => { if (p === '/favorites') tab.value = 'favorites' })
onMounted(() => {
  loadProfile()
  if (tab.value === 'favorites') loadFavorites()
})
</script>

<style scoped>
.profile-page { padding-bottom: 40px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
.profile-hero { display: grid; grid-template-columns: 84px 1.2fr 2fr; gap: 22px; align-items: center; padding: 20px 24px; margin: 18px 0 20px; border-radius: 16px; }
.big-avatar { width: 84px; height: 84px; border-radius: 50%; object-fit: cover; border: 2px solid var(--cyan); }
.hero-main h3 { margin: 0 0 8px; font-size: 20px; }
.hero-main p { margin: 4px 0; color: var(--txt-sub); font-size: 13px; }
.hero-hint { opacity: .75; }
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.stat { padding: 10px 12px; border-radius: 10px; }
.stat span { display: block; color: var(--txt-sub); font-size: 11.5px; }
.stat b { font-size: 17px; }
.profile-tabs { margin-bottom: 12px; }
.panel { padding: 20px 24px; border-radius: 14px; }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; padding-left: 10px; border-left: 3px solid var(--cyan); }
.panel-title small { color: var(--txt-sub); font-weight: 400; font-size: 12px; margin-left: 8px; }
.form { max-width: 560px; }
.card-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.mini-card { overflow: hidden; border-radius: 12px; }
.mini-img { width: 100%; height: 110px; object-fit: cover; display: block; }
.mini-body { padding: 10px 12px; }
.mini-body b { display: block; font-size: 13.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mini-body small { color: var(--txt-sub); font-size: 12px; }
.mini-ops { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.tiny { padding: 3px 8px; font-size: 11.5px; }
.lnk { color: var(--cyan); text-decoration: none; font-size: 13px; }
.browse-list { display: flex; flex-direction: column; gap: 10px; }
.browse-item { display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 10px; text-decoration: none; color: var(--txt); }
.browse-item img { width: 64px; height: 46px; object-fit: cover; border-radius: 8px; }
.browse-item div { flex: 1; }
.browse-item b { display: block; font-size: 13.5px; }
.browse-item small { color: var(--txt-sub); font-size: 12px; }
.arrow { color: var(--cyan); }
.complaint-form { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }
.complaint-list { display: flex; flex-direction: column; gap: 10px; }
.complaint-item { padding: 12px 16px; border-radius: 10px; }
.ci-head { display: flex; align-items: center; gap: 12px; }
.ci-head b { font-size: 13.5px; }
.ci-head small { color: var(--txt-sub); margin-left: auto; }
.ci-content { margin: 8px 0 0; font-size: 13px; color: var(--txt-sub); }
.ci-reply { margin: 8px 0 0; font-size: 13px; color: #4ade80; }
@media (max-width: 900px) {
  .profile-hero { grid-template-columns: 70px 1fr; }
  .stat-grid { grid-column: 1 / -1; }
  .card-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
