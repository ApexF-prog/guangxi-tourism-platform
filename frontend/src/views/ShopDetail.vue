<template>
  <main class="page" v-if="shop">
    <div class="head glass">
      <img :src="shop.coverImg" class="head-img" />
      <div class="head-info">
        <h1 class="head-name">
          <el-tag :type="shop.type === 'FOOD' ? 'danger' : 'success'" effect="dark" round>
            {{ shop.type === 'FOOD' ? '美食' : '奶茶饮品' }}
          </el-tag>
          {{ shop.name }}
        </h1>
        <p class="head-desc">{{ shop.description }}</p>
        <div class="guide glass">
          <b>🧭 到店指引</b>
          <p>{{ shop.address }}</p>
          <p v-if="shop.phone">☎ {{ shop.phone }} · 位于「{{ spotName }}」景区内</p>
        </div>
      </div>
    </div>

    <h2 class="page-title" style="margin-top: 30px">🛒 店内在售</h2>
    <div class="prod-list">
      <div v-for="p in products" :key="p.id" class="prod-item glass">
        <img :src="p.coverImg" class="prod-thumb" />
        <div class="prod-info">
          <div class="prod-name">{{ p.name }}</div>
          <div class="prod-desc">{{ p.description }}</div>
        </div>
        <div class="prod-buy">
          <div class="price-line">
            <span class="price" :class="{ 'price-free': p.price <= 0 }">{{ p.price > 0 ? '¥' + p.price : '免费预约' }}</span>
            <span v-if="p.originalPrice && p.price > 0" class="origin">¥{{ p.originalPrice }}</span>
          </div>
          <el-input-number v-model="qtys[p.id]" :min="0" :max="9" size="small" class="qty-picker" />
          <button class="btn-grad buy-btn" @click="buy(p)">购买</button>
          <button class="btn-ghost review-btn" @click="openReview(p)">评价</button>
        </div>
      </div>
      <el-empty v-if="products.length === 0" description="店家整理中，敬请期待" />
    </div>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="发表评价" width="420px">
      <p class="review-tip">已购可评 · 每个商品最多评价 2 次</p>
      <el-rate v-model="reviewForm.rating" :max="5" style="margin-bottom: 12px" />
      <el-input v-model="reviewForm.content" type="textarea" :rows="4" maxlength="300" show-word-limit
                placeholder="说说味道和服务怎么样…" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">发布评价</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { currentUser, requireLogin, recordBrowse } from '../api.js'

const route = useRoute()
const shop = ref(null)
const spotName = ref('')
const products = ref([])
const qtys = ref({})
const reviewVisible = ref(false)
const reviewForm = ref({ productId: null, rating: 5, content: '' })

onMounted(async () => {
  const data = await request.get('/shop/' + route.params.id)
  shop.value = data.shop
  spotName.value = data.spotName
  products.value = data.products
  // 数量默认 0（未选择），点 + 增加、可减回 0
  data.products.forEach(p => { qtys.value[p.id] = 0 })
  recordBrowse('SHOP', Number(route.params.id))
})

async function buy(p) {
  if (requireLogin()) return
  const qty = qtys.value[p.id] ?? 0
  if (!qty) return ElMessage.warning('请先选择数量（点 + 增加）')
  const order = await request.post('/order/create', { productId: p.id, quantity: qty })
  location.href = '/pay/' + order.orderNo
}

function openReview(p) {
  if (!currentUser()) return (location.href = '/login')
  reviewForm.value = { productId: p.id, rating: 5, content: '' }
  reviewVisible.value = true
}

async function submitReview() {
  await request.post('/review', reviewForm.value)
  ElMessage.success('评价发布成功')
  reviewVisible.value = false
}
</script>

<style scoped>
.head { overflow: hidden; display: grid; grid-template-columns: 1fr 1.3fr; min-height: 280px; }
.head-img { width: 100%; height: 100%; object-fit: cover; }
.head-info { padding: 26px 30px; display: flex; flex-direction: column; gap: 12px; }
.head-name { margin: 0; font-size: 24px; display: flex; align-items: center; gap: 10px; }
.head-desc { color: var(--txt-sub); line-height: 1.7; margin: 0; flex: 1; }
.guide { padding: 14px 16px; border-radius: 12px; }
.guide b { color: var(--cyan); }
.guide p { margin: 6px 0 0; color: var(--txt-sub); font-size: 13px; line-height: 1.6; }

.prod-list { display: flex; flex-direction: column; gap: 14px; margin-top: 16px; }
.prod-item { display: flex; align-items: center; gap: 18px; padding: 14px 18px; }
.prod-thumb { width: 110px; height: 78px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.prod-info { flex: 1; min-width: 0; }
.prod-name { font-weight: 600; }
.prod-desc { color: var(--txt-sub); font-size: 13px; margin-top: 6px; }
.prod-buy { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.price-line .price { color: var(--cyan); font-size: 22px; font-weight: 700; }
.price-line .origin { color: var(--txt-sub); text-decoration: line-through; margin-left: 6px; }
.review-btn { padding: 8px 16px; }
.review-tip { color: var(--txt-sub); font-size: 12px; margin: 0 0 10px; }
@media (max-width: 760px) {
  .head { grid-template-columns: 1fr; }
  .head-img { height: 200px; }
}
</style>
