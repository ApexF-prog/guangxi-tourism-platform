<template>
  <main class="page">
    <h1 class="page-title">我的订单</h1>
    <p class="page-sub">待支付订单可继续支付 · 已支付的商品可以评价（每商品限 2 次）</p>

    <div class="order-list" v-loading="loading">
      <div v-for="o in orders" :key="o.id" class="order-card glass">
        <img :src="covers[o.productId] || '/images/tesemeishi.jpg'" class="thumb" />
        <div class="info">
          <div class="name">{{ o.productName }}
            <el-tag size="small" effect="dark" round :type="tagType(o.productType)">{{ typeLabel(o.productType) }}</el-tag>
            <el-tag v-if="o.vipPrice === 1" size="small" effect="dark" round type="warning">👑 会员价</el-tag>
          </div>
          <div class="meta">
            订单号 {{ o.orderNo }} · {{ o.createdAt?.replace('T', ' ').slice(0, 16) }}
            <template v-if="o.visitDate"> · 游玩 {{ o.visitDate }}</template>
          </div>
          <!-- 取餐号：仅美食/饮品订单 -->
          <div v-if="o.pickNo" class="pick-row">
            <span class="pick-label">取餐号</span>
            <span class="pick-no">{{ o.pickNo }}</span>
            <span class="pick-tip">{{ o.status === 'PAID' ? '到店报号即可取餐' : '支付成功后生效' }}</span>
          </div>
          <!-- 订单履约进度 -->
          <div class="steps">
            <span v-for="(s, i) in stepsOf(o)" :key="i" class="step" :class="{ done: s.done, cancelled: o.status === 'CANCELED' && i === 1 }">
              <i></i>{{ s.label }}
            </span>
          </div>
        </div>
        <div class="right">
          <span class="total">¥{{ o.total }}</span>
          <el-tag :type="statusTag(o.status)" effect="dark" round>{{ statusLabel(o.status) }}</el-tag>
          <div class="actions">
            <button v-if="o.status === 'PENDING'" class="btn-grad" style="padding: 7px 16px; font-size: 13px"
                    @click="$router.push('/pay/' + o.orderNo)">去支付</button>
            <button v-if="o.status === 'PAID' && canReview(o)" class="btn-ghost" style="padding: 7px 14px; font-size: 13px"
                    @click="openReview(o)">评价</button>
            <button v-if="o.status === 'PENDING'" class="btn-ghost" style="padding: 7px 14px; font-size: 13px"
                    @click="cancel(o)">取消</button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="还没有订单，快去逛逛吧" />
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="reviewVisible" title="发表评价" width="420px">
      <p style="color: var(--txt-sub); font-size: 12px; margin: 0 0 10px">已购可评 · 每个商品最多评价 2 次</p>
      <el-rate v-model="reviewForm.rating" :max="5" style="margin-bottom: 12px" />
      <el-input v-model="reviewForm.content" type="textarea" :rows="4" maxlength="300" show-word-limit
                placeholder="这次旅行/美食体验怎么样…" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api.js'

const orders = ref([])
const covers = ref({})
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const reviewedProducts = ref(new Set())
const reviewVisible = ref(false)
const reviewForm = ref({ productId: null, rating: 5, content: '' })

function typeLabel(t) { return { TICKET: '门票', FOOD: '美食', DRINK: '饮品', PACKAGE: '套餐' }[t] || t }
function tagType(t) { return t === 'PACKAGE' ? 'danger' : 'primary' }
function statusLabel(s) { return { PENDING: '待支付', PAID: '已支付', CANCELED: '已取消' }[s] }
function statusTag(s) { return { PENDING: 'warning', PAID: 'success', CANCELED: 'info' }[s] }
function canReview(o) { return !reviewedProducts.value.has(o.productId) }
function stepsOf(o) {
  const labels = o.status === 'CANCELED'
    ? ['提交订单', '订单已取消', '已关闭', '已关闭']
    : ['提交订单', '完成支付', '出票/备餐', '可评价/取号']
  const reached = o.status === 'PAID' ? 4 : o.status === 'CANCELED' ? 1 : 1
  return labels.map((label, i) => ({ label, done: i < reached }))
}

async function load(p = 1) {
  loading.value = true
  page.value = p
  try {
    const data = await request.get('/order/my', { params: { page: p, size } })
    orders.value = data.records
    total.value = Number(data.total)
    // 拉商品封面
    const ids = [...new Set(orders.value.map(o => o.productId))]
    await Promise.all(ids.filter(id => !covers.value[id]).map(async id => {
      try {
        const detail = await request.get('/product/' + id, { silent: true })
        covers.value[id] = detail.product?.coverImg
      } catch { /* 忽略 */ }
    }))
  } finally { loading.value = false }
}

async function cancel(o) {
  await request.post(`/order/${o.orderNo}/cancel`)
  ElMessage.success('订单已取消')
  load(page.value)
}

function openReview(o) {
  reviewForm.value = { productId: o.productId, rating: 5, content: '' }
  reviewVisible.value = true
}

async function submitReview() {
  try {
    await request.post('/review', reviewForm.value)
    ElMessage.success('评价发布成功')
    reviewedProducts.value.add(reviewForm.value.productId)
    reviewVisible.value = false
  } catch { /* 后端提示已弹 */ }
}

onMounted(load)
</script>

<style scoped>
.order-list { display: flex; flex-direction: column; gap: 14px; margin-top: 18px; min-height: 200px; }
.order-card { display: flex; align-items: center; gap: 16px; padding: 14px 18px; }
.thumb { width: 96px; height: 68px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.info { flex: 1; min-width: 0; }
.name { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.meta { color: var(--txt-sub); font-size: 12px; margin-top: 7px; }
.pick-row { display: flex; align-items: center; gap: 10px; margin-top: 9px; }
.pick-label {
  font-size: 11.5px; color: #052a33; background: linear-gradient(120deg, #5eeaff, #7bffd4);
  padding: 2px 8px; border-radius: 6px; font-weight: 700;
}
.pick-no {
  font-size: 20px; font-weight: 800; letter-spacing: 2px; color: var(--cyan);
  font-family: "SF Mono", Consolas, monospace;
}
.pick-tip { font-size: 11.5px; color: var(--txt-sub); }
.steps { display: flex; align-items: center; gap: 0; margin-top: 12px; }
.step { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: var(--txt-sub); white-space: nowrap; }
.step:not(:last-child)::after { content: ''; width: 26px; height: 1px; margin: 0 8px; background: rgba(255,255,255,.16); }
.step i { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,.18); }
.step.done { color: #4ade80; }
.step.done i { background: #4ade80; }
.step.cancelled { color: #f87171; }
.step.cancelled i { background: #f87171; }
.right { display: flex; align-items: center; gap: 14px; }
.total { color: var(--cyan); font-weight: 700; font-size: 18px; }
.actions { display: flex; gap: 8px; }
.pager { display: flex; justify-content: center; margin-top: 28px; }
@media (max-width: 700px) {
  .order-card { flex-wrap: wrap; }
}
</style>
