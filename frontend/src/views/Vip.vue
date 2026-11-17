<template>
  <main class="page vip-page">
    <!-- 顶部横幅：仿汽水音乐会员弹窗头 -->
    <div class="vip-hero glass">
      <div class="vip-hero-left">
        <div class="vip-crown">👑</div>
        <div>
          <h1 class="vip-title">开通会员 · 全站畅享优惠</h1>
          <p class="vip-sub">门票 / 美食 / 奶茶 / 团购套餐 / 跟团游，统统 <b class="grad-text">9 折</b></p>
        </div>
      </div>
      <div class="vip-state" v-if="me">
        <template v-if="isVip">
          <span class="vip-on">VIP 生效中</span>
          <small>到期：{{ fmt(me.vipExpire) }}</small>
        </template>
        <template v-else>
          <span class="vip-off">未开通</span>
          <small>开通即享全站折扣</small>
        </template>
      </div>
    </div>

    <!-- 套餐档位：仿汽水音乐档位卡 -->
    <div class="plans glass">
      <div class="plans-head"><span class="bar"></span>选择套餐</div>
      <div class="plan-row">
        <div v-for="p in plans" :key="p.id" class="plan-card"
             :class="{ active: selected?.id === p.id }" @click="selected = p">
          <span class="plan-tag" v-if="p.tag">{{ p.tag }}</span>
          <div class="plan-name">{{ p.name }} <i class="vip-mini">VIP</i></div>
          <div class="plan-price"><b>¥{{ p.price }}</b><s v-if="p.original !== p.price">¥{{ p.original }}</s></div>
          <small class="plan-note">{{ p.months }} 个月 · 折合 ¥{{ (p.price / p.months).toFixed(1) }}/月</small>
        </div>
      </div>
      <p class="plans-tip">会员折扣由后端在下单时自动按 9 折结算，美食 / 奶茶 / 门票 / 团购 / 跟团游通用。</p>
    </div>

    <!-- 支付面板：仿汽水音乐「同意并支付」 -->
    <div class="paybox glass" v-if="me">
      <div class="paybox-left">
        <div class="lock">🔒</div>
        <small>开通前请同意相关协议</small>
      </div>
      <div class="paybox-right">
        <div class="pay-price">
          <b class="grad-text">{{ selected ? '¥' + selected.price : '--' }}</b>
          <span class="pay-ways">🅰 支付宝（沙箱）</span>
        </div>
        <p class="pay-note">支付成功立即生效，可在「会员中心」查看到期时间。</p>
        <label class="agree" @click.prevent="agree = !agree">
          <span class="ck" :class="{ on: agree }"><i v-if="agree">✓</i></span>
          已阅读并同意 <a class="lnk" @click.stop>《会员服务协议》</a>
        </label>
        <button class="btn-grad pay-btn" :disabled="!selected || jumping" @click="pay">
          {{ jumping ? '正在跳转支付宝…' : '同意并支付' }}
        </button>
        <div class="poll-line" v-if="jumping">等待付款确认中… 已查询 {{ polls }} 次</div>
      </div>
    </div>
    <div class="paybox glass need-login" v-else>
      <p>登录后即可开通会员</p>
      <router-link to="/login" class="btn-grad" style="padding: 10px 30px; text-decoration: none">去登录</router-link>
    </div>

    <!-- 支付成功 -->
    <div class="paybox glass success-box" v-if="success">
      <div class="success-icon">✓</div>
      <h2>会员开通成功！</h2>
      <p class="grad-text" style="font-size: 22px; font-weight: 700">有效期至 {{ fmt(success.vipExpire) }}</p>
      <p style="color: var(--txt-sub); font-size: 13px">现在去下单，门票、美食、团购、跟团游自动 9 折</p>
    </div>

    <!-- VIP 权益 -->
    <div class="benefits glass">
      <div class="plans-head"><span class="bar"></span>VIP 基础权益</div>
      <div class="benefit-grid">
        <div class="benefit"><span class="b-ico">🎫</span><b>门票 9 折</b><small>全平台直营景点门票</small></div>
        <div class="benefit"><span class="b-ico">🍜</span><b>美食 9 折</b><small>到店餐饮 / 特色美食</small></div>
        <div class="benefit"><span class="b-ico">🧋</span><b>奶茶 9 折</b><small>联名饮品 / 当地茶饮</small></div>
        <div class="benefit"><span class="b-ico">🎁</span><b>团购折上折</b><small>套餐价再享会员折扣</small></div>
        <div class="benefit"><span class="b-ico">🚌</span><b>跟团游 9 折</b><small>广西四城旅游团报名</small></div>
        <div class="benefit"><span class="b-ico">🎧</span><b>专属客服</b><small>会员优先接入</small></div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import request, { currentUser, saveUser } from '../api.js'

const me = ref(currentUser())
const plans = ref([])
const selected = ref(null)
const agree = ref(false)
const jumping = ref(false)
const polls = ref(0)
const success = ref(null)
let timer = null

const isVip = computed(() => me.value?.vipLevel === 1 && me.value?.vipExpire &&
  new Date(me.value.vipExpire).getTime() > Date.now())

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

function refreshMe() {
  request.get('/vip/status', { silent: true }).then(s => {
    const u = currentUser()
    if (u) {
      saveUser({ ...u, vipLevel: s.vipLevel ?? (s.vip ? 1 : 0), vipExpire: s.vipExpire })
      me.value = currentUser()
    }
  }).catch(() => {})
}

function pay() {
  if (!selected.value) return ElMessage.warning('请先选择套餐')
  if (!agree.value) return ElMessage.warning('请先勾选同意《会员服务协议》')
  jumping.value = true
  request.post('/vip/create', { planId: selected.value.id }).then(({ form, orderNo }) => {
    const win = window.open('', '_blank')
    if (!win) {
      ElMessage.error('浏览器拦截了新窗口，请允许弹出后重试')
      jumping.value = false
      return
    }
    win.document.write(form)
    win.document.close()
    timer = setInterval(async () => {
      try {
        const r = await request.get('/vip/query/' + orderNo, { silent: true })
        polls.value++
        if (r.status === 'PAID') {
          clearInterval(timer)
          jumping.value = false
          success.value = r
          refreshMe()
        }
      } catch { /* 继续轮询 */ }
    }, 3000)
  }).catch(() => { jumping.value = false })
}

onMounted(async () => {
  plans.value = await request.get('/vip/plans')
  // 默认选中「连续包月」（最便宜入口档）
  selected.value = plans.value[0] || null
  // 仅已登录才刷新会员状态（避免未登录 401 被拦截器跳走，页面可先浏览套餐）
  if (me.value) refreshMe()
})

onBeforeUnmount(() => timer && clearInterval(timer))
</script>

<style scoped>
.vip-page { max-width: 1000px; }

.vip-hero { display: flex; align-items: center; justify-content: space-between; padding: 24px 28px; gap: 20px; }
.vip-hero-left { display: flex; align-items: center; gap: 16px; }
.vip-crown {
  width: 62px; height: 62px; border-radius: 18px; display: grid; place-items: center; font-size: 30px;
  background: linear-gradient(135deg, rgba(94, 234, 255, 0.2), rgba(129, 140, 248, 0.2));
  border: 1px solid rgba(94, 234, 255, 0.35);
}
.vip-title { margin: 0 0 6px; font-size: 24px; }
.vip-sub { margin: 0; color: var(--txt-sub); font-size: 14px; }
.vip-state { text-align: right; display: flex; flex-direction: column; gap: 4px; }
.vip-on { color: #34d399; font-weight: 700; font-size: 16px; }
.vip-off { color: var(--txt-sub); font-size: 15px; }
.vip-state small { color: var(--txt-sub); font-size: 12px; }

.plans { margin-top: 18px; padding: 20px 22px; }
.plans-head { display: flex; align-items: center; gap: 10px; font-weight: 700; margin-bottom: 16px; }
.plans-head .bar { width: 4px; height: 16px; border-radius: 2px; background: var(--cyan); box-shadow: 0 0 8px var(--cyan); }
.plan-row { display: flex; gap: 14px; flex-wrap: wrap; }
.plan-card {
  position: relative; flex: 1; min-width: 150px; cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px; padding: 16px 14px 14px;
  background: rgba(255, 255, 255, 0.04);
  transition: all 0.25s;
}
.plan-card:hover { transform: translateY(-3px); }
.plan-card.active {
  border-color: var(--cyan);
  background: linear-gradient(160deg, rgba(94, 234, 255, 0.14), rgba(129, 140, 248, 0.08));
  box-shadow: 0 10px 28px rgba(94, 234, 255, 0.18);
}
.plan-tag {
  position: absolute; top: -9px; left: 10px;
  background: linear-gradient(120deg, #5eeaff, #7bffd4); color: #04121f;
  font-size: 10.5px; font-weight: 700; padding: 2px 8px; border-radius: 999px;
}
.plan-name { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.vip-mini {
  font-style: normal; font-size: 9px; font-weight: 800; color: #04121f;
  background: linear-gradient(120deg, #fbbf24, #f59e0b); padding: 1px 5px; border-radius: 4px;
}
.plan-price { margin-top: 10px; }
.plan-price b { font-size: 26px; color: var(--cyan); }
.plan-price s { margin-left: 8px; color: var(--txt-sub); font-size: 12.5px; }
.plan-note { display: block; margin-top: 6px; color: var(--txt-sub); font-size: 11.5px; }
.plans-tip { margin: 14px 0 0; color: var(--txt-sub); font-size: 12px; }

.paybox { margin-top: 18px; padding: 22px 24px; display: flex; gap: 26px; align-items: flex-start; }
.paybox-left {
  width: 150px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 10px;
  border-right: 1px dashed rgba(255, 255, 255, 0.14); padding-right: 20px; text-align: center;
}
.paybox-left .lock {
  width: 58px; height: 58px; border-radius: 14px; display: grid; place-items: center; font-size: 26px;
  background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.14);
}
.paybox-left small { color: var(--txt-sub); font-size: 11.5px; line-height: 1.6; }
.paybox-right { flex: 1; }
.pay-price { display: flex; align-items: baseline; gap: 16px; }
.pay-price b { font-size: 34px; }
.pay-ways { color: #38bdf8; font-size: 13.5px; font-weight: 600; }
.pay-note { color: var(--txt-sub); font-size: 12.5px; margin: 8px 0 10px; }
.agree { display: flex; align-items: center; gap: 7px; font-size: 12px; color: var(--txt-sub); cursor: pointer; }
.ck {
  width: 16px; height: 16px; border-radius: 50%; border: 1px solid rgba(255, 255, 255, 0.35);
  display: grid; place-items: center; transition: all 0.2s; font-style: normal;
}
.ck.on { background: var(--cyan); border-color: var(--cyan); }
.ck i { font-size: 11px; color: #04121f; font-weight: 800; line-height: 1; }
.lnk { color: var(--cyan); }
.pay-btn { margin-top: 12px; padding: 12px 46px; font-size: 15px; }
.poll-line { margin-top: 10px; color: var(--txt-sub); font-size: 12px; }

.need-login { display: flex; align-items: center; justify-content: space-between; }
.need-login p { margin: 0; color: var(--txt-sub); }

.success-box { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; }
.success-icon {
  width: 64px; height: 64px; border-radius: 50%; display: grid; place-items: center;
  background: linear-gradient(135deg, #10b981, #22d3ee); color: #04121f; font-size: 32px; font-weight: 800;
}
.success-box h2 { margin: 0; }

.benefits { margin-top: 18px; padding: 20px 22px; }
.benefit-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.benefit {
  display: flex; flex-direction: column; gap: 4px; padding: 14px 16px;
  border-radius: 12px; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08);
}
.benefit .b-ico { font-size: 20px; }
.benefit b { font-size: 14px; }
.benefit small { color: var(--txt-sub); font-size: 11.5px; }

@media (max-width: 760px) {
  .paybox { flex-direction: column; }
  .paybox-left { border-right: none; border-bottom: 1px dashed rgba(255,255,255,0.14); padding: 0 0 14px; width: 100%; }
  .benefit-grid { grid-template-columns: repeat(2, 1fr); }
  .plan-card { min-width: 130px; }
}
</style>
