<template>
  <main class="page login-page">
    <div class="login-card glass">
      <!-- QQ 式头像 -->
      <div class="avatar-ring">
        <div class="big-avatar">
          <span class="avatar-fallback">GX</span>
        </div>
        <span class="avatar-badge">Hi</span>
      </div>
      <h1 class="grad-text" style="text-align: center; margin: 0 0 4px; font-size: 22px">广西旅游网</h1>
      <p style="text-align: center; color: var(--txt-sub); font-size: 12.5px; margin: 0 0 18px">
        发现山水之美 · 门票 / 美食 / 奶茶 / 团购
      </p>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <div class="account-wrap">
            <el-input
              v-model="loginForm.username"
              placeholder="账号 / 手机号（点击可看历史登录账号）"
              size="large"
              style="margin-bottom: 14px"
              @focus="showHistory = true"
              @blur="hideHistorySoon"
            />
            <!-- 历史账号下拉 -->
            <div v-if="showHistory && history.length" class="history glass">
              <div class="history-head">最近登录的账号</div>
              <div v-for="h in history" :key="h.username" class="history-item"
                   @mousedown.prevent="useHistory(h)">
                <span class="h-avatar">{{ h.nickname?.slice(0, 1) || '游' }}</span>
                <div class="h-info">
                  <b>{{ h.username }}</b>
                  <small>{{ h.nickname }} · {{ roleLabel(h.role) }}</small>
                </div>
                <span class="h-time">{{ h.time }}</span>
                <span class="h-del" @mousedown.prevent.stop="removeHistory(h.username)">×</span>
              </div>
            </div>
          </div>
          <el-input v-model="loginForm.password" type="password" placeholder="输入密码" size="large" show-password
                    style="margin-bottom: 12px" @keyup.enter="doLogin" />

          <!-- 自动登录 / 记住密码 -->
          <div class="option-row">
            <label class="opt" @click.prevent="autoLogin = !autoLogin">
              <span class="ck" :class="{ on: autoLogin }"><i v-if="autoLogin">✓</i></span> 自动登录
            </label>
            <label class="opt" @click.prevent="rememberPwd = !rememberPwd">
              <span class="ck" :class="{ on: rememberPwd }"><i v-if="rememberPwd">✓</i></span> 记住密码
            </label>
          </div>

          <button class="btn-grad" style="width: 100%; padding: 12px; margin-top: 12px" @click="doLogin">登 录</button>
          <p v-if="history.length" class="clear-line">
            <a href="#" @click.prevent="clearHistory">清空历史账号</a>
          </p>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-radio-group v-model="regForm.role" style="width: 100%; margin-bottom: 14px">
            <el-radio-button value="CUSTOMER" style="width: 50%">我是游客</el-radio-button>
            <el-radio-button value="MERCHANT" style="width: 50%">我是商家</el-radio-button>
          </el-radio-group>

          <!-- +86 手机号 -->
          <div class="phone-row">
            <span class="prefix">+86 <i class="caret">▾</i></span>
            <el-input v-model="regForm.username" placeholder="输入手机号" size="large" class="phone-input" />
          </div>
          <!-- 短信验证码 -->
          <div class="sms-row">
            <el-input v-model="smsCode" placeholder="短信验证码" size="large" style="flex: 1" />
            <button class="sms-btn" :disabled="countdown > 0" @click="sendSms">
              {{ countdown > 0 ? countdown + 's 后重发' : '发送验证码' }}
            </button>
          </div>
          <el-input v-model="regForm.nickname" placeholder="输入昵称" size="large" style="margin: 14px 0" />
          <el-input v-model="regForm.password" type="password" placeholder="输入密码（至少 6 位）" size="large" show-password
                    style="margin-bottom: 12px" @keyup.enter="doRegister" />

          <label class="opt agree" @click.prevent="agree = !agree">
            <span class="ck" :class="{ on: agree }"><i v-if="agree">✓</i></span>
            已阅读并同意 <a class="lnk" @click.stop>《服务协议》</a> 和 <a class="lnk" @click.stop>《隐私政策》</a>
          </label>

          <button class="btn-grad" style="width: 100%; padding: 12px; margin-top: 12px" @click="doRegister">立即注册</button>
        </el-tab-pane>
      </el-tabs>

      <div class="demo-tip glass">
        <b>演示账号（点击自动填入，统一密码 {{ DEMO_PASSWORD }}）</b>
        <p @click="fill('13800000001')">游客：13800000001 · 演示用户</p>
        <p @click="fill('merchant001')">商家：merchant001 · 龙脊人家老板</p>
        <p @click="fill('admin')">管理员：admin · 平台管理后台</p>
        <a class="all-link" @click="showAccounts = true">查看全部演示账号（{{ demoAccounts.length }} 个）→</a>
      </div>
    </div>

    <el-dialog v-model="showAccounts" title="全部演示账号 · 统一密码 123456" width="600px">
      <p class="dlg-tip">点击任意一行即可自动填入账号密码。商家之间数据完全隔离，只能看和改自己店铺的商品与订单。</p>
      <div class="acct-head"><span>身份</span><span>账号</span><span>密码</span><span>说明</span></div>
      <div v-for="a in demoAccounts" :key="a.username" class="acct-row" @click="fill(a.username); showAccounts = false">
        <span><b :class="'tag-' + a.role.toLowerCase()">{{ roleLabel(a.role) }}</b></span>
        <span class="mono">{{ a.username }}</span>
        <span class="mono">{{ DEMO_PASSWORD }}</span>
        <span class="acct-desc">{{ a.nickname }}<small v-if="a.shops?.length"> · {{ a.shops.join('、') }}</small></span>
      </div>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { saveUser, DEMO_PASSWORD } from '../api.js'

const router = useRouter()
const route = useRoute()
const tab = ref('login')
const loginForm = ref({ username: '', password: '' })
const regForm = ref({ role: 'CUSTOMER', nickname: '', username: '', password: '' })

// QQ 式选项
const autoLogin = ref(false)
const rememberPwd = ref(false)
const AUTO_KEY = 'gx_auto_login'
const REMEMBER_KEY = 'gx_remember_pwd'
const agree = ref(false)

// 短信验证码（演示：本地生成并提示）
const smsCode = ref('')
const sentCode = ref('')
const countdown = ref(0)
let cdTimer = null

const HISTORY_KEY = 'gx_login_history'
const history = ref([])
const showHistory = ref(false)
let blurTimer = null

// 演示账号一览：数据来自后端数据库，不写死在页面里
const demoAccounts = ref([])
const showAccounts = ref(false)
function roleLabel(r) {
  return r === 'ADMIN' ? '管理员' : r === 'MERCHANT' ? '商家' : '游客'
}

function loadHistory() {
  try {
    history.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
  } catch { history.value = [] }
}

function rememberAccount(user) {
  const list = history.value.filter(h => h.username !== user.username)
  list.unshift({
    username: user.username,
    nickname: user.nickname || user.username,
    role: user.role || 'CUSTOMER',
    time: new Date().toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  })
  history.value = list.slice(0, 5)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
}

function useHistory(h) {
  loginForm.value.username = h.username
  const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || '{}')
  if (saved[h.username]) loginForm.value.password = saved[h.username]
  showHistory.value = false
}

function removeHistory(username) {
  history.value = history.value.filter(h => h.username !== username)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
}

function clearHistory() {
  history.value = []
  localStorage.removeItem(HISTORY_KEY)
  ElMessage.success('已清空历史账号')
}

function hideHistorySoon() {
  clearTimeout(blurTimer)
  blurTimer = setTimeout(() => { showHistory.value = false }, 160)
}

function fill(username) {
  loginForm.value.username = username
  loginForm.value.password = DEMO_PASSWORD
}

function enter(data) {
  saveUser(data)
  rememberAccount(data.user)
  // 记住密码
  const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || '{}')
  if (rememberPwd.value) saved[loginForm.value.username] = loginForm.value.password
  else delete saved[loginForm.value.username]
  localStorage.setItem(REMEMBER_KEY, JSON.stringify(saved))
  localStorage.setItem(AUTO_KEY, rememberPwd.value && autoLogin.value ? loginForm.value.username : '')
  ElMessage.success('欢迎回来，' + data.user.nickname)

  const role = data.user.role
  // 新注册商家未通过平台审核前，不进入商家后台（后端也会挡住）
  const pendingMerchant = role === 'MERCHANT' && data.user.auditStatus
    && !['APPROVED', ''].includes(data.user.auditStatus)
  if (pendingMerchant) {
    ElMessage.info('商家入驻申请待平台管理员审核，审核通过后即可进入商家后台')
    return router.push('/')
  }

  // 从哪里被拦下来的，登录成功后就回到哪里（例如点「购买」→ 登录 → 回商品页）
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('/login')) {
    const wrongAdmin = redirect.startsWith('/admin') && role !== 'ADMIN'
    const wrongMerchant = redirect.startsWith('/merchant') && role !== 'MERCHANT'
    if (!wrongAdmin && !wrongMerchant) return router.push(redirect)
  }
  router.push(role === 'ADMIN' ? '/admin' : role === 'MERCHANT' ? '/merchant' : '/')
}

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    return ElMessage.warning('请输入账号和密码')
  }
  try {
    const data = await request.post('/auth/login', loginForm.value)
    enter(data)
  } catch { /* 拦截器已提示 */ }
}

function sendSms() {
  const phone = regForm.value.username?.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    return ElMessage.warning('请先输入正确的 11 位手机号')
  }
  sentCode.value = String(Math.floor(100000 + Math.random() * 900000))
  ElMessage.success('验证码已发送（演示环境验证码：' + sentCode.value + '）')
  countdown.value = 60
  clearInterval(cdTimer)
  cdTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(cdTimer)
  }, 1000)
}

async function doRegister() {
  const phone = regForm.value.username?.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) return ElMessage.warning('请输入正确的 11 位手机号')
  if (!new RegExp('^' + sentCode.value + '$').test(smsCode.value?.trim() || '')) {
    return ElMessage.warning('请输入正确的短信验证码')
  }
  if (!regForm.value.nickname?.trim()) return ElMessage.warning('请输入昵称')
  if (!regForm.value.password || regForm.value.password.length < 6) return ElMessage.warning('密码至少 6 位')
  if (!agree.value) return ElMessage.warning('请先阅读并同意《服务协议》和《隐私政策》')
  try {
    const data = await request.post('/auth/register', regForm.value)
    ElMessage.success('注册成功')
    enter(data)
  } catch { /* 拦截器已提示 */ }
}

onMounted(() => {
  loadHistory()
  request.get('/auth/demo-accounts').then(d => { demoAccounts.value = d || [] }).catch(() => {})
  // 记住密码 / 自动登录回填
  try {
    const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || '{}')
    const autoUser = localStorage.getItem(AUTO_KEY)
    const lastUser = Object.keys(saved)[0]
    if (autoUser && saved[autoUser]) {
      loginForm.value.username = autoUser
      loginForm.value.password = saved[autoUser]
      rememberPwd.value = true
      autoLogin.value = true
      // 自动登录
      request.post('/auth/login', loginForm.value).then(enter).catch(() => {})
    } else if (lastUser) {
      loginForm.value.username = lastUser
      loginForm.value.password = saved[lastUser]
      rememberPwd.value = true
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.login-page { display: flex; justify-content: center; padding-top: 36px; }
.login-card { width: 100%; max-width: 430px; padding: 26px 36px 30px; }

/* QQ 式头像 */
.avatar-ring { position: relative; width: 84px; margin: 0 auto 8px; }
.big-avatar {
  width: 84px; height: 84px; border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #818cf8);
  display: grid; place-items: center;
  box-shadow: 0 0 0 4px rgba(94, 234, 255, 0.18), 0 10px 26px rgba(56, 189, 248, 0.35);
  overflow: hidden;
}
.avatar-fallback { font-size: 26px; font-weight: 800; color: #04121f; letter-spacing: 1px; }
.avatar-badge {
  position: absolute; right: -2px; bottom: 2px;
  background: linear-gradient(120deg, #5eeaff, #7bffd4);
  color: #04121f; font-size: 11px; font-weight: 700;
  padding: 2px 8px; border-radius: 999px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

/* 自动登录 / 记住密码 */
.option-row { display: flex; justify-content: space-between; padding: 0 4px; }
.opt { display: flex; align-items: center; gap: 7px; font-size: 13px; color: var(--txt-sub); cursor: pointer; user-select: none; }
.opt:hover { color: var(--cyan); }
.ck {
  width: 16px; height: 16px; border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: grid; place-items: center; transition: all 0.2s;
  font-style: normal;
}
.ck.on { background: var(--cyan); border-color: var(--cyan); }
.ck i { font-size: 11px; color: #04121f; font-weight: 800; line-height: 1; }
.agree { justify-content: center; margin-top: 4px; font-size: 12px; }
.agree .lnk { color: var(--cyan); text-decoration: none; }

/* +86 手机号 */
.phone-row {
  display: flex; align-items: center; gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px; padding: 0 12px; margin-bottom: 14px;
}
.phone-row:focus-within { border-color: var(--cyan); }
.prefix { color: var(--cyan); font-size: 14.5px; font-weight: 700; white-space: nowrap; }
.prefix .caret { font-size: 10px; font-style: normal; opacity: 0.7; }
.phone-input :deep(.el-input__wrapper) { background: transparent; box-shadow: none !important; }

/* 短信验证码 */
.sms-row { display: flex; gap: 10px; }
.sms-btn {
  border: 1px solid rgba(94, 234, 255, 0.45);
  background: rgba(94, 234, 255, 0.12);
  color: var(--cyan); font-size: 12.5px;
  padding: 0 14px; border-radius: 10px; cursor: pointer; white-space: nowrap;
}
.sms-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.sms-btn:not(:disabled):hover { background: rgba(94, 234, 255, 0.22); }

.account-wrap { position: relative; }
.history {
  position: absolute;
  top: 44px; left: 0; right: 0;
  z-index: 20;
  padding: 6px;
  border-radius: 12px;
  background: rgba(12, 20, 36, 0.96);
  border: 1px solid rgba(94, 234, 255, 0.28);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}
.history-head { font-size: 11.5px; color: var(--txt-sub); padding: 6px 10px 8px; }
.history-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border-radius: 8px; cursor: pointer;
}
.history-item:hover { background: rgba(94, 234, 255, 0.1); }
.h-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  display: grid; place-items: center; font-size: 12px; font-weight: 700;
  background: linear-gradient(120deg, #38bdf8, #818cf8); color: #04121f;
}
.h-info { flex: 1; display: flex; flex-direction: column; }
.h-info b { font-size: 13px; }
.h-info small { color: var(--txt-sub); font-size: 11px; }
.h-time { color: var(--txt-sub); font-size: 11px; }
.h-del { color: var(--txt-sub); font-size: 15px; padding: 0 4px; }
.h-del:hover { color: #f87171; }
.clear-line { text-align: right; font-size: 11.5px; margin: 8px 0 0; }
.clear-line a { color: var(--txt-sub); text-decoration: none; }
.clear-line a:hover { color: var(--cyan); }
.demo-tip { margin-top: 20px; padding: 12px 16px; border-radius: 12px; }
.demo-tip b { color: var(--cyan); font-size: 13px; }
.demo-tip p { margin: 6px 0 0; color: var(--txt-sub); font-size: 12px; cursor: pointer; }
.demo-tip p:hover { color: var(--cyan); }
.all-link { display: inline-block; margin-top: 10px; color: var(--cyan); font-size: 12px; cursor: pointer; }
.all-link:hover { text-decoration: underline; }
.dlg-tip { margin: 0 0 14px; color: var(--txt-sub); font-size: 12.5px; line-height: 1.7; }
.acct-head, .acct-row { display: grid; grid-template-columns: 64px 130px 80px 1fr; gap: 8px; align-items: center; }
.acct-head { color: var(--txt-sub); font-size: 11.5px; padding: 6px 10px; border-bottom: 1px solid var(--border-glass); }
.acct-row { padding: 9px 10px; border-radius: 9px; cursor: pointer; font-size: 12.5px; }
.acct-row:hover { background: rgba(94, 234, 255, 0.1); }
.acct-row b { font-size: 11px; padding: 2px 8px; border-radius: 999px; white-space: nowrap; }
.tag-merchant { color: #7dd3fc; background: rgba(56,189,248,0.16); }
.tag-customer { color: #86efac; background: rgba(74,222,128,0.14); }
.tag-admin { color: #fcd34d; background: rgba(251,191,36,0.16); }
.mono { font-family: var(--font-mono, monospace); font-size: 12px; }
.acct-desc { color: var(--txt-sub); line-height: 1.5; }
.acct-desc small { display: block; font-size: 11px; opacity: 0.75; }
:deep(.el-tabs__item) { color: var(--txt-sub); }
:deep(.el-tabs__item.is-active) { color: var(--cyan); }
:deep(.el-tabs__active-bar) { background-color: var(--cyan); }
:deep(.el-tabs__nav-wrap::after) { background-color: rgba(255,255,255,0.08); }
</style>
