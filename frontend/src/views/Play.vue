<template>
  <main class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">玩转{{ city }}</h1>
        <p class="page-sub">{{ cityDesc }} · 共 {{ list.length }} 个玩法，点进去可直接下单</p>
      </div>
      <el-select v-model="cityModel" style="width: 120px">
        <el-option v-for="c in CITIES" :key="c.key" :label="c.label" :value="c.key" />
      </el-select>
    </div>

    <div class="filter-bar glass">
      <el-input v-model="keyword" placeholder="搜索玩法，如 竹筏、夜游、嗦粉…" clearable style="width: 280px" />
      <div class="tag-row">
        <button class="tag" :class="{ on: tag === '' }" @click="tag = ''">全部</button>
        <button v-for="t in tags" :key="t" class="tag" :class="{ on: tag === t }" @click="tag = t">{{ t }}</button>
      </div>
    </div>

    <div class="play-grid">
      <div v-for="(p, i) in paged" :key="i" class="play-card glass glass-hover">
        <div class="play-cover">
          <img :src="p.img" :alt="p.title" loading="lazy" />
          <span class="badge">{{ p.tag }}</span>
        </div>
        <div class="play-body">
          <h3 class="play-title">{{ p.title }}</h3>
          <p class="play-desc">{{ p.desc }}</p>
          <div class="play-meta">
            <span>⏱ {{ p.days }}</span>
            <span class="price">{{ p.price > 0 ? '¥' + p.price + ' 起' : '免费' }}</span>
          </div>
          <div class="play-actions">
            <button v-if="p.productId" class="btn-grad" @click="order(p)">立即预订</button>
            <button v-else-if="p.spotId" class="btn-ghost" @click="$router.push('/spots/' + p.spotId)">查看景点</button>
            <button v-else-if="p.shopId" class="btn-ghost" @click="$router.push('/shops/' + p.shopId)">去这家店</button>
            <button v-else class="btn-ghost" @click="askAi(p)">让 AI 规划</button>
          </div>
        </div>
      </div>
      <el-empty v-if="filtered.length === 0" description="没有匹配的玩法，换个关键词试试" style="grid-column: 1/-1" />
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="filtered.length" :page-size="6"
                     :current-page="page" @current-change="p => (page = p)" />
    </div>

    <div class="tips glass">
      <h3>🧭 行程小贴士</h3>
      <p>· 门票类玩法建议提前 1 天预订，节假日提前 3 天；下单后在「我的订单」里可随时查看取票信息。</p>
      <p>· 美食类玩法下单成功后会生成<strong>取餐号</strong>，到店报号即可取餐，无需排队点单。</p>
      <p>· 不确定怎么安排？回到首页用 <a href="/#ai-zone">AI 智选</a> 输入预算和偏好，让 AI 帮你排一条路线。</p>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PLAYS } from '../data/content.js'
import { city, setCity, CITIES } from '../store/city.js'
import { currentUser, requireLogin } from '../api.js'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const tag = ref('')
const page = ref(1)

const cityModel = computed({
  get: () => city.value,
  set: v => { setCity(v); page.value = 1 }
})
const cityDesc = computed(() => CITIES.find(c => c.key === city.value)?.desc || '')

const list = computed(() => PLAYS.filter(p => p.city === city.value))
const tags = computed(() => [...new Set(list.value.map(p => p.tag))])
const filtered = computed(() => list.value.filter(p =>
  (!tag.value || p.tag === tag.value) &&
  (!keyword.value || p.title.includes(keyword.value) || p.desc.includes(keyword.value) || p.tag.includes(keyword.value))
))
const paged = computed(() => filtered.value.slice((page.value - 1) * 6, page.value * 6))

// 商品 → 下单页映射（门票去景点详情选日期，美食去店铺）
const PRODUCT_LINK = {
  1: '/spots/1', 2: '/spots/2', 3: '/spots/3', 4: '/spots/4', 5: '/spots/5', 6: '/spots/6', 7: '/spots/8',
  8: '/spots/9', 16: '/spots/21', 9: '/spots/10', 10: '/spots/13', 11: '/spots/15', 67: '/shops/2',
  76: '/spots/27', 77: '/spots/29', 78: '/spots/30', 79: '/spots/31', 80: '/spots/32'
}

function order(p) {
  if (requireLogin()) return
  router.push(PRODUCT_LINK[p.productId] || '/spots')
}

function askAi() {
  router.push({ path: '/', hash: '#ai-zone' })
}

watch(() => route.query.city, v => { if (v) setCity(v) }, { immediate: true })
watch([keyword, tag], () => { page.value = 1 })
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.filter-bar { display: flex; gap: 14px; padding: 14px 16px; flex-wrap: wrap; align-items: center; margin-bottom: 22px; }
.tag-row { display: flex; gap: 8px; flex-wrap: wrap; }
.tag {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-glass);
  color: var(--txt-sub);
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
}
.tag.on { background: rgba(94, 234, 255, 0.18); color: var(--cyan); border-color: rgba(94, 234, 255, 0.5); }
.play-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.play-card { overflow: hidden; }
.play-cover { position: relative; height: 180px; overflow: hidden; }
.play-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.45s; }
.play-card:hover .play-cover img { transform: scale(1.07); }
.badge {
  position: absolute; left: 12px; top: 12px;
  background: rgba(4, 12, 24, 0.72);
  border: 1px solid rgba(94, 234, 255, 0.45);
  color: var(--cyan); font-size: 11.5px;
  padding: 3px 10px; border-radius: 999px;
}
.play-body { padding: 14px 16px 16px; }
.play-title { font-size: 16px; margin: 0 0 8px; }
.play-desc { color: var(--txt-sub); font-size: 12.5px; line-height: 1.7; margin: 0 0 12px; min-height: 42px; }
.play-meta { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--txt-sub); margin-bottom: 12px; }
.play-meta .price { color: var(--cyan); font-weight: 700; font-size: 16px; }
.play-actions { display: flex; gap: 10px; }
.play-actions button { padding: 8px 18px; font-size: 13px; }
.tips { margin-top: 34px; padding: 20px 24px; }
.pager { display: flex; justify-content: center; margin-top: 28px; }
.tips h3 { margin: 0 0 12px; font-size: 15px; }
.tips p { color: var(--txt-sub); font-size: 13px; line-height: 1.9; margin: 0 0 6px; }
.tips a { color: var(--cyan); }
</style>
