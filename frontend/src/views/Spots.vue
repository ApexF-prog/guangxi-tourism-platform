<template>
  <main class="page">
    <h1 class="page-title">{{ city }}景点门票</h1>
    <p class="page-sub">共 {{ total }} 个景点 · 门票与团购套餐可在线预订，支付宝沙箱支付</p>

    <div class="filter-bar glass">
      <el-select v-model="cityModel" style="width: 130px" @change="load(1)">
        <el-option v-for="c in CITIES" :key="c.key" :label="c.label" :value="c.key" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索景点名称、标签、地址…" clearable style="width: 280px"
                @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="level" placeholder="全部等级" clearable style="width: 120px" @change="load(1)">
        <el-option label="5A" value="5A" /><el-option label="4A" value="4A" />
      </el-select>
      <button class="btn-grad" style="padding: 8px 22px" @click="load(1)">搜索</button>
    </div>

    <div v-loading="loading" class="spot-grid">
      <div v-for="s in list" :key="s.id" class="spot-card glass glass-hover"
           @click="$router.push('/spots/' + s.id)">
        <img :src="s.coverImg" class="cover-img spot-img" loading="lazy" />
        <div class="spot-body">
          <div class="spot-row">
            <span class="spot-name">{{ s.name }}</span>
            <span class="spot-rating">★ {{ s.rating }}</span>
          </div>
          <div class="spot-desc">{{ s.description }}</div>
          <div class="spot-meta">
            <el-tag size="small" effect="dark" round>{{ s.city }}</el-tag>
            <el-tag v-if="s.level !== '无'" size="small" type="warning" effect="dark" round>{{ s.level }}</el-tag>
            <span class="spot-price">{{ s.price > 0 ? '¥' + s.price + ' 起' : '免费开放' }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="该城市暂无相关景点" style="grid-column: 1/-1" />
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="8"
                     :current-page="page" @current-change="load" />
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../api.js'
import { city, cityGroup, setCity, CITIES } from '../store/city.js'

const route = useRoute()
const keyword = ref(route.query.keyword || '')
const level = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const cityModel = computed({ get: () => city.value, set: v => setCity(v) })

async function load(p = 1) {
  loading.value = true
  page.value = p
  try {
    const data = await request.get('/spot/page', {
      params: {
        page: p, size: 8,
        keyword: keyword.value || undefined,
        city: cityGroup.value || undefined,
        level: level.value || undefined
      }
    })
    list.value = data.records
    total.value = Number(data.total)
  } finally { loading.value = false }
}

watch(() => route.query.keyword, v => { keyword.value = v || ''; load(1) })
watch(city, () => load(1))

onMounted(() => load(1))
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; padding: 14px 16px; flex-wrap: wrap; align-items: center; }
.spot-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-top: 22px; min-height: 300px; }
.spot-card { overflow: hidden; cursor: pointer; }
.spot-img { height: 190px; transition: transform 0.4s; }
.spot-card:hover .spot-img { transform: scale(1.06); }
.spot-body { padding: 14px 16px 16px; }
.spot-row { display: flex; justify-content: space-between; align-items: center; }
.spot-name { font-weight: 600; font-size: 16px; }
.spot-rating { color: #fbbf24; }
.spot-desc { color: var(--txt-sub); font-size: 13px; margin-top: 8px; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.spot-meta { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.spot-price { margin-left: auto; color: var(--cyan); font-weight: 700; }
.pager { display: flex; justify-content: center; margin-top: 30px; }
</style>
