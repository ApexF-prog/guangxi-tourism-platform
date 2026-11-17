<template>
  <section class="ai-panel glass">
    <div class="ai-head">
      <div>
        <h2 class="ai-title">✨ AI 智选 <span class="grad-text">· 不知道去哪？让它帮你选</span></h2>
        <p class="ai-sub">告诉它预算、口味、同行人，AI 会结合评分与销量推荐景点和商家，并说明推荐理由</p>
      </div>
      <span class="ai-badge">DeepSeek 驱动</span>
    </div>

    <div class="ai-input-row">
      <input
        v-model="preference"
        class="ai-input"
        placeholder="例如：周末两天，预算 500，带小孩，想吃本地小吃，不想爬太多山"
        @keyup.enter="ask"
      />
      <button class="btn-grad ai-btn" :disabled="loading" @click="ask">
        {{ loading ? 'AI 推荐中…' : '智能推荐' }}
      </button>
    </div>

    <div class="chips">
      <button v-for="c in chips" :key="c" class="chip" @click="pick(c)">{{ c }}</button>
    </div>

    <div v-if="loading" class="ai-loading">
      <span class="dot" /><span class="dot" /><span class="dot" />
      正在分析 {{ city }} 的景点评分与商家销量…
    </div>

    <div v-if="result" class="ai-result">
      <p class="reply">💡 {{ result.reply }}</p>

      <div v-if="result.spots?.length" class="group">
        <h3 class="group-title">🏞 推荐景点</h3>
        <div class="rec-grid">
          <div v-for="s in mappedSpots" :key="s.id" class="rec-card glass-hover">
            <img :src="s.img" class="rec-img" />
            <div class="rec-body">
              <div class="rec-name">{{ s.name }}</div>
              <div class="rec-meta">
                <span class="star">★ {{ s.rating }}</span>
                <span>{{ s.city }}</span>
                <span class="price">{{ s.price > 0 ? '¥' + s.price : '免费' }}</span>
              </div>
              <p class="reason">{{ s.reason }}</p>
              <button class="btn-grad rec-btn" @click="goSpot(s.id)">查看并下单</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="result.products?.length" class="group">
        <h3 class="group-title">🍜 推荐商家商品</h3>
        <div class="rec-grid">
          <div v-for="p in mappedProducts" :key="p.id" class="rec-card glass-hover">
            <img :src="p.img" class="rec-img" />
            <div class="rec-body">
              <div class="rec-name">{{ p.name }}</div>
              <div class="rec-meta">
                <span class="price" :class="{ 'price-free': p.price <= 0 }">{{ p.price > 0 ? '¥' + p.price : '免费预约' }}</span>
                <span>已售 {{ p.sales }}</span>
                <el-tag size="small" effect="dark" round>{{ typeLabel(p.type) }}</el-tag>
              </div>
              <p class="reason">{{ p.reason }}</p>
              <button class="btn-grad rec-btn" @click="goProduct(p)">去下单</button>
            </div>
          </div>
        </div>
      </div>

      <p class="engine">
        推荐引擎：{{ result.engine === 'deepseek' ? 'DeepSeek 大模型' : '本地评分兜底（AI 服务暂不可用）' }}
      </p>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { currentUser, requireLogin } from '../api.js'
import { city, cityGroup } from '../store/city.js'

const router = useRouter()
const preference = ref('')
const loading = ref(false)
const result = ref(null)
const spotMap = ref({})
const productMap = ref({})

const chips = ['亲子游', '情侣出游', '预算 500 元', '拍照出片', '本地美食', '不想爬山']

function pick(c) {
  preference.value = preference.value ? preference.value + '，' + c : c
  ask()
}

const mappedSpots = computed(() => (result.value?.spots || []).map(s => {
  const info = spotMap.value[s.id] || {}
  return { ...info, ...s, img: info.coverImg || '/images/01.jpg' }
}))
const mappedProducts = computed(() => (result.value?.products || []).map(p => {
  const info = productMap.value[p.id] || {}
  return { ...info, ...p, img: info.coverImg || '/images/tesemeishi.jpg' }
}))

function typeLabel(t) {
  return { TICKET: '门票', FOOD: '美食', DRINK: '饮品', PACKAGE: '套餐' }[t] || '商品'
}

async function ask() {
  if (loading.value) return
  loading.value = true
  result.value = null
  try {
    result.value = await request.post('/ai/recommend', {
      preference: preference.value,
      city: cityGroup.value ? city.value : undefined
    })
    if (!result.value.spots?.length && !result.value.products?.length) {
      ElMessage.warning('没有匹配到合适的结果，换个说法试试')
    }
  } catch {
    ElMessage.error('AI 服务暂时不可用，请稍后再试')
  } finally {
    loading.value = false
  }
}

function needLogin() {
  return requireLogin()
}

function goSpot(id) {
  if (needLogin()) return
  router.push('/spots/' + id)
}

function goProduct(p) {
  if (needLogin()) return
  if (p.type === 'FOOD' || p.type === 'DRINK') router.push('/shops/' + p.shopId)
  else router.push('/spots/' + p.spotId)
}

onMounted(async () => {
  try {
    const params = cityGroup.value ? { city: cityGroup.value } : {}
    const [spots, products] = await Promise.all([
      request.get('/spot/hot', { params: { limit: 30, ...params }, silent: true }),
      request.get('/product/hot', { params: { limit: 30, ...params }, silent: true })
    ])
    spotMap.value = Object.fromEntries(spots.map(s => [s.id, s]))
    productMap.value = Object.fromEntries(products.map(p => [p.id, p]))
  } catch { /* 静默 */ }
})
</script>

<style scoped>
.ai-panel {
  margin-top: 34px;
  padding: 24px 26px 26px;
  border-radius: 20px;
  border: 1px solid rgba(120, 220, 255, 0.25);
  background:
    linear-gradient(135deg, rgba(30, 120, 190, 0.20), rgba(120, 70, 210, 0.16)),
    rgba(9, 15, 28, 0.34);
  position: relative;
  overflow: hidden;
}
.ai-panel::after {
  content: '';
  position: absolute;
  inset: -40% -20% auto auto;
  width: 420px; height: 420px;
  background: radial-gradient(circle, rgba(94, 234, 255, 0.18), transparent 65%);
  pointer-events: none;
}
.ai-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.ai-title { font-size: 20px; margin: 0 0 8px; }
.ai-sub { color: var(--txt-sub); font-size: 13px; margin: 0; line-height: 1.7; }
.ai-badge {
  flex-shrink: 0;
  font-size: 11.5px;
  padding: 5px 12px;
  border-radius: 999px;
  color: #052a33;
  background: linear-gradient(120deg, #5eeaff, #7bffd4);
  font-weight: 700;
}
.ai-input-row { display: flex; gap: 10px; margin-top: 18px; }
.ai-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-glass);
  border-radius: 12px;
  padding: 12px 16px;
  color: var(--txt);
  font-size: 14px;
  outline: none;
}
.ai-input:focus { border-color: rgba(94, 234, 255, 0.55); }
.ai-btn { padding: 0 26px; border-radius: 12px; }
.ai-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.chip {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-glass);
  color: var(--txt-sub);
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
}
.chip:hover { color: var(--txt); border-color: rgba(94, 234, 255, 0.5); }
.ai-loading {
  margin-top: 18px; display: flex; align-items: center; gap: 8px;
  color: var(--txt-sub); font-size: 13px;
}
.ai-loading .dot {
  width: 7px; height: 7px; border-radius: 50%; background: var(--cyan);
  animation: bounce 1s infinite ease-in-out;
}
.ai-loading .dot:nth-child(2) { animation-delay: 0.15s; }
.ai-loading .dot:nth-child(3) { animation-delay: 0.3s; margin-right: 6px; }
@keyframes bounce { 0%, 100% { transform: translateY(0); opacity: 0.4; } 50% { transform: translateY(-5px); opacity: 1; } }
.ai-result { margin-top: 20px; }
.reply {
  font-size: 13.5px; line-height: 1.8; color: #cfe6ff;
  background: rgba(94, 234, 255, 0.08);
  border-left: 3px solid var(--cyan);
  padding: 10px 14px; border-radius: 8px; margin: 0 0 16px;
}
.group { margin-top: 12px; }
.group-title { font-size: 15px; margin: 0 0 12px; }
.rec-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; }
.rec-card { display: flex; gap: 12px; padding: 10px; border-radius: 14px; background: rgba(255,255,255,0.05); border: 1px solid var(--border-glass); }
.rec-img { width: 86px; height: 86px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.rec-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.rec-name { font-weight: 600; font-size: 14px; }
.rec-meta { display: flex; gap: 8px; font-size: 11.5px; color: var(--txt-sub); align-items: center; flex-wrap: wrap; }
.rec-meta .star { color: #fbbf24; }
.rec-meta .price { color: var(--cyan); font-weight: 700; }
.reason {
  font-size: 12px; color: var(--txt-sub); line-height: 1.6; margin: 0;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.rec-btn { padding: 5px 12px; font-size: 12px; border-radius: 8px; align-self: flex-start; margin-top: 2px; }
.engine { font-size: 11.5px; color: var(--txt-sub); opacity: 0.75; margin: 16px 0 0; }
@media (max-width: 680px) {
  .ai-head { flex-direction: column; }
  .ai-input-row { flex-direction: column; }
  .ai-btn { padding: 11px 0; }
}
</style>
