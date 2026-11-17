<template>
  <main class="page">
    <h1 class="page-title">美食推荐</h1>
    <p class="page-sub">{{ city }}本地餐饮与奶茶饮品 · 每家店都有到店指引，购买后凭取餐号取餐</p>

    <div class="filter-bar glass">
      <el-select v-model="cityModel" style="width: 130px" @change="load">
        <el-option v-for="c in CITIES" :key="c.key" :label="c.label" :value="c.key" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索店铺、美食、奶茶…" clearable style="width: 260px"
                @keyup.enter="load" @clear="load" />
      <el-radio-group v-model="type" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="FOOD">🍜 美食</el-radio-button>
        <el-radio-button value="DRINK">🥤 奶茶饮品</el-radio-button>
      </el-radio-group>
      <button class="btn-grad" style="padding: 8px 22px" @click="load">搜索</button>
    </div>

    <div class="shop-grid" v-loading="loading">
      <div v-for="s in paged" :key="s.id" class="shop-card glass glass-hover"
           @click="$router.push('/shops/' + s.id)">
        <img :src="s.coverImg" class="cover-img shop-img" loading="lazy" />
        <div class="shop-body">
          <div class="shop-row">
            <span class="shop-name">{{ s.name }}</span>
            <span class="shop-rating">★ {{ s.rating }}</span>
          </div>
          <div class="tag-row">
            <el-tag size="small" :type="s.type === 'FOOD' ? 'danger' : 'success'" effect="dark" round>
              {{ s.type === 'FOOD' ? '美食' : '奶茶饮品' }}
            </el-tag>
            <el-tag size="small" effect="dark" round>{{ s.spotName }}</el-tag>
          </div>
          <div class="shop-addr">🧭 {{ s.address }}</div>
          <div class="shop-desc">{{ s.description }}</div>
        </div>
      </div>
      <el-empty v-if="!loading && filtered.length === 0" description="该城市暂无美食店铺" style="grid-column: 1/-1" />
    </div>

    <div class="pager" v-if="filtered.length > 4">
      <el-pagination background layout="prev, pager, next" :total="filtered.length" :page-size="4"
                     :current-page="page" @current-change="p => (page = p)" />
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../api.js'
import { city, cityGroup, setCity, CITIES } from '../store/city.js'

const route = useRoute()
const shops = ref([])
const type = ref('')
const keyword = ref(route.query.keyword || '')
const loading = ref(false)
const page = ref(1)

const cityModel = computed({ get: () => city.value, set: v => { setCity(v); page.value = 1 } })

const filtered = computed(() => shops.value.filter(s =>
  !keyword.value || s.name.includes(keyword.value) || (s.description || '').includes(keyword.value)
))
const paged = computed(() => filtered.value.slice((page.value - 1) * 4, page.value * 4))

async function load() {
  loading.value = true
  page.value = 1
  try {
    shops.value = await request.get('/shop/list', {
      params: { type: type.value || undefined, city: cityGroup.value || undefined }
    })
  } finally { loading.value = false }
}

watch(() => route.query.keyword, v => { keyword.value = v || '' })
watch(city, load)
onMounted(load)
</script>

<style scoped>
.filter-bar { display: flex; gap: 14px; padding: 14px 16px; flex-wrap: wrap; align-items: center; }
.shop-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18px; margin-top: 22px; min-height: 220px; }
.shop-card { overflow: hidden; cursor: pointer; }
.shop-img { height: 170px; transition: transform 0.4s; }
.shop-card:hover .shop-img { transform: scale(1.06); }
.shop-body { padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 9px; align-items: flex-start; }
.shop-row { width: 100%; display: flex; justify-content: space-between; }
.shop-name { font-weight: 600; font-size: 16px; }
.shop-rating { color: #fbbf24; }
.tag-row { display: flex; gap: 8px; }
.shop-addr { color: var(--txt-sub); font-size: 12px; line-height: 1.6; }
.shop-desc { color: var(--txt-sub); font-size: 12.5px; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pager { display: flex; justify-content: center; margin-top: 26px; }
@media (max-width: 860px) { .shop-grid { grid-template-columns: 1fr; } }
</style>
