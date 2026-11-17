<template>
  <main class="page merchant">
    <aside class="side glass">
      <div class="side-user">
        <img :src="avatarOf(user)" class="avatar" @error="onAvatarError" />
        <div>
          <b>{{ user?.nickname || '未登录' }}</b>
          <p>商家工作台</p>
        </div>
      </div>
      <router-link to="/merchant" class="side-link" exact-active-class="active">📊 经营看板</router-link>
      <router-link to="/merchant/products" class="side-link" active-class="active">📦 商品管理</router-link>
      <router-link to="/merchant/orders" class="side-link" active-class="active">🧾 订单管理</router-link>
      <router-link to="/merchant/income" class="side-link" active-class="active">💰 我的收入</router-link>
      <router-link to="/" class="side-link">← 返回前台</router-link>

      <div class="isolation glass">
        <b>🔒 数据隔离说明</b>
        <p>当前身份只能查看和操作
          <em>{{ shopNames || '自己名下店铺' }}</em>
          的商品与订单；其他商家的商品、订单、营业额一律不可见、不可修改。</p>
      </div>

      <div class="switch-box glass">
        <b>🔄 切换到另一个商家</b>
        <p>演示用快捷登录，切换后立刻只能看到该商家的后台</p>
        <el-select v-model="switchTo" placeholder="选择商家账号" size="default" style="width: 100%">
          <el-option v-for="m in merchants" :key="m.username"
                     :label="m.nickname + '（' + m.username + '）'" :value="m.username" />
        </el-select>
        <button class="switch-btn" :disabled="!switchTo || switching" @click="switchMerchant">
          {{ switching ? '切换中…' : '切换并登录' }}
        </button>
        <small>演示密码统一为 {{ DEMO_PASSWORD }}，切换会重新走登录接口换取新令牌</small>
      </div>

      <button class="logout-btn" @click="doLogout">退出登录</button>
    </aside>
    <section class="content">
      <router-view />
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request, { authUser, saveUser, logout, avatarOf, DEFAULT_AVATAR, DEMO_PASSWORD } from '../../api.js'

const user = authUser
const merchants = ref([])
const switchTo = ref('')
const switching = ref(false)
const shopNames = ref('')

function onAvatarError(e) { e.target.src = DEFAULT_AVATAR }

async function loadMine() {
  try {
    const shops = await request.get('/merchant/shops', { silent: true })
    shopNames.value = (shops || []).map(s => s.name).join('、')
  } catch { shopNames.value = '' }
  try {
    const accounts = await request.get('/auth/demo-accounts')
    merchants.value = (accounts || []).filter(a =>
      a.role === 'MERCHANT' && a.username !== user.value?.username)
  } catch { merchants.value = [] }
}

async function switchMerchant() {
  if (!switchTo.value) return
  switching.value = true
  try {
    // 正常走登录接口，拿到该商家的 token；切换后所有数据按新身份重新加载
    const data = await request.post('/auth/login', {
      username: switchTo.value, password: DEMO_PASSWORD
    })
    saveUser(data)
    ElMessage.success('已切换到 ' + data.user.nickname)
    location.href = '/merchant/products'
  } catch { /* 拦截器已提示 */ }
  finally { switching.value = false }
}

function doLogout() {
  logout()
  location.href = '/'
}

onMounted(loadMine)
</script>

<style scoped>
.merchant { display: grid; grid-template-columns: 220px 1fr; gap: 20px; align-items: start; }
.side { padding: 16px 12px; position: sticky; top: 90px; display: flex; flex-direction: column; gap: 6px; }
.side-user { display: flex; gap: 10px; align-items: center; padding: 8px 8px 16px; border-bottom: 1px solid var(--border-glass); margin-bottom: 10px; }
.side-user .avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; background: rgba(255,255,255,0.1); flex-shrink: 0; }
.side-user b { font-size: 14px; }
.side-user p { margin: 0; color: var(--txt-sub); font-size: 12px; }
.side-link { color: var(--txt-sub); text-decoration: none; padding: 10px 14px; border-radius: 10px; font-size: 14px; }
.side-link:hover { color: var(--txt); background: rgba(255,255,255,0.06); }
.side-link.active { color: var(--txt); background: rgba(56, 189, 248, 0.14); border: 1px solid var(--border-glow); }
.isolation, .switch-box { margin-top: 14px; padding: 12px; border-radius: 12px; }
.isolation b, .switch-box b { font-size: 12.5px; color: var(--cyan); }
.isolation p, .switch-box p { margin: 8px 0; color: var(--txt-sub); font-size: 11.5px; line-height: 1.7; }
.isolation em { color: var(--txt); font-style: normal; }
.switch-box small { display: block; margin-top: 8px; color: var(--txt-sub); font-size: 11px; line-height: 1.6; }
.switch-btn, .logout-btn {
  width: 100%; margin-top: 8px; padding: 8px 10px; border-radius: 9px; cursor: pointer;
  border: 1px solid var(--border-glow); background: rgba(56,189,248,0.16);
  color: var(--txt); font-size: 12.5px;
}
.switch-btn:hover { background: rgba(56,189,248,0.28); }
.switch-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.logout-btn { margin-top: 14px; background: transparent; border-color: var(--border-glass); color: var(--txt-sub); }
.logout-btn:hover { color: #f87171; border-color: #f87171; }
@media (max-width: 820px) {
  .merchant { grid-template-columns: 1fr; }
  .side { position: static; flex-direction: row; flex-wrap: wrap; }
  .side-user { border: none; }
  .isolation, .switch-box { width: 100%; }
}
</style>
