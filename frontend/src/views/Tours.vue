<template>
  <main class="page tours-page">
    <!-- 顶部横幅：仿去哪儿跟团游 -->
    <div class="tour-hero glass">
      <div class="hero-txt">
        <h1 class="grad-text">跟团游 · 野行广西路线</h1>
        <p>桂林 · 柳州 · 南宁 · 北海 —— 精选小团，会员报名立享 9 折</p>
      </div>
      <!-- 咨询条 -->
      <div class="contact-bar">
        <button class="contact-item" @click="copy(contact.phone, '电话')">
          <span class="c-ico">📞</span>
          <span class="c-txt"><small>咨询电话</small><b>{{ contact.phone }}</b></span>
        </button>
        <button class="contact-item" @click="copy(contact.wechat, '微信号')">
          <span class="c-ico">💬</span>
          <span class="c-txt"><small>微信咨询</small><b>{{ contact.wechat }}</b></span>
        </button>
      </div>
    </div>

    <!-- 城市筛选 -->
    <div class="filter-row glass">
      <button v-for="c in cityTabs" :key="c" class="chip" :class="{ on: cur === c }" @click="cur = c">{{ c }}</button>
      <span class="spacer"></span>
      <span class="vip-tip">👑 会员报名 9 折，<router-link to="/vip" class="lnk">开通会员</router-link></span>
    </div>

    <!-- 价格口径：线路为项目演示自拟，不能冒充旅行社实时报价 -->
    <div class="price-notice glass">
      <span class="pn-ico">ℹ️</span>
      <span>以下为 <b>演示参考价（每人起）</b>，非旅行社实时报价；实际线路与包含项目以报名确认为准。景区门票按 2026 年公示价展示，免费景区标注「免费开放」。</span>
    </div>

    <!-- 线路卡片 -->
    <div class="tour-grid" v-loading="loading">
      <div v-for="t in list" :key="t.id" class="tour-card glass glass-hover">
        <div class="cover-wrap" @click="open(t)">
          <img :src="t.coverImg" class="cover" loading="lazy" />
          <span class="days">{{ t.days }} 天 {{ t.days > 1 ? '跟团' : '日游' }}</span>
          <span class="city-chip">{{ t.city }}出发</span>
        </div>
        <div class="body">
          <h3 class="title" @click="open(t)">{{ t.title }}</h3>
          <div class="hl-row">
            <el-tag v-for="h in hl(t.highlights).slice(0, 3)" :key="h" size="small" effect="dark" round>{{ h }}</el-tag>
          </div>
          <div class="meta">🚌 {{ t.departure }}出发 · 余位 <b :style="{color: t.stock <= 5 ? '#f87171' : ''}">{{ t.stock }}</b></div>
          <div class="buy-row">
            <div class="price-line">
              <span class="price">¥{{ vipPrice(t) }}<small class="ref-tag">起</small></span>
              <s v-if="isVip">¥{{ t.price }}</s>
              <span v-if="isVip" class="vip-badge">会员价</span>
            </div>
            <button class="btn-grad" style="padding: 8px 22px; font-size: 13.5px" @click="open(t)">立即报名</button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="该城市暂无线路" style="grid-column: 1/-1" />
    </div>

    <!-- 我的报名 -->
    <div class="my-booking glass" v-if="me">
      <div class="plans-head"><span class="bar"></span>我的报名</div>
      <div v-for="b in myBookings" :key="b.id" class="b-row">
        <div class="b-info">
          <b>{{ b.tourTitle }}</b>
          <small>报名号 {{ b.bookingNo }} · 出发 {{ b.travelDate }} · {{ b.people }} 人 · 联系电话 {{ b.contactPhone }}</small>
        </div>
        <span class="b-total">¥{{ b.total }}</span>
        <el-tag :type="{ PENDING: 'warning', PAID: 'success', CANCELED: 'info' }[b.status]" effect="dark" round>
          {{ { PENDING: '待支付', PAID: '已报名', CANCELED: '已取消' }[b.status] }}
        </el-tag>
        <button v-if="b.status === 'PENDING'" class="btn-grad" style="padding: 6px 16px; font-size: 12.5px"
                @click="payBooking(b)">去支付</button>
        <button v-if="b.status === 'PENDING'" class="btn-ghost" style="padding: 6px 14px; font-size: 12.5px"
                @click="cancelBooking(b)">取消</button>
      </div>
      <el-empty v-if="!myBookings.length" description="还没有报名，挑一条路线出发吧" :image-size="70" />
    </div>

    <!-- 报名弹窗 -->
    <el-dialog v-model="dialog" :title="sel ? '报名 · ' + sel.title : '报名'" width="520px">
      <template v-if="sel">
        <div class="dlg-tour glass">
          <img :src="sel.coverImg" class="dlg-cover" />
          <div>
            <b>{{ sel.title }}</b>
            <p class="dlg-itin">{{ sel.itinerary }}</p>
          </div>
        </div>
        <div class="dlg-form">
          <div class="f-row"><span class="f-label">出发日期</span>
            <el-date-picker v-model="form.travelDate" type="date" placeholder="选择出发日期"
                            format="YYYY年MM月DD日" value-format="YYYY-MM-DD"
                            :disabled-date="d => d.getTime() < Date.now() - 86400000" style="width: 220px" />
          </div>
          <div class="f-row"><span class="f-label">出行人数</span>
            <el-input-number v-model="form.people" :min="0" :max="Math.min(9, sel.stock)" class="qty-picker" />
          </div>
          <div class="f-row"><span class="f-label">联系电话</span>
            <el-input v-model="form.contactPhone" placeholder="用于出行通知，如 13800000001" style="width: 220px" />
          </div>
          <div class="f-row price-row">
            <span class="f-label">费用</span>
            <div class="price-detail">
              <template v-if="isVip">
                <s style="color: var(--txt-sub)">¥{{ sel.price }} × {{ form.people }} 人</s>
                <b class="grad-text" style="font-size: 22px">¥{{ (vipUnit * form.people).toFixed(2) }}</b>
                <span class="vip-badge">会员 9 折</span>
              </template>
              <template v-else>
                <b class="grad-text" style="font-size: 22px">¥{{ sel.price }} × {{ form.people }} 人</b>
                <span style="color: var(--txt-sub); font-size: 12px">开通会员可享 9 折</span>
              </template>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <template v-if="!paying">
          <el-button @click="dialog = false">取消</el-button>
          <el-button type="primary" @click="submit">提交报名</el-button>
        </template>
        <template v-else>
          <div style="width: 100%; text-align: center">
            <p style="color: var(--txt-sub); font-size: 13px; margin: 0 0 10px">
              报名单已生成（{{ booking?.bookingNo }}），请在支付宝沙箱收银台完成付款，本页每 3 秒自动确认。
            </p>
            <button class="btn-grad" style="padding: 11px 34px" @click="payBooking(booking)">
              {{ paid ? '已支付 ✓' : (jumping ? '正在跳转…' : '前往支付宝支付') }}
            </button>
            <div style="color: var(--txt-sub); font-size: 12px; margin-top: 8px" v-if="jumping && !paid">
              等待付款确认中… 已查询 {{ polls }} 次
            </div>
            <div style="margin-top: 10px" v-if="paid">
              <el-button type="success" round @click="closeAll">报名成功，完成</el-button>
            </div>
          </div>
        </template>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import request, { currentUser, requireLogin } from '../api.js'
import { city } from '../store/city.js'

const CONTACT = { phone: '19175657645', wechat: 'waxrl0911' }
const contact = ref(CONTACT)
const cityTabs = ['全部', '桂林', '柳州', '南宁', '北海']
const cur = ref('全部')
const list = ref([])
const loading = ref(false)
const me = ref(currentUser())
const myBookings = ref([])

// 报名弹窗
const dialog = ref(false)
const sel = ref(null)
const form = ref({ travelDate: '', people: 0, contactPhone: '' })
const paying = ref(false)
const booking = ref(null)
const paid = ref(false)
const jumping = ref(false)
const polls = ref(0)
let timer = null

const isVip = computed(() => me.value?.vipLevel === 1 && me.value?.vipExpire &&
  new Date(me.value.vipExpire).getTime() > Date.now())
const vipUnit = computed(() => sel.value
  ? (isVip.value ? +(sel.value.price * 0.9).toFixed(2) : +sel.value.price) : 0)

function hl(s) { return (s || '').split(',').filter(Boolean) }
function vipPrice(t) { return isVip.value ? (t.price * 0.9).toFixed(2).replace(/\.00$/, '') : t.price }

async function load() {
  loading.value = true
  try {
    list.value = await request.get('/tour/list', { params: { city: cur.value } })
  } finally { loading.value = false }
}

async function loadMine() {
  if (!me.value) return
  try { myBookings.value = await request.get('/tour/my', { silent: true }) } catch { /* 未登录忽略 */ }
}

function open(t) {
  if (requireLogin()) return
  sel.value = t
  form.value = { travelDate: '', people: 0, contactPhone: me.value.username?.match(/^1\d{10}$/) ? me.value.username : '' }
  paying.value = false
  paid.value = false
  dialog.value = true
}

async function submit() {
  if (!form.value.travelDate) return ElMessage.warning('请选择出发日期')
  if (!form.value.people) return ElMessage.warning('请先选择出行人数（点 + 增加）')
  if (!/^1[3-9]\d{9}$/.test(form.value.contactPhone || '')) return ElMessage.warning('请填写正确的 11 位联系电话')
  booking.value = await request.post('/tour/book', {
    tourId: sel.value.id, travelDate: form.value.travelDate,
    people: form.value.people, contactPhone: form.value.contactPhone
  })
  ElMessage.success('报名成功，请完成支付')
  paying.value = true
}

function payBooking(b) {
  if (!b) return
  jumping.value = true
  request.post('/tour/pay', { bookingNo: b.bookingNo }).then(form => {
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
        const r = await request.get('/tour/query/' + b.bookingNo, { silent: true })
        polls.value++
        if (r.status === 'PAID') {
          clearInterval(timer)
          paid.value = true
          loadMine()
        }
      } catch { /* 继续轮询 */ }
    }, 3000)
  }).catch(() => { jumping.value = false })
}

async function cancelBooking(b) {
  await request.post('/tour/cancel/' + b.bookingNo)
  ElMessage.success('已取消报名')
  loadMine(); load()
}

function closeAll() {
  dialog.value = false
  load()
}

function copy(text, label) {
  navigator.clipboard?.writeText(text).then(
    () => ElMessage.success(label + '已复制：' + text),
    () => ElMessage.info(label + '：' + text)
  )
}

watch(cur, load)
onMounted(() => {
  load()
  loadMine()
  request.get('/tour/contact', { silent: true }).then(c => { if (c) contact.value = c }).catch(() => {})
})
onBeforeUnmount(() => timer && clearInterval(timer))
</script>

<style scoped>
.tour-hero {
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 26px 28px;
  background: linear-gradient(120deg, rgba(94, 234, 255, 0.12), rgba(129, 140, 248, 0.1));
}
.hero-txt h1 { margin: 0 0 8px; font-size: 26px; }
.hero-txt p { margin: 0; color: var(--txt-sub); font-size: 13.5px; }
.contact-bar { display: flex; gap: 12px; }
.contact-item {
  display: flex; align-items: center; gap: 10px;
  background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(94, 234, 255, 0.3);
  border-radius: 12px; padding: 10px 16px; cursor: pointer; color: var(--txt);
}
.contact-item:hover { background: rgba(94, 234, 255, 0.12); }
.c-ico { font-size: 20px; }
.c-txt { display: flex; flex-direction: column; text-align: left; }
.c-txt small { color: var(--txt-sub); font-size: 11px; }
.c-txt b { font-size: 15px; letter-spacing: 0.5px; }

.filter-row {
  margin-top: 16px; display: flex; align-items: center; gap: 8px; padding: 12px 16px; flex-wrap: wrap;
}
.chip {
  border: 1px solid rgba(255, 255, 255, 0.16); background: rgba(255, 255, 255, 0.05);
  color: var(--txt-sub); border-radius: 999px; padding: 6px 18px; cursor: pointer; font-size: 13px;
}
.chip.on { background: var(--cyan); border-color: var(--cyan); color: #04121f; font-weight: 700; }
.spacer { flex: 1; }
.vip-tip { color: var(--txt-sub); font-size: 12.5px; }
.lnk { color: var(--cyan); text-decoration: none; }

.tour-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr)); gap: 18px; margin-top: 18px; }
.tour-card { overflow: hidden; display: flex; flex-direction: column; }
.cover-wrap { position: relative; height: 170px; overflow: hidden; cursor: pointer; }
.cover { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.tour-card:hover .cover { transform: scale(1.06); }
.days {
  position: absolute; left: 12px; top: 12px;
  background: rgba(4, 18, 31, 0.72); color: #fff; font-size: 12px;
  padding: 3px 10px; border-radius: 999px;
}
.city-chip {
  position: absolute; right: 12px; top: 12px;
  background: linear-gradient(120deg, #5eeaff, #7bffd4); color: #04121f;
  font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 999px;
}
.body { padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 10px; flex: 1; }
.title { margin: 0; font-size: 15.5px; line-height: 1.5; cursor: pointer; }
.title:hover { color: var(--cyan); }
.hl-row { display: flex; gap: 6px; flex-wrap: wrap; }
.meta { color: var(--txt-sub); font-size: 12.5px; }
.buy-row { display: flex; align-items: center; justify-content: space-between; margin-top: auto; }
.price-line .price { color: var(--cyan); font-size: 22px; font-weight: 700; }
.price-line s { color: var(--txt-sub); font-size: 12.5px; margin-left: 6px; }
.ref-tag { margin-left: 4px; font-size: 11px; font-weight: 500; color: var(--txt-sub); }
.price-notice { display: flex; align-items: flex-start; gap: 10px; margin: 14px 0 4px;
  padding: 12px 16px; border-radius: 12px; font-size: 12.5px; line-height: 1.7;
  color: var(--txt-sub); border-left: 3px solid #38bdf8; }
.price-notice b { color: #7dd3fc; }
.pn-ico { flex: none; font-size: 15px; }
.vip-badge {
  margin-left: 6px; font-size: 10px; font-weight: 800; color: #04121f;
  background: linear-gradient(120deg, #fbbf24, #f59e0b); padding: 1px 6px; border-radius: 4px;
  vertical-align: 3px;
}

.my-booking { margin-top: 22px; padding: 18px 20px; }
.plans-head { display: flex; align-items: center; gap: 10px; font-weight: 700; margin-bottom: 12px; }
.plans-head .bar { width: 4px; height: 16px; border-radius: 2px; background: var(--cyan); box-shadow: 0 0 8px var(--cyan); }
.b-row { display: flex; align-items: center; gap: 14px; padding: 12px 4px; border-bottom: 1px dashed rgba(255, 255, 255, 0.1); }
.b-row:last-of-type { border-bottom: none; }
.b-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.b-info small { color: var(--txt-sub); font-size: 12px; }
.b-total { color: var(--cyan); font-weight: 700; }

.dlg-tour { display: flex; gap: 14px; padding: 12px; margin-bottom: 16px; }
.dlg-cover { width: 130px; height: 88px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.dlg-tour b { font-size: 14px; }
.dlg-itin { color: var(--txt-sub); font-size: 12px; line-height: 1.7; margin: 6px 0 0;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.dlg-form { display: flex; flex-direction: column; gap: 14px; }
.f-row { display: flex; align-items: center; gap: 14px; }
.f-label { width: 70px; color: var(--txt-sub); font-size: 13.5px; flex-shrink: 0; }
.price-detail { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

@media (max-width: 760px) {
  .tour-hero { flex-direction: column; align-items: flex-start; }
  .tour-grid { grid-template-columns: 1fr; }
}
</style>
