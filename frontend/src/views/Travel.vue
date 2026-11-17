<template>
  <main class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">旅游资讯</h1>
        <p class="page-sub">{{ city }}最新攻略 · 路线、购票、时令提醒一站掌握</p>
      </div>
      <el-select v-model="cityModel" style="width: 120px">
        <el-option v-for="c in CITIES" :key="c.key" :label="c.label" :value="c.key" />
      </el-select>
    </div>

    <!-- 头条 -->
    <div v-if="featured" class="feature glass" @click="open(featured)">
      <img :src="featured.img" class="feature-img" />
      <div class="feature-body">
        <el-tag effect="dark" type="danger" round size="small">头条</el-tag>
        <h2>{{ featured.title }}</h2>
        <p>{{ featured.desc }}</p>
        <div class="meta">{{ featured.date }} · 阅读 {{ featured.views }} · {{ featured.city }}</div>
      </div>
    </div>

    <div class="filter-bar glass">
      <button class="tag" :class="{ on: tag === '' }" @click="tag = ''">全部</button>
      <button v-for="t in tags" :key="t" class="tag" :class="{ on: tag === t }" @click="tag = t">{{ t }}</button>
      <el-input v-model="keyword" placeholder="搜索资讯标题…" clearable style="width: 220px; margin-left: auto" />
    </div>

    <div class="news-list">
      <div v-for="n in paged" :key="n.title" class="news-item glass glass-hover" @click="open(n)">
        <img :src="n.img" class="news-img" loading="lazy" />
        <div class="news-body">
          <el-tag size="small" effect="dark" type="success" round>{{ n.tag }}</el-tag>
          <h3>{{ n.title }}</h3>
          <p>{{ n.desc }}</p>
          <div class="meta">{{ n.date }} · 阅读 {{ n.views }}</div>
        </div>
      </div>
      <el-empty v-if="filtered.length === 0" description="暂无相关资讯" />
    </div>

    <div class="pager" v-if="filtered.length > 4">
      <el-pagination background layout="prev, pager, next" :total="filtered.length" :page-size="4"
                     :current-page="page" @current-change="p => (page = p)" />
    </div>

    <el-dialog v-model="visible" :title="active?.title" width="640px">
      <img v-if="active" :src="active.img" style="width: 100%; border-radius: 12px; margin-bottom: 16px" />
      <p style="color: var(--txt-sub); line-height: 2">{{ active?.desc }}</p>
      <p style="color: var(--txt-sub); line-height: 2">
        本页面为课程实践项目内容，攻略信息根据公开旅游资料整理，实际出行请以景区官方公告为准。
        门票与美食可直接在站内下单，支付使用支付宝沙箱环境演示。
      </p>
      <template #footer>
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="goBook">去订门票</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEWS } from '../data/content.js'
import { city, setCity, CITIES } from '../store/city.js'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const tag = ref('')
const page = ref(1)
const visible = ref(false)
const active = ref(null)

const cityModel = computed({ get: () => city.value, set: v => { setCity(v); page.value = 1 } })
const list = computed(() => NEWS.filter(n => n.city === city.value))
const featured = computed(() => list.value[0])
const tags = computed(() => [...new Set(list.value.map(n => n.tag))])
const filtered = computed(() => list.value.slice(1).filter(n =>
  (!tag.value || n.tag === tag.value) && (!keyword.value || n.title.includes(keyword.value))
))
const paged = computed(() => filtered.value.slice((page.value - 1) * 4, page.value * 4))

function open(n) { active.value = n; visible.value = true }
function goBook() { visible.value = false; router.push('/spots') }

watch(() => route.query.city, v => { if (v) setCity(v) }, { immediate: true })
watch([keyword, tag], () => { page.value = 1 })
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.feature { display: grid; grid-template-columns: 1.1fr 1fr; overflow: hidden; cursor: pointer; margin-bottom: 22px; }
.feature-img { width: 100%; height: 100%; min-height: 260px; object-fit: cover; }
.feature-body { padding: 26px 28px; display: flex; flex-direction: column; gap: 10px; justify-content: center; }
.feature-body h2 { margin: 0; font-size: 22px; line-height: 1.5; }
.feature-body p { color: var(--txt-sub); line-height: 1.9; margin: 0; font-size: 13.5px; }
.meta { color: var(--txt-sub); font-size: 12px; opacity: 0.85; }
.filter-bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; padding: 12px 16px; margin-bottom: 20px; }
.tag {
  background: rgba(255, 255, 255, 0.05); border: 1px solid var(--border-glass);
  color: var(--txt-sub); font-size: 12.5px; padding: 6px 14px; border-radius: 999px; cursor: pointer;
}
.tag.on { background: rgba(94, 234, 255, 0.18); color: var(--cyan); border-color: rgba(94, 234, 255, 0.5); }
.news-list { display: flex; flex-direction: column; gap: 14px; }
.pager { display: flex; justify-content: center; margin-top: 26px; }
.news-item { display: flex; gap: 18px; padding: 14px; cursor: pointer; }
.news-img { width: 200px; height: 130px; border-radius: 12px; object-fit: cover; flex-shrink: 0; }
.news-body { display: flex; flex-direction: column; gap: 8px; justify-content: center; }
.news-body h3 { margin: 0; font-size: 16px; line-height: 1.5; }
.news-body p { margin: 0; color: var(--txt-sub); font-size: 13px; line-height: 1.8;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
@media (max-width: 760px) {
  .feature { grid-template-columns: 1fr; }
  .news-item { flex-direction: column; }
  .news-img { width: 100%; height: 180px; }
}
</style>
