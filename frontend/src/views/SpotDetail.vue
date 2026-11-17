<template>
  <main class="page" v-if="spot">
    <div class="hero glass">
      <img :src="spot.coverImg" class="hero-img" />
      <div class="hero-info">
        <h1 class="hero-name">{{ spot.name }}
          <el-tag v-if="spot.level !== '无'" type="warning" effect="dark" round>{{ spot.level }}</el-tag>
        </h1>
        <p class="hero-desc">{{ spot.description }}</p>
        <div class="hero-meta">
          <span>📍 {{ spot.address }}</span>
          <span class="rating">★ {{ spot.rating }}</span>
          <span>🔥 热度 {{ spot.hot }}</span>
        </div>
      </div>
    </div>

    <h2 class="page-title" style="margin-top: 34px">🎫 门票 · 团购套餐</h2>
    <div class="prod-list">
      <div v-for="p in products" :key="p.id" class="prod-item glass">
        <img :src="p.coverImg" class="prod-thumb" />
        <div class="prod-info">
          <div class="prod-name">
            <el-tag size="small" :type="p.type === 'PACKAGE' ? 'danger' : 'primary'" effect="dark" round>
              {{ p.type === 'PACKAGE' ? '团购套餐' : '门票' }}
            </el-tag>
            {{ p.name }}
          </div>
          <div class="prod-desc">{{ p.description }}</div>
        </div>
        <div class="prod-buy">
          <div class="price-line">
            <span class="price" :class="{ 'price-free': p.price <= 0 }">{{ p.price > 0 ? '¥' + p.price : '免费预约' }}</span>
            <span v-if="p.originalPrice && p.price > 0" class="origin">¥{{ p.originalPrice }}</span>
          </div>
          <el-date-picker v-if="needDate" v-model="dates[p.id]" type="date" placeholder="选择游玩日期"
                          format="YYYY年MM月DD日" value-format="YYYY-MM-DD"
                          :disabled-date="d => d.getTime() < Date.now() - 86400000" style="width: 170px" />
          <el-input-number v-model="qtys[p.id]" :min="0" :max="9" size="small" class="qty-picker" />
          <button class="btn-grad buy-btn" @click="buy(p)">立即预订</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { currentUser, requireLogin, recordBrowse } from '../api.js'

const route = useRoute()
const router = useRouter()
const spot = ref(null)
const products = ref([])
const dates = ref({})
const qtys = ref({})

const needDate = computed(() => products.value.some(p => p.type === 'TICKET' || p.type === 'PACKAGE'))

onMounted(async () => {
  const data = await request.get('/spot/' + route.params.id)
  spot.value = data.spot
  products.value = data.products
  // 数量默认 0（未选择），点 + 增加、可减回 0
  data.products.forEach(p => { qtys.value[p.id] = 0 })
  recordBrowse('SPOT', Number(route.params.id), data.spot?.city)
})

async function buy(p) {
  if (requireLogin()) return
  const needVisitDate = p.type === 'PACKAGE' || p.type === 'TICKET'
  const date = needVisitDate ? dates.value[p.id] : undefined
  const qty = qtys.value[p.id] ?? 0
  if (!qty) return ElMessage.warning('请先选择数量（点 + 增加）')
  if (needVisitDate && !date) return ElMessage.warning('请先选择游玩日期')
  const order = await request.post('/order/create', {
    productId: p.id, quantity: qty, visitDate: date
  })
  router.push('/pay/' + order.orderNo)
}
</script>

<style scoped>
.hero { overflow: hidden; display: grid; grid-template-columns: 1.2fr 1fr; min-height: 320px; }
.hero-img { width: 100%; height: 100%; object-fit: cover; }
.hero-info { padding: 30px 32px; display: flex; flex-direction: column; gap: 14px; }
.hero-name { margin: 0; font-size: 28px; display: flex; align-items: center; gap: 10px; }
.hero-desc { color: var(--txt-sub); line-height: 1.8; flex: 1; }
.hero-meta { display: flex; gap: 20px; color: var(--txt-sub); font-size: 14px; }
.rating { color: #fbbf24; }

.prod-list { display: flex; flex-direction: column; gap: 14px; margin-top: 16px; }
.prod-item { display: flex; align-items: center; gap: 18px; padding: 14px 18px; }
.prod-thumb { width: 110px; height: 78px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.prod-info { flex: 1; min-width: 0; }
.prod-name { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.prod-desc { color: var(--txt-sub); font-size: 13px; margin-top: 6px; }
.prod-buy { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; justify-content: flex-end; }
.price-line .price { color: var(--cyan); font-size: 22px; font-weight: 700; }
.price-line .origin { color: var(--txt-sub); text-decoration: line-through; margin-left: 6px; }
.buy-btn { padding: 9px 20px; }
@media (max-width: 760px) {
  .hero { grid-template-columns: 1fr; }
  .hero-img { height: 220px; }
  .prod-item { flex-direction: column; align-items: flex-start; }
  .prod-buy { justify-content: flex-start; }
}
</style>
