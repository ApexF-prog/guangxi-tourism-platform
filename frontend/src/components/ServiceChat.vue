<template>
  <div class="cs-wrap" :style="wrapStyle" :class="{ 'cs-dragging': dragging }">
    <!-- 主动引导气泡：浅色卡片压在深色页面上，第一眼就能看见；靠近左边缘时自动翻到右侧 -->
    <transition name="cs-hint">
      <div v-if="!open && hintVisible" class="cs-hint-card" :class="{ 'cs-hint-right': hintRight }">
        <button class="cs-hint-close" @click="dismiss" aria-label="关闭提示">✕</button>
        <p class="cs-hint-title">遇到问题？让我来为你解答～</p>
        <button class="cs-hint-cta" @click="startChat">去咨询</button>
        <i class="cs-hint-tail"></i>
      </div>
    </transition>

    <!-- 机器人常驻入口：气泡关掉后仍可随时点开 -->
    <button v-if="!open" class="cs-bot" :class="{ 'cs-bot-alone': !hintVisible }"
            @mousedown="onDown" @click="onBotClick"
            title="按住可拖动 · 点击打开客服" aria-label="在线客服，可拖动">
      <svg viewBox="0 0 64 64" class="cs-bot-svg" aria-hidden="true">
        <defs>
          <linearGradient id="csBotG" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#66b4ff" />
            <stop offset="1" stop-color="#3b6fe8" />
          </linearGradient>
          <linearGradient id="csBotFace" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#ffffff" />
            <stop offset="1" stop-color="#dceaff" />
          </linearGradient>
        </defs>
        <ellipse cx="32" cy="58" rx="17" ry="3.2" fill="#0b1a30" opacity=".16" />
        <path d="M32 12 L32 6" stroke="#2f62d8" stroke-width="2.4" stroke-linecap="round" />
        <circle cx="32" cy="5" r="2.6" fill="#ffcc4d" />
        <path d="M12 9 l4 3 M52 9 l-4 3" stroke="#8fa2c4" stroke-width="2" stroke-linecap="round" />
        <rect x="7" y="25" width="6" height="12" rx="3" fill="#2f62d8" />
        <rect x="51" y="25" width="6" height="12" rx="3" fill="#2f62d8" />
        <rect x="12" y="13" width="40" height="36" rx="16" fill="url(#csBotG)" />
        <rect x="17" y="20" width="30" height="22" rx="11" fill="url(#csBotFace)" />
        <circle cx="26" cy="30" r="3.4" fill="#1b3a6b" />
        <circle cx="38" cy="30" r="3.4" fill="#1b3a6b" />
        <circle cx="27" cy="29" r="1.1" fill="#fff" />
        <circle cx="39" cy="29" r="1.1" fill="#fff" />
        <path d="M28 36 q4 3.5 8 0" stroke="#1b3a6b" stroke-width="1.8" fill="none" stroke-linecap="round" />
        <path d="M24 48 q8 6 16 0" fill="#2f62d8" opacity=".85" />
      </svg>
      <span v-if="hintVisible" class="cs-bot-dot"></span>
    </button>

    <!-- 提示关掉之后，「去咨询」放在机器人正下方 -->
    <button v-if="!open && !hintVisible" class="cs-mini-cta" @click="startChat">去咨询</button>

    <!-- 聊天面板 -->
    <transition name="cs-pop">
      <div v-if="open" class="cs-panel glass" :style="panelStyle">
        <div class="cs-head" @mousedown="onPanelDown" @dblclick="onPanelReset"
             title="按住顶部可拖动 · 双击回到默认位置">
          <div class="cs-head-l">
            <span class="cs-avatar">GX</span>
            <div>
              <b>广西旅游网 · 智能客服</b>
              <small><i class="on-dot"></i> 在线 · 平均 1 分钟回复</small>
            </div>
          </div>
          <button class="cs-close" @mousedown.stop @click="open = false">✕</button>
        </div>

        <div class="cs-body" ref="bodyEl">
          <div v-for="(m, i) in msgs" :key="i" class="cs-msg" :class="m.from">
            <div class="cs-bubble">{{ m.text }}</div>
            <div v-if="m.cards && m.cards.length" class="cs-cards">
              <button v-for="c in m.cards" :key="c.label" class="cs-card" @click="onQuick(c)">
                {{ c.label }}
              </button>
            </div>
          </div>
          <div v-if="typing" class="cs-msg bot"><div class="cs-bubble typing">正在输入…</div></div>
        </div>

        <div class="cs-quick">
          <button v-for="q in quicks" :key="q.label" class="cs-chip" @click="onQuick(q)">{{ q.label }}</button>
        </div>

        <div class="cs-input-row">
          <input v-model="draft" class="cs-input" placeholder="描述您的问题，如：怎么退款？"
                 @keyup.enter="send" />
          <button class="cs-send" @click="send">发送</button>
        </div>
        <div class="cs-foot">
          人工咨询：📞 19175657645 · 💬 微信 waxrl0911
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const open = ref(false)
const hintVisible = ref(false)
const typing = ref(false)
const draft = ref('')
const bodyEl = ref(null)
const router = useRouter()

const WRAP = 64
const PANEL_W = 340
const PANEL_H = 520
const POS_KEY = 'gx_cs_pos'
const PANEL_POS_KEY = 'gx_cs_panel_pos'
const HINT_KEY = 'gx_cs_hint_dismissed'

const pos = ref({ x: 0, y: 0 })
const panelPos = ref(null)
const viewport = ref({ w: 1440, h: 900 })
const posReady = ref(false)
const dragging = ref(false)

function clampPoint(p) {
  const w = viewport.value.w, h = viewport.value.h
  return {
    x: Math.min(Math.max(8, p.x), Math.max(8, w - WRAP - 8)),
    // 下边界多留空间，避免机器人下方的「去咨询」按钮被裁掉
    y: Math.min(Math.max(8, p.y), Math.max(8, h - 120)),
  }
}
function defaultPos() {
  // 默认放在右侧偏上，不再窝在右下角
  return { x: Math.max(8, viewport.value.w - WRAP - 28), y: Math.round(viewport.value.h * 0.36) }
}
const hintRight = computed(() => pos.value.x < 300)
const wrapStyle = computed(() => ({
  left: pos.value.x + 'px',
  top: pos.value.y + 'px',
  visibility: posReady.value ? 'visible' : 'hidden',
}))
// 面板默认跟随机器人；拖过之后用自己记住的位置，两者都夹在视口内
const panelStyle = computed(() => {
  const w = viewport.value.w, h = viewport.value.h
  const width = Math.min(PANEL_W, Math.max(240, w - 16))
  const height = Math.min(PANEL_H, Math.max(280, h - 16))
  let x, y
  if (panelPos.value && Number.isFinite(panelPos.value.x) && Number.isFinite(panelPos.value.y)) {
    x = panelPos.value.x; y = panelPos.value.y
  } else {
    x = pos.value.x - width - 12
    if (x < 8) x = pos.value.x + WRAP + 12
    y = pos.value.y + WRAP - height
  }
  x = Math.min(Math.max(8, x), Math.max(8, w - width - 8))
  y = Math.min(Math.max(8, y), Math.max(8, h - height - 8))
  return { left: x + 'px', top: y + 'px', width: width + 'px', height: height + 'px' }
})

let dragFrom = null
let justDragged = false
function onDown(e) {
  if (e.button !== 0) return
  dragFrom = { mx: e.clientX, my: e.clientY, x: pos.value.x, y: pos.value.y, moved: false }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}
function onMove(e) {
  if (!dragFrom) return
  const dx = e.clientX - dragFrom.mx
  const dy = e.clientY - dragFrom.my
  if (Math.abs(dx) + Math.abs(dy) > 4) dragFrom.moved = true
  if (!dragFrom.moved) return
  dragging.value = true
  pos.value = clampPoint({ x: dragFrom.x + dx, y: dragFrom.y + dy })
}
function onUp() {
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('mouseup', onUp)
  if (dragFrom && dragFrom.moved) {
    justDragged = true
    try { localStorage.setItem(POS_KEY, JSON.stringify(pos.value)) } catch (err) { /* 隐私模式忽略 */ }
    setTimeout(() => { justDragged = false }, 0)
  }
  dragging.value = false
  dragFrom = null
}
function onBotClick() {
  if (justDragged) return
  startChat()
}

/* 聊天面板：按住顶部拖动，双击顶部回到默认位置 */
let panelDrag = null
function onPanelDown(e) {
  if (e.button !== 0) return
  const s = panelStyle.value
  panelDrag = {
    mx: e.clientX, my: e.clientY,
    x: parseFloat(s.left), y: parseFloat(s.top),
    w: parseFloat(s.width), h: parseFloat(s.height),
    moved: false,
  }
  document.addEventListener('mousemove', onPanelMove)
  document.addEventListener('mouseup', onPanelUp)
}
function onPanelMove(e) {
  if (!panelDrag) return
  const dx = e.clientX - panelDrag.mx
  const dy = e.clientY - panelDrag.my
  if (Math.abs(dx) + Math.abs(dy) > 4) panelDrag.moved = true
  if (!panelDrag.moved) return
  dragging.value = true
  const w = viewport.value.w, h = viewport.value.h
  panelPos.value = {
    x: Math.min(Math.max(8, panelDrag.x + dx), Math.max(8, w - panelDrag.w - 8)),
    y: Math.min(Math.max(8, panelDrag.y + dy), Math.max(8, h - panelDrag.h - 8)),
  }
}
function onPanelUp() {
  document.removeEventListener('mousemove', onPanelMove)
  document.removeEventListener('mouseup', onPanelUp)
  if (panelDrag && panelDrag.moved) {
    try { localStorage.setItem(PANEL_POS_KEY, JSON.stringify(panelPos.value)) } catch (err) { /* 隐私模式忽略 */ }
  }
  dragging.value = false
  panelDrag = null
}
function onPanelReset() {
  panelPos.value = null
  try { localStorage.removeItem(PANEL_POS_KEY) } catch (e) { /* 隐私模式忽略 */ }
}

function onResize() {
  viewport.value = { w: window.innerWidth, h: window.innerHeight }
  pos.value = clampPoint(pos.value)
  if (panelPos.value) {
    const s = panelStyle.value
    panelPos.value = { x: parseFloat(s.left), y: parseFloat(s.top) }
  }
}

onMounted(() => {
  viewport.value = { w: window.innerWidth, h: window.innerHeight }
  let saved = null
  try { saved = JSON.parse(localStorage.getItem(POS_KEY) || 'null') } catch (e) { /* 忽略损坏数据 */ }
  const init = saved && Number.isFinite(saved.x) && Number.isFinite(saved.y) ? saved : defaultPos()
  pos.value = clampPoint(init)
  let savedPanel = null
  try { savedPanel = JSON.parse(localStorage.getItem(PANEL_POS_KEY) || 'null') } catch (e) { /* 忽略损坏数据 */ }
  if (savedPanel && Number.isFinite(savedPanel.x) && Number.isFinite(savedPanel.y)) panelPos.value = savedPanel
  posReady.value = true
  window.addEventListener('resize', onResize)

  let dismissed = false
  try { dismissed = sessionStorage.getItem(HINT_KEY) === '1' } catch (e) { /* 隐私模式忽略 */ }
  if (!dismissed) setTimeout(() => { if (!open.value) hintVisible.value = true }, 900)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('mouseup', onUp)
  document.removeEventListener('mousemove', onPanelMove)
  document.removeEventListener('mouseup', onPanelUp)
})

function dismiss() {
  hintVisible.value = false
  try { sessionStorage.setItem(HINT_KEY, '1') } catch (e) { /* 隐私模式忽略 */ }
}
function startChat() {
  hintVisible.value = false
  open.value = true
}

const msgs = ref([{
  from: 'bot',
  text: '您好！我是广西旅游网智能客服 🌟 门票、美食、会员、跟团游、支付退款等问题都可以问我，也可以点击下方快捷问题。',
  cards: [
    { label: '🎫 门票怎么买', key: 'ticket' },
    { label: '💸 怎么退款', key: 'refund' },
    { label: '👑 会员有什么优惠', key: 'vip' },
    { label: '🚌 跟团游怎么报名', key: 'tour' }
  ]
}])

const quicks = [
  { label: '门票购买', key: 'ticket' },
  { label: '退款/改签', key: 'refund' },
  { label: '会员优惠', key: 'vip' },
  { label: '跟团游报名', key: 'tour' },
  { label: '取餐号在哪看', key: 'pickno' },
  { label: '支付方式', key: 'pay' },
  { label: '开发票', key: 'invoice' },
  { label: '联系人工客服', key: 'human' }
]

const KB = {
  ticket: '购买门票：首页「景点门票」或在景点详情页选择游玩日期和数量 → 点击「立即预订」→ 支付宝沙箱付款，支付成功后订单秒级出票，在「我的订单」可随时查看，凭订单二维码或短信码入园。',
  refund: '退款规则：\n① 未使用的门票/美食订单：在「我的订单」里可申请取消，金额原路退回（1-3 个工作日到账）；\n② 跟团游：出发前 48 小时可免费取消，48 小时内收取 20% 损失费；\n③ 已核销/已使用的订单不支持退款。特殊天气闭园可全额退款。',
  vip: '会员优惠 👑：开通会员后全站 9 折 —— 门票、美食、奶茶、团购套餐、跟团游报名统统打折！连续包月仅 6.8 元/月，连续包年 68 元/年更划算。支付成功立即生效，登录后下单自动按会员价结算。',
  tour: '跟团游报名：进入「跟团游」页面 → 选城市筛选线路 → 点「立即报名」→ 选出发日期、人数，填联系电话 → 提交后支付宝沙箱付款。会员报名享 9 折。出发前会有客服电话/短信通知集合时间地点。更多疑问可拨打 19175657645。',
  pickno: '取餐号：购买美食/饮品并支付成功后，系统自动生成取餐号（如 B-7896），在「我的订单」里可以看到。到店后把取餐号报给店员即可取餐，无需排队。',
  pay: '支付方式：目前支持支付宝（沙箱环境）付款。下单后跳转支付宝收银台，可用沙箱买家账号完成支付；支付后页面每 3 秒自动确认到账，无需手动刷新。',
  invoice: '开发票：支持开具电子普通发票。请在「我的订单」找到对应订单 → 点「申请开票」→ 填写抬头（个人/企业）与邮箱，电子发票将在 24 小时内发送到您的邮箱。跟团游发票在行程结束后统一开具。',
  human: '人工客服：\n📞 服务热线：19175657645（9:00-22:00）\n💬 微信：waxrl0911（备注"旅游网咨询"）\n也欢迎到「服务中心」页面提交工单，我们会在 1 小时内回复您。'
}

const FAQ_ROUTE = [
  { k: ['门票', '买票', '购票'], a: 'ticket' },
  { k: ['退款', '退票', '取消', '改签'], a: 'refund' },
  { k: ['会员', 'vip', 'VIP', '折扣', '优惠'], a: 'vip' },
  { k: ['跟团', '旅游团', '报名', '线路'], a: 'tour' },
  { k: ['取餐', '取餐号', '排队'], a: 'pickno' },
  { k: ['支付', '付款', '支付宝', '沙箱'], a: 'pay' },
  { k: ['发票', '开票', '报销'], a: 'invoice' },
  { k: ['人工', '电话', '客服', '联系'], a: 'human' }
]

function push(m) { msgs.value.push(m); scrollBottom() }
function scrollBottom() { nextTick(() => { bodyEl.value?.scrollTo({ top: 99999, behavior: 'smooth' }) }) }

function answer(text) {
  const t = text.toLowerCase()
  for (const f of FAQ_ROUTE) if (f.k.some(k => t.includes(k.toLowerCase()))) return KB[f.a]
  if (t.includes('你好') || t.includes('您好') || t.includes('hi')) return '您好呀～请问有什么可以帮您？门票、美食、跟团游、会员都可以问我哦 😊'
  return '这个问题我可能没太理解～您可以把问题说得更具体一点（比如"门票怎么退款"），或直接联系人工客服：📞 19175657645 / 💬 微信 waxrl0911。'
}

function send() {
  const text = (draft.value || '').trim()
  if (!text) return
  draft.value = ''
  push({ from: 'user', text })
  typing.value = true
  setTimeout(() => {
    typing.value = false
    push({ from: 'bot', text: answer(text) })
  }, 600 + Math.random() * 500)
}

function onQuick(c) {
  if (c.go) { router.push(c.go); open.value = false; return }
  push({ from: 'user', text: c.label })
  typing.value = true
  setTimeout(() => {
    typing.value = false
    push({ from: 'bot', text: KB[c.key] || answer(c.label) })
  }, 500)
}
</script>

<style scoped>
.cs-wrap { position: fixed; z-index: 500; width: 64px; height: 64px;
  user-select: none; -webkit-user-select: none; }

/* 引导气泡：浅色卡片压在深色页面上，第一眼就能看到 */
.cs-hint-card {
  position: absolute; right: 82px; top: -12px; width: 232px;
  padding: 16px 18px 18px; border-radius: 12px; box-sizing: border-box;
  background: linear-gradient(135deg, #f6f9ff, #e7efff);
  border: 1px solid rgba(70, 110, 200, 0.18);
  box-shadow: 0 14px 38px rgba(8, 24, 60, 0.38);
}
.cs-hint-tail {
  position: absolute; right: -9px; top: 34px; width: 0; height: 0;
  border-left: 10px solid #eaf1ff;
  border-top: 8px solid transparent; border-bottom: 8px solid transparent;
}
/* 机器人被拖到左侧时，气泡翻到右边，避免跑出屏幕 */
.cs-hint-right { right: auto; left: 82px; }
.cs-hint-right .cs-hint-tail {
  right: auto; left: -9px; border-left: none; border-right: 10px solid #f2f7ff;
}
.cs-hint-close {
  position: absolute; top: 7px; right: 9px; padding: 4px;
  border: none; background: none; color: #b9cee8; font-size: 14px; line-height: 1; cursor: pointer;
}
.cs-hint-close:hover { color: #1e2a3f; }
.cs-hint-title {
  margin: 2px 24px 14px 0; font-size: 15px; font-weight: 600;
  line-height: 1.5; color: #1e2a3f;
}
.cs-hint-cta {
  border: none; border-radius: 6px; padding: 8px 20px; cursor: pointer;
  background: #1a73ff; color: #fff; font-size: 13px; font-weight: 600;
  box-shadow: 0 4px 12px rgba(26, 115, 255, 0.35);
}
.cs-hint-cta:hover { background: #0b5fe0; }
.cs-hint-enter-active, .cs-hint-leave-active { transition: all 0.32s cubic-bezier(0.2, 0.8, 0.2, 1); }
.cs-hint-enter-from, .cs-hint-leave-to { opacity: 0; transform: translateX(16px) scale(0.96); }

/* 机器人 + 纸飞机：两个常驻显眼入口 */
.cs-bot {
  position: absolute; right: 0; top: 0; width: 64px; height: 64px; padding: 0;
  display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; cursor: grab;
  filter: drop-shadow(0 8px 18px rgba(20, 80, 180, 0.45));
}
.cs-dragging .cs-bot { cursor: grabbing; }
.cs-bot:hover { filter: drop-shadow(0 10px 26px rgba(56, 140, 255, 0.75)); }
.cs-bot-svg { width: 60px; height: 60px; }
.cs-bot-alone { animation: csFloat 3.2s ease-in-out infinite; }
.cs-bot-dot {
  position: absolute; top: 8px; right: 7px; width: 10px; height: 10px;
  border-radius: 50%; background: #ef4444; border: 2px solid #fff;
  animation: csPulse 1.8s infinite;
}
/* 提示关掉后，「去咨询」贴在机器人正下方 */
.cs-mini-cta {
  position: absolute; left: 50%; top: 70px; transform: translateX(-50%);
  white-space: nowrap; padding: 8px 16px; border-radius: 999px; cursor: pointer;
  border: 1px solid rgba(125, 211, 252, 0.55);
  background: linear-gradient(120deg, rgba(18, 179, 214, 0.94), rgba(99, 102, 241, 0.94));
  color: #fff; font-size: 12.5px; font-weight: 700;
  box-shadow: 0 6px 18px rgba(20, 120, 220, 0.42);
}
.cs-mini-cta:hover { filter: brightness(1.14); }
@keyframes csFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }
@keyframes csPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.55); }
  50% { box-shadow: 0 0 0 7px rgba(239, 68, 68, 0); }
}

.cs-panel {
  position: fixed;
  width: 340px; height: 520px; border-radius: 18px;
  display: flex; flex-direction: column; overflow: hidden;
  background: rgba(10, 22, 38, 0.92) !important;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5);
}
.cs-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(120deg, rgba(18, 179, 214, 0.25), rgba(45, 212, 191, 0.15));
  cursor: move; user-select: none; -webkit-user-select: none;
}
.cs-dragging .cs-head { cursor: grabbing; }
.cs-head-l { display: flex; align-items: center; gap: 10px; }
.cs-avatar {
  width: 38px; height: 38px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #5eeaff, #7bffd4);
  color: #04121f; font-weight: 800; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.cs-head b { font-size: 13.5px; display: block; }
.cs-head small { color: var(--txt-sub, #c9d9f2); font-size: 11px; display: flex; align-items: center; gap: 4px; }
.on-dot { width: 6px; height: 6px; border-radius: 50%; background: #34d399; display: inline-block; }
.cs-close { background: none; border: none; color: var(--txt-sub, #c9d9f2); font-size: 15px; cursor: pointer; }

.cs-body { flex: 1; overflow-y: auto; padding: 14px 12px; display: flex; flex-direction: column; gap: 12px; }
.cs-msg { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; }
.cs-msg.user { align-items: flex-end; }
.cs-bubble {
  max-width: 84%; padding: 9px 12px; border-radius: 12px;
  font-size: 12.8px; line-height: 1.7; white-space: pre-wrap;
  background: rgba(255, 255, 255, 0.08); color: #dbeafe;
}
.cs-msg.user .cs-bubble { background: linear-gradient(120deg, #12b3d6, #2dd4bf); color: #fff; }
.cs-bubble.typing { animation: blink 1.2s infinite; }
@keyframes blink { 50% { opacity: 0.45; } }
.cs-cards { display: flex; flex-wrap: wrap; gap: 6px; max-width: 90%; }
.cs-card {
  border: 1px solid rgba(94, 234, 255, 0.4); background: rgba(94, 234, 255, 0.08);
  color: #7dd3fc; font-size: 11.5px; padding: 4px 10px; border-radius: 999px; cursor: pointer;
}
.cs-card:hover { background: rgba(94, 234, 255, 0.18); }

.cs-quick {
  display: flex; gap: 6px; padding: 8px 10px; overflow-x: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.08); scrollbar-width: none;
}
.cs-quick::-webkit-scrollbar { display: none; }
.cs-chip {
  flex-shrink: 0; border: 1px solid rgba(255, 255, 255, 0.16); background: rgba(255, 255, 255, 0.05);
  color: #9fb8d0; font-size: 11.5px; padding: 4px 10px; border-radius: 999px; cursor: pointer;
}
.cs-chip:hover { color: #7dd3fc; border-color: rgba(94, 234, 255, 0.4); }

.cs-input-row { display: flex; gap: 8px; padding: 10px 12px 6px; }
.cs-input {
  flex: 1; background: rgba(255, 255, 255, 0.07); border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 10px; padding: 9px 12px; color: #e2f4ff; font-size: 13px; outline: none;
}
.cs-input:focus { border-color: rgba(94, 234, 255, 0.5); }
.cs-send {
  background: linear-gradient(120deg, #12b3d6, #2dd4bf); border: none; border-radius: 10px;
  color: #04121f; font-weight: 700; font-size: 13px; padding: 0 16px; cursor: pointer;
}
.cs-foot {
  text-align: center; font-size: 11px; color: var(--txt-sub, #c9d9f2);
  padding: 4px 10px 10px; border-top: 1px dashed rgba(255, 255, 255, 0.08);
}

.cs-pop-enter-active, .cs-pop-leave-active { transition: all 0.25s ease; }
.cs-pop-enter-from, .cs-pop-leave-to { opacity: 0; transform: translateY(16px) scale(0.96); }
</style>
