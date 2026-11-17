<template>
  <main class="page pay-page" v-if="order">
    <div class="pay-card glass">
      <template v-if="!paid">
        <div class="cashier-head">
          <div class="ali-brand">
            <span class="ali-mark">支</span>
            <div class="ali-brand-txt">
              <b>支付宝</b>
              <span>广西旅游平台 · 沙箱收银台</span>
            </div>
          </div>
          <span class="sandbox-badge">沙箱环境</span>
        </div>

        <div class="order-brief">
          <div class="brief-name">{{ order.productName }}</div>
          <div class="brief-no">订单号 {{ order.orderNo }}</div>
          <div class="brief-amount">¥{{ order.total }}</div>
          <div class="brief-count">共 {{ order.quantity }} 件<span v-if="order.visitDate"> · 游玩日期 {{ order.visitDate }}</span></div>
        </div>

        <div class="sandbox-card">
          <div class="sc-title">沙箱测试号<span>付款时用这个登录，不是你的真实支付宝</span></div>
          <div class="sc-row">
            <span class="sc-label">买家账号</span>
            <b class="sc-value">{{ SANDBOX.account }}</b>
            <button class="copy-btn" @click="copy(SANDBOX.account, '买家账号')">复制</button>
          </div>
          <div class="sc-row">
            <span class="sc-label">登录密码</span>
            <b class="sc-value">{{ SANDBOX.loginPwd }}</b>
            <button class="copy-btn" @click="copy(SANDBOX.loginPwd, '登录密码')">复制</button>
          </div>
          <div class="sc-row">
            <span class="sc-label">支付密码</span>
            <b class="sc-value">{{ SANDBOX.payPwd }}</b>
            <button class="copy-btn" @click="copy(SANDBOX.payPwd, '支付密码')">复制</button>
          </div>
          <div class="sc-row">
            <span class="sc-label">APPID</span>
            <b class="sc-value sc-appid">{{ SANDBOX.appId }}</b>
          </div>
          <p class="sc-note">密码若被改动过，可在沙箱控制台「沙箱账号」页重置为默认值</p>
        </div>

        <p class="pay-hint">点击下方按钮，跳到支付宝沙箱收银台完成付款。收银台会以手机尺寸打开，请选「<b>支付密码登录</b>」，<b>不要用手机号登录</b>（沙箱账号会提示系统繁忙）。</p>

        <button class="btn-grad pay-btn" @click="goAlipay">
          {{ jumping ? '重新打开支付宝沙箱收银台' : '前往支付宝沙箱支付' }}
        </button>
        <button class="ghost-btn" :disabled="checking" @click="checkNow">
          {{ checking ? '正在核实支付结果…' : '我已付款，刷新支付状态' }}
        </button>

        <div class="countdown" v-if="jumping">付款完成后本页会自动确认并显示取餐号，无需手动操作 · 已查询 {{ polls }} 次</div>

        <button class="link-btn" @click="cancel">暂不支付，返回订单</button>

        <p class="pay-foot">真实 RSA2 签名 + 真实沙箱网关，只有账户余额是虚拟的（在沙箱控制台可充值）。<br />
          付款完成后收银台会自动关闭、本页自动跳转到取餐号页；若没反应，点上面的「我已付款，刷新支付状态」即可确认。</p>
      </template>

      <template v-else>
        <div class="success">
          <div class="success-icon">✓</div>
          <h2>支付成功！</h2>
          <p class="grad-text" style="font-size: 26px; font-weight: 700">¥{{ order.total }}</p>
          <p class="sub">{{ order.productName }} × {{ order.quantity }} 已出票</p>
          <div v-if="order.pickNo" class="pick-box">
            <span class="pick-label">取餐号</span>
            <b class="pick-no">{{ order.pickNo }}</b>
            <span class="pick-tip">到店报号取餐，也可在「我的订单」查看</span>
          </div>
          <div v-else class="pick-box">
            <span class="pick-label">订单号</span>
            <b class="pick-no order-no">{{ order.orderNo }}</b>
            <span class="pick-tip">凭此单号入园，也可在「我的订单」查看</span>
          </div>
          <p class="auto-tip">已自动关闭支付宝收银台窗口并完成入账</p>
          <button class="btn-grad" style="padding: 10px 30px" @click="$router.push('/orders')">查看我的订单</button>
        </div>
      </template>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api.js'

// 沙箱测试账号：本应用的买家账号（不是点餐平台那个），以开放平台「沙箱账号」页为准
const SANDBOX = {
  account: 'apyvlg9205@sandbox.com',
  loginPwd: '111111',
  payPwd: '111111',
  appId: '9021000171601705'
}

// 收银台按手机尺寸打开：支付宝这个页面是移动端 H5，铺满桌面窗口会拉伸变形（输入框超长、大片空白）。
// 520x860 实测输入框/密码格/「使用长密码登录」链接完整无裁切。
const CASHIER_W = 520
const CASHIER_H = 860

const route = useRoute()
const order = ref(null)
const paid = ref(false)
const jumping = ref(false)
const checking = ref(false)
const polls = ref(0)
const lastCashier = ref(null) // {mode:'redirect'|'form', target}
let timer = null
let cashierWin = null

async function load() {
  order.value = await request.get('/order/' + route.params.orderNo)
  if (order.value.status === 'PAID') paid.value = true
}

function openWindow() {
  const left = Math.max(0, Math.round((window.screen.width - CASHIER_W) / 2))
  return window.open('', '_blank',
    `width=${CASHIER_W},height=${CASHIER_H},left=${left},top=30,menubar=no,toolbar=no,status=no,resizable=yes,scrollbars=yes`)
}

async function copy(text, label) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制`)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success(`${label}已复制`)
  }
}

/**
 * 把 EasySDK 生成的自动提交表单写入收银台窗口。
 * 显式声明 UTF-8：新开的 about:blank 窗口默认编码不固定，
 * 商品名含中文时若不指定会导致 biz_content 转码乱码、网关验签失败。
 */
function renderCashier(form) {
  if (!cashierWin || cashierWin.closed) return false
  const doc = cashierWin.document
  doc.open()
  doc.write('<!doctype html><html><head><meta charset="utf-8"><title>支付宝沙箱收银台</title></head><body>'
    + form + '</body></html>')
  doc.close()
  return true
}

function goAlipay() {
  // 关键：window.open 必须留在用户点击的同步调用栈内。
  // 放在 Promise.then 回调里会被浏览器弹窗拦截器拦掉，表现为「点了没反应 / 一直卡住」。
  cashierWin = openWindow()
  if (!cashierWin) {
    ElMessage.error('浏览器拦截了新窗口，请允许本站弹出窗口后重试')
    return
  }
  cashierWin.document.write('<!doctype html><html><head><meta charset="utf-8"><title>支付宝沙箱收银台</title>'
    + '</head><body style="font-family:system-ui;padding:40px 20px;color:#334;text-align:center"><h3>正在前往支付宝沙箱收银台…</h3>'
    + '<p style="color:#889">请勿关闭本窗口</p></body></html>')
  cashierWin.document.close()
  jumping.value = true
  polls.value = 0
  request.post('/pay/alipay/cashier', { orderNo: order.value.orderNo }).then(res => {
    lastCashier.value = res
    if (res.mode === 'redirect') {
      // 服务端已替我们走过「继续浏览器付款」这一步，直接落到沙箱买家登录页
      if (cashierWin && !cashierWin.closed) cashierWin.location.href = res.target
      else reopenCashier()
    } else if (!renderCashier(res.target)) {
      ElMessage.error('收银台窗口已关闭，请点击「重新打开支付宝沙箱收银台」')
      return
    }
    clearInterval(timer)
    timer = setInterval(check, 3000)
  }).catch(() => {
    jumping.value = false
    if (cashierWin && !cashierWin.closed) cashierWin.close()
  })
}

function reopenCashier() {
  const c = lastCashier.value
  if (!c) return goAlipay()
  // 同样在点击同步栈内重新开窗，避免被拦截
  if (!cashierWin || cashierWin.closed) cashierWin = openWindow()
  if (!cashierWin) return ElMessage.error('浏览器拦截了新窗口，请允许本站弹出窗口后重试')
  if (c.mode === 'redirect') cashierWin.location.href = c.target
  else renderCashier(c.target)
}

/** 付款成功后自动关闭我们打开的收银台窗口，回到本站的取餐号/订单页 */
function closeCashier() {
  try {
    if (cashierWin && !cashierWin.closed) cashierWin.close()
  } catch { /* 跨域窗口可能拒绝关闭，忽略 */ }
  cashierWin = null
}

async function check() {
  try {
    const o = await request.get('/pay/alipay/query/' + order.value.orderNo, { silent: true })
    polls.value++
    order.value = o
    if (o.status === 'PAID') {
      paid.value = true
      jumping.value = false
      clearInterval(timer)
      closeCashier()
    }
  } catch { /* 查单失败继续轮询 */ }
}

async function checkNow() {
  checking.value = true
  ElMessage.info('正在向支付宝核实支付结果…')
  try { await check() } finally { checking.value = false }
}

async function cancel() {
  await request.post(`/order/${order.value.orderNo}/cancel`)
  ElMessage.success('订单已取消')
  clearInterval(timer)
  closeCashier()
  location.href = '/orders'
}

onMounted(load)
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.pay-page { display: flex; justify-content: center; }
.pay-card { width: 100%; max-width: 520px; padding: 26px 28px 22px; }

.cashier-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.ali-brand { display: flex; align-items: center; gap: 10px; }
.ali-mark {
  width: 34px; height: 34px; border-radius: 9px; display: grid; place-items: center;
  background: linear-gradient(135deg, #1677ff, #38bdf8); color: #fff; font-weight: 800; font-size: 17px;
}
.ali-brand-txt { display: flex; flex-direction: column; line-height: 1.3; }
.ali-brand-txt b { font-size: 15px; color: var(--txt); }
.ali-brand-txt span { font-size: 11px; color: var(--txt-sub); }
.sandbox-badge {
  font-size: 11px; padding: 3px 9px; border-radius: 20px; color: #1677ff;
  background: rgba(22, 119, 255, 0.12); border: 1px solid rgba(22, 119, 255, 0.35); font-weight: 600;
}

.order-brief { text-align: center; padding: 6px 0 18px; border-bottom: 1px dashed rgba(120, 160, 200, 0.28); margin-bottom: 18px; }
.brief-name { font-size: 15px; font-weight: 600; color: var(--txt); }
.brief-no { font-size: 11px; color: var(--txt-sub); margin-top: 4px; letter-spacing: 0.3px; }
.brief-amount { font-size: 36px; font-weight: 800; margin: 10px 0 4px; color: var(--txt); letter-spacing: -1px; }
.brief-count { font-size: 12px; color: var(--txt-sub); }

.sandbox-card {
  border-radius: 12px; padding: 14px 16px; margin-bottom: 16px;
  background: rgba(22, 119, 255, 0.07); border: 1px solid rgba(22, 119, 255, 0.2);
}
.sc-title { font-size: 13px; font-weight: 700; color: #1677ff; margin-bottom: 10px; display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.sc-title span { font-size: 11px; font-weight: 400; color: var(--txt-sub); }
.sc-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; font-size: 13px; }
.sc-label { color: var(--txt-sub); width: 66px; flex: none; }
.sc-value { color: var(--txt); font-family: Consolas, Monaco, monospace; font-size: 13px; word-break: break-all; }
.sc-appid { font-size: 12px; color: var(--txt-sub); }
.sc-note { margin: 8px 0 0; font-size: 11px; color: var(--txt-sub); line-height: 1.6; }
.copy-btn {
  margin-left: auto; flex: none; cursor: pointer; font-size: 11px; padding: 3px 10px; border-radius: 16px;
  color: #1677ff; background: rgba(22, 119, 255, 0.1); border: 1px solid rgba(22, 119, 255, 0.3);
  transition: all 0.18s;
}
.copy-btn:hover { background: rgba(22, 119, 255, 0.2); }

.pay-hint { font-size: 12px; line-height: 1.8; color: var(--txt-sub); margin: 0 0 16px; text-align: left; }
.pay-hint b { color: var(--txt); }

.pay-btn { width: 100%; padding: 13px 0; font-size: 15px; font-weight: 600; }
.ghost-btn {
  width: 100%; margin-top: 10px; padding: 11px 0; font-size: 14px; cursor: pointer;
  border-radius: 10px; color: #1677ff; background: rgba(22, 119, 255, 0.08);
  border: 1px solid rgba(22, 119, 255, 0.28); transition: all 0.18s;
}
.ghost-btn:hover:not(:disabled) { background: rgba(22, 119, 255, 0.16); }
.ghost-btn:disabled { opacity: 0.65; cursor: default; }

.countdown { color: var(--txt-sub); font-size: 12px; margin-top: 12px; text-align: center; }
.link-btn {
  display: block; margin: 16px auto 0; cursor: pointer; font-size: 12px; color: var(--txt-sub);
  background: none; border: none; text-decoration: underline; text-underline-offset: 3px;
}
.link-btn:hover { color: var(--txt); }
.pay-foot {
  margin: 16px 0 0; padding-top: 14px; border-top: 1px dashed rgba(120, 160, 200, 0.22);
  font-size: 11px; line-height: 1.8; color: var(--txt-sub); text-align: center;
}

.success { text-align: center; padding: 26px 0 10px; }
.success-icon {
  width: 74px; height: 74px; margin: 0 auto 18px;
  display: grid; place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #22d3ee);
  color: #04121f; font-size: 38px; font-weight: 800;
}
.success h2 { margin: 0 0 8px; }
.sub { color: var(--txt-sub); margin-bottom: 24px; }
.pick-box {
  display: inline-flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-bottom: 12px; border-radius: 12px;
  background: rgba(94, 234, 255, 0.1);
  border: 1px dashed rgba(94, 234, 255, 0.45);
}
.pick-label { font-size: 12px; color: #052a33; background: linear-gradient(120deg, #5eeaff, #7bffd4);
  padding: 2px 8px; border-radius: 6px; font-weight: 700; }
.pick-no { font-size: 24px; letter-spacing: 2px; color: var(--cyan); font-family: Consolas, monospace; }
.pick-no.order-no { font-size: 14px; letter-spacing: 0.5px; }
.pick-tip { font-size: 12px; color: var(--txt-sub); }
.auto-tip { font-size: 11px; color: var(--txt-sub); margin-bottom: 18px; }
</style>
