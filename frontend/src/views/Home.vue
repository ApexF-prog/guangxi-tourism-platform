<template>
  <section class="hero">
    <div class="hero-videos" :style="{ backgroundImage: 'url(' + currentPoster + ')' }">
      <video
        v-for="v in allVideos"
        :key="v.src"
        :ref="el => setVideo(el, v)"
        class="hero-video"
        :class="{ active: isActive(v) }"
        :src="v.src"
        :poster="v.poster"
        :preload="v.city === city ? 'auto' : 'metadata'"
        muted
        loop
        playsinline
        @canplay="markReady(v)"
      ></video>
      <span v-if="videoLoading" class="video-loading glass">高清视频加载中…</span>
    </div>
    <div class="hero-mask"></div>

    <div class="hero-inner">
      <div class="hero-text">
        <p class="hero-kicker">{{ city }} · {{ cityDesc }}</p>
        <h1 class="hero-title">{{ videos[current].title }}</h1>
        <p class="hero-sub">{{ videos[current].desc }}</p>
        <div class="hero-actions">
          <button class="btn-grad hero-btn" @click="$router.push({ path: '/play', query: { city } })">玩转{{ city }}</button>
          <button class="btn-ghost hero-btn" @click="$router.push('/shops')">美食推荐</button>
          <button class="btn-ghost hero-btn" @click="scrollToAi">AI 智选</button>
        </div>
      </div>

      <!-- 竖向缓慢下滚的城市介绍 -->
      <div class="roll-panel glass">
        <div class="roll-head">
          <span class="roll-dot"></span> {{ city }} · 城市速览
        </div>
        <div class="roll-window">
          <div class="roll-track">
            <p v-for="(t, i) in rollLines" :key="'a' + i" class="roll-line">{{ t }}</p>
            <p v-for="(t, i) in rollLines" :key="'b' + i" class="roll-line">{{ t }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="hero-controls">
      <button class="ctrl" @click="prevVideo">‹</button>
      <div class="dots">
        <span v-for="(v, i) in videos" :key="i" class="dot" :class="{ on: i === current }" @click="goVideo(i)"></span>
      </div>
      <button class="ctrl" @click="nextVideo">›</button>
    </div>

    <div class="progress"><span :key="current" class="bar"></span></div>
  </section>

  <main class="page">
    <!-- 快捷入口 -->
    <div class="entrance-grid">
      <div class="entrance glass glass-hover" @click="$router.push('/spots')">
        <span class="ico">🎫</span><b>景点门票</b><small>直营出票 · 即买即用</small>
      </div>
      <div class="entrance glass glass-hover" @click="$router.push('/shops')">
        <span class="ico">🍜</span><b>美食推荐</b><small>到店指引 · 取餐号</small>
      </div>
      <div class="entrance glass glass-hover" @click="$router.push({ path: '/play', query: { city } })">
        <span class="ico">🧭</span><b>玩法路线</b><small>{{ city }}必玩清单</small>
      </div>
      <div class="entrance glass glass-hover" @click="$router.push('/service')">
        <span class="ico">🎧</span><b>服务中心</b><small>退改 · 发票 · 客服</small>
      </div>
    </div>

    <!-- 智能发现：行为画像推荐 + 实时订单播报 + 热搜榜 -->
    <div class="discover">
      <div class="discover-main">
        <div class="section-head">
          <h2 class="page-title">✨ {{ recTitle }}</h2>
          <button class="more btn-ghost" @click="loadRecommend">换一批 →</button>
        </div>
        <div class="rec-grid">
          <div v-for="(r, i) in recItems" :key="r.itemType + '-' + r.itemId"
               class="rec-card glass glass-hover" @click="goRec(r)">
            <img :src="r.image || DEFAULT_AVATAR" class="rec-img" loading="lazy" />
            <span class="rec-rank">TOP {{ i + 1 }}</span>
            <button class="fav-btn" :class="{ on: favMap[r.itemType + '-' + r.itemId] }"
                    :title="favMap[r.itemType + '-' + r.itemId] ? '取消收藏' : '加入想去'"
                    @click.stop="toggleFav(r)">{{ favMap[r.itemType + '-' + r.itemId] ? '★' : '☆' }}</button>
            <div class="rec-body">
              <b>{{ r.title }}</b>
              <small class="rec-reason">{{ r.reason }}</small>
              <div class="rec-foot"><span>{{ r.city || '广西' }}</span><em>{{ r.price > 0 ? '¥' + r.price : '免费开放' }}</em></div>
            </div>
          </div>
        </div>
      </div>

      <aside class="discover-side glass">
        <div class="side-title"><span class="pulse-dot"></span> 实时订单播报</div>
        <div class="feed-list">
          <div v-for="(f, i) in liveFeed" :key="i" class="feed-item">
            <b>{{ f.buyer }}</b> 预订了 <span>{{ f.productName }}</span>
            <small>¥{{ f.amount }}</small>
          </div>
          <el-empty v-if="!liveFeed.length" description="等待首笔订单" :image-size="50" />
        </div>
        <div class="side-title" style="margin-top: 18px">🔥 广西热搜榜</div>
        <div class="hot-list">
          <span v-for="(h, i) in hotWords" :key="h.word" class="hot-chip" @click="quickSearch(h.word)">
            <i :class="{ top: i < 3 }">{{ i + 1 }}</i>{{ h.word }}
          </span>
        </div>
      </aside>
    </div>

    <!-- AI 智选 -->
    <div id="ai-zone"><AiPicker /></div>

    <!-- 热门景点 -->
    <div class="section-head">
      <h2 class="page-title">🔥 {{ city }}热门景点</h2>
      <router-link to="/spots" class="more">查看全部 →</router-link>
    </div>
    <div class="spot-grid">
      <div v-for="s in spots" :key="s.id" class="spot-card glass glass-hover" @click="$router.push('/spots/' + s.id)">
        <img :src="s.coverImg" class="cover-img spot-img" loading="lazy" />
        <div class="spot-body">
          <div class="spot-row">
            <span class="spot-name">{{ s.name }}</span>
            <span class="spot-rating">★ {{ s.rating }}</span>
          </div>
          <div class="spot-meta">
            <el-tag size="small" effect="dark" round>{{ s.city }}</el-tag>
            <el-tag v-if="s.level !== '无'" size="small" type="warning" effect="dark" round>{{ s.level }}</el-tag>
            <span class="spot-price">{{ s.price > 0 ? '¥' + s.price + ' 起' : '免费开放' }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!spots.length" description="该城市暂无景点数据" style="grid-column: 1/-1" />
    </div>

    <!-- 美食推荐 -->
    <div class="section-head" style="margin-top: 42px">
      <h2 class="page-title">🍜 {{ city }}美食推荐</h2>
      <router-link to="/shops" class="more">全部店铺 →</router-link>
    </div>
    <div class="prod-grid">
      <div v-for="p in products" :key="p.id" class="prod-card glass glass-hover" @click="goProduct(p)">
        <img :src="p.coverImg" class="cover-img prod-img" loading="lazy" />
        <div class="prod-body">
          <div class="prod-name">{{ p.name }}</div>
          <div class="prod-desc">{{ p.description }}</div>
          <div class="prod-row">
            <div class="prod-price">
              <span class="now" :class="{ 'price-free': p.price <= 0 }">{{ p.price > 0 ? '¥' + p.price : '免费预约' }}</span>
              <span v-if="p.originalPrice && p.price > 0" class="origin">¥{{ p.originalPrice }}</span>
            </div>
            <span class="sales">已售 {{ p.sales }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!products.length" description="该城市暂无美食数据" style="grid-column: 1/-1" />
    </div>

    <!-- 旅游攻略 -->
    <div class="section-head" style="margin-top: 42px">
      <h2 class="page-title">📖 {{ city }}旅游攻略</h2>
      <router-link to="/travel" class="more">更多资讯 →</router-link>
    </div>
    <div class="guide-grid">
      <div v-for="n in news" :key="n.title" class="guide-card glass glass-hover" @click="$router.push({ path: '/travel', query: { city } })">
        <img :src="n.img" class="guide-img" loading="lazy" />
        <div class="guide-body">
          <el-tag size="small" effect="dark" type="success" round>{{ n.tag }}</el-tag>
          <h3 class="guide-title">{{ n.title }}</h3>
          <p class="guide-desc">{{ n.desc }}</p>
          <div class="guide-meta">{{ n.date }} · 阅读 {{ n.views }}</div>
        </div>
      </div>
    </div>

    <!-- 视频欣赏 -->
    <div class="section-head" style="margin-top: 42px">
      <h2 class="page-title">🎬 {{ city }}风光欣赏</h2>
      <router-link to="/play" class="more">更多玩法 →</router-link>
    </div>
    <div class="media-grid">
      <div v-for="m in mediaList" :key="m.src" class="media-item glass">
        <video v-if="m.src.endsWith('.mp4')" :src="m.src" controls muted playsinline preload="metadata" class="media-content"></video>
        <img v-else :src="m.src" :alt="m.title" class="media-content" />
        <p class="media-desc">{{ m.title }}</p>
      </div>
    </div>

    <!-- 数据大屏入口 -->
    <div class="screen-banner glass" @click="$router.push('/screen')">
      <div class="sb-left">
        <span class="sb-badge">DATA V</span>
        <h2 class="sb-title grad-text">广西旅游可视化大屏</h2>
        <p class="sb-desc">全区 14 市 186 个在监测景区 · 实时客流量、门票价格、月度趋势、客源结构 —— 大数据视角，一屏看懂广西旅游</p>
      </div>
      <button class="btn-grad sb-btn">进入大屏 →</button>
    </div>

    <!-- 数据条 -->
    <div class="stats-bar glass">
      <div class="stat"><b class="grad-text">{{ spots.length }}</b><span>{{ city }}精选景点</span></div>
      <div class="stat"><b class="grad-text">{{ products.length }}</b><span>美食好物</span></div>
      <div class="stat"><b class="grad-text">8</b><span>大类玩法</span></div>
      <div class="stat"><b class="grad-text">3s</b><span>支付到账</span></div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { currentUser, requireLogin, DEFAULT_AVATAR } from '../api.js'
import { city, cityGroup, CITIES } from '../store/city.js'
import { NEWS } from '../data/content.js'
import AiPicker from '../components/AiPicker.vue'

const router = useRouter()

// ===== 智能发现：行为画像推荐 / 实时订单播报 / 热搜榜 =====
const recTitle = ref('大家都在看 · 广西热门榜')
const recItems = ref([])
const liveFeed = ref([])
const hotWords = ref([])
const favMap = ref({})
let feedTimer = null

async function loadRecommend() {
  try {
    const d = await request.get('/discovery/recommend', { params: { limit: 8 }, silent: true }) || {}
    recTitle.value = d.title || '大家都在看 · 广西热门榜'
    recItems.value = d.items || []
    if (currentUser()) {
      const map = {}
      for (const r of recItems.value) {
        try {
          const v = await request.get('/user/favorites/check',
            { params: { itemType: r.itemType, itemId: r.itemId }, silent: true })
          map[r.itemType + '-' + r.itemId] = !!v
        } catch { /* 单项失败不影响展示 */ }
      }
      favMap.value = map
    } else favMap.value = {}
  } catch { /* 推荐失败不阻塞首页 */ }
}
async function toggleFav(r) {
  if (requireLogin()) return
  const key = r.itemType + '-' + r.itemId
  try {
    const d = await request.post('/user/favorites/toggle', { itemType: r.itemType, itemId: r.itemId })
    favMap.value = { ...favMap.value, [key]: !!d.favorited }
    ElMessage.success(d.favorited ? '已加入想去清单' : '已取消收藏')
  } catch { /* 拦截器已提示 */ }
}
function goRec(r) {
  if (r.itemType === 'SPOT') return router.push('/spots/' + r.itemId)
  if (r.itemType === 'TOUR') return router.push('/tours')
  router.push('/spots')
}
function quickSearch(w) { router.push({ path: '/spots', query: { keyword: w } }) }
async function loadFeed() {
  try { liveFeed.value = await request.get('/discovery/live-feed', { silent: true }) || [] } catch { /* 静默 */ }
}
async function loadHot() {
  try { hotWords.value = await request.get('/discovery/hot-search', { silent: true }) || [] } catch { /* 静默 */ }
}

/** 每个城市独立的首页轮播：柳州用用户提供的 4 个视频，其余城市视频来自网络公开素材 */
const HERO_VIDEOS = {
  桂林: [
    { src: '/video/guilin_hd1.mp4', title: '漓江 · 4K 山水航拍', desc: '喀斯特峰林间的漓江如青罗带蜿蜒，20 元人民币背面的山水画卷，无人机视角一览无余' },
    { src: '/video/guilin_hd2.mp4', title: '桂林 · 不止山水', desc: '漓江烟雨、两江四湖、龙脊梯田——官方文旅视角，看桂林山水的另一面' },
    { src: '/video/guilin_hd3.mp4', title: '桂林米粉 · 一碗卤香', desc: '本地人排队的 15 年老店，先干拌后加汤，锅烧脆皮配卤水，桂林人的一天从这碗粉开始' }
  ],
  柳州: [
    { src: '/video/01.mp4', title: '龙城柳州 · 一江环抱', desc: '柳江在城中绕出一个半岛，青山环抱工业龙城，山水与烟火气在这里共存' },
    { src: '/video/02.mp4', title: '百里柳江 · 夜色如画', desc: '江面音乐喷泉与两岸灯火，乘船夜游，看一座工业城市的温柔一面' },
    { src: '/video/03.mp4', title: '喀斯特秘境 · 溶洞奇观', desc: '亿万年滴水成石，钟乳石长廊在灯光下像另一片星空' },
    { src: '/video/04.mp4', title: '民族风情 · 侗寨鼓楼', desc: '三江程阳风雨桥、百家宴、侗族大歌，把柳州的少数民族故事听一遍' }
  ],
  南宁: [
    { src: '/video/nanning_hd1.mp4', title: '绿城南宁 · 地标航拍', desc: '航拍延时俯瞰南宁：会展中心、三街两巷、邕江两岸，半城绿树半城楼' },
    { src: '/video/nanning_hd2.mp4', title: '南宁老城 · 市井烟火', desc: '从朝阳商圈的老街巷到邕江夜色，看南宁的过去、现在与未来' },
    { src: '/video/nanning_hd3.mp4', title: '南宁老友粉 · 酸辣鲜香', desc: '酸笋豆豉爆香，一碗老友粉是南宁人刻在骨子里的早餐答案' }
  ],
  北海: [
    { src: '/video/beihai_hd2.mp4', title: '涠洲岛 · 天空之岛', desc: '4K 航拍中国最年轻的火山岛，碧海蓝天环抱五彩滩与火山口，风景不输东南亚' },
    { src: '/video/beihai_hd1.mp4', title: '北海银滩 · 无人机视角', desc: '无人机掠过"天下第一滩"，沙细如粉、滩长平缓，滑翔机与海风共舞' },
    { src: '/video/beihai_hd3.mp4', title: '北海寻味 · 海鲜盛宴', desc: '侨港风情街的虾饼糖水、外沙岛的海鲜大餐，北海的烟火气在餐桌上' }
  ]
}
// 播放统一走压缩版（原片 15~60MB，切换城市会卡住；压缩后每支 1~5MB）
function slimSrc(src) { return '/video/slim/' + src.split('/').pop() }
function posterSrc(src) { return '/video/poster/' + src.split('/').pop().replace('.mp4', '.jpg') }

const videos = computed(() => (HERO_VIDEOS[city.value] || []).map(v => ({
  ...v, src: slimSrc(v.src), poster: posterSrc(v.src)
})))

// 四个城市的视频常驻 DOM：切换城市不再销毁/重建 video 元素，避免重新下载大文件
const allVideos = computed(() => Object.entries(HERO_VIDEOS).flatMap(([c, arr]) =>
  arr.map((v, i) => ({ ...v, src: slimSrc(v.src), poster: posterSrc(v.src), city: c, index: i }))))

const current = ref(0)
const videoEls = ref(new Map())
const readyMap = ref({})
let timer = null

const currentPoster = computed(() => videos.value[current.value]?.poster || '/images/spot-guilin.jpg')
const videoLoading = computed(() => {
  const s = videos.value[current.value]?.src
  return !!s && !readyMap.value[s]
})

const cityDesc = computed(() => CITIES.find(c => c.key === city.value)?.desc || '')

const rollLines = computed(() => ({
  桂林: [
    '“桂林山水甲天下”，漓江从兴坪到杨堤，是 20 元人民币背面的那片山水。',
    '两江四湖环城水系，夜游时日月双塔与玻璃桥的灯光会同时亮起。',
    '龙脊梯田在龙胜，4-6 月灌水如镜，9-10 月金黄稻浪，是摄影师的年度功课。',
    '阳朔西街适合住一晚：白天遇龙河竹筏，晚上啤酒鱼和手作小店。',
    '桂林米粉要吃卤菜粉，先干拌后加汤，才算本地吃法。'
  ],
  柳州: [
    '柳州别称龙城，柳江在城中绕出一个半岛，夜景被称为“百里柳江，百里画廊”。',
    '螺蛳粉的发源地就在柳州，酸笋、螺蛳汤、腐竹、鸭脚是标准配置。',
    '程阳八寨在三江，程阳永济桥是世界四大历史名桥之一，不用一根铁钉。',
    '马鞍山公园登顶 15 分钟，是拍柳州全景最省力的机位。',
    '三江油茶是侗族待客的第一道礼，一碗下去从喉咙暖到胃。'
  ],
  南宁: [
    '南宁被称为“绿城”，半城绿树半城楼，冬天也不冷。',
    '青秀山是城市绿肺，苏铁园和兰园四季都有花，山顶能俯瞰邕江。',
    '广西民族博物馆收藏铜鼓与壮锦，一次看懂十二个世居民族。',
    '中山路夜市是南宁夜宵主场：老友粉、卷筒粉、酸野、烤生蚝。',
    '老友粉的酸笋与豆豉要爆香，是南宁人早餐的固定答案。'
  ],
  北海: [
    '北海银滩沙细如粉，退潮时能捡到贝壳和小螃蟹。',
    '涠洲岛是中国最年轻的火山岛，从北海国际客运港乘船约 70 分钟。',
    '岛上租一台电动车，火山口、滴水丹屏、五彩滩一天能环完。',
    '北海老街的骑楼有百年历史，虾饼和糖水铺值得慢慢逛。',
    '德天跨国瀑布在崇左，从北海出发可安排一日游，对面就是越南。'
  ]
}[city.value] || []))

const spots = ref([])
const products = ref([])
const news = computed(() => NEWS.filter(n => n.city === city.value).slice(0, 3))

/** 每个城市的风光欣赏（视频 + 图片，凑满 6 格 × 2 整排） */
const MEDIA_EXTRA = {
  桂林: [
    { src: '/images/06.jpg', title: '龙脊梯田 · 千层稻浪' },
    { src: '/images/spot-yulonghe.jpg', title: '遇龙河 · 田园竹筏' },
    { src: '/images/spot-xijie.jpg', title: '阳朔西街 · 夜色' }
  ],
  柳州: [
    { src: '/images/maanshan.jpg', title: '马鞍山 · 柳州全景' },
    { src: '/images/sanjiangchengyangbazhai.jpg', title: '程阳八寨 · 风雨桥' },
    { src: '/images/liuzhouluosifen.jpg', title: '柳州螺蛳粉 · 烟火气' },
    { src: '/images/liujiangyejing.jpg', title: '百里柳江 · 夜景画廊' },
    { src: '/images/sanjiangyoucha.jpg', title: '三江油茶 · 侗家味道' }
  ],
  南宁: [
    { src: '/images/spot-qingxiushan.jpg', title: '青秀山 · 城市绿肺' },
    { src: '/images/spot-minzubowuguan.jpg', title: '民族博物馆 · 铜鼓' },
    { src: '/images/tesemeishi.jpg', title: '中山路夜市 · 老友粉' }
  ],
  北海: [
    { src: '/images/spot-weizhoudao.jpg', title: '涠洲岛 · 火山海岸' },
    { src: '/images/spot-beihailaojie.jpg', title: '北海老街 · 骑楼' },
    { src: '/images/spot-detian.jpg', title: '德天瀑布 · 跨国奇观' }
  ]
}
const mediaList = computed(() => [
  ...videos.value.map(v => ({ src: v.src, title: v.title })),
  ...(MEDIA_EXTRA[city.value] || [])
])

function markReady(v) {
  if (!readyMap.value[v.src]) readyMap.value = { ...readyMap.value, [v.src]: true }
}

function isActive(v) {
  return v.city === city.value && v.index === current.value
}

function setVideo(el, v) {
  if (el) videoEls.value.set(v.src, el)
}

function playCurrent() {
  const activeSrc = videos.value[current.value]?.src
  videoEls.value.forEach((el, src) => {
    if (!el) return
    if (src === activeSrc) {
      try { el.currentTime = 0 } catch { /* 元数据未就绪，稍后自动播放 */ }
      const p = el.play?.()
      if (p?.catch) p.catch(() => {})
    } else {
      el.pause?.()
    }
  })
}

function goVideo(i) {
  current.value = i
  nextTick(playCurrent)
  restartTimer()
}
function nextVideo() { goVideo((current.value + 1) % videos.value.length) }
function prevVideo() { goVideo((current.value - 1 + videos.value.length) % videos.value.length) }

function restartTimer() {
  clearInterval(timer)
  timer = setInterval(nextVideo, 9000)
}

function scrollToAi() {
  document.getElementById('ai-zone')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function goProduct(p) {
  if (p.type === 'FOOD' || p.type === 'DRINK') router.push('/shops/' + p.shopId)
  else router.push('/spots/' + p.spotId)
}

async function loadCity() {
  const params = cityGroup.value ? { city: cityGroup.value } : {}
  const [s, p] = await Promise.all([
    request.get('/spot/hot', { params: { limit: 8, ...params } }),
    request.get('/product/hot', { params: { limit: 8, ...params } })
  ])
  spots.value = (s || []).slice(0, 8)
  products.value = (p || []).slice(0, 8)
}

watch(city, loadCity)

// 切换城市时只重置轮播下标，DOM 常驻不清空，已缓冲的视频直接接着播
watch(videos, () => {
  current.value = 0
  nextTick(() => {
    playCurrent()
    restartTimer()
  })
})

onMounted(() => {
  loadCity()
  nextTick(playCurrent)
  restartTimer()
  loadRecommend()
  loadFeed()
  loadHot()
  feedTimer = setInterval(loadFeed, 20000)
})

onBeforeUnmount(() => { clearInterval(timer); clearInterval(feedTimer) })
</script>

<style scoped>
.hero {
  position: relative;
  height: 74vh;
  min-height: 520px;
  margin: 16px auto 0;
  max-width: 1240px;
  width: calc(100% - 40px);
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid var(--border-glass);
}
.hero-videos { position: absolute; inset: 0; background-size: cover; background-position: center; }
.video-loading {
  position: absolute; right: 18px; top: 18px; z-index: 3;
  padding: 6px 14px; border-radius: 999px; font-size: 12px; color: var(--txt-sub);
}
.hero-video {
  position: absolute; inset: 0; width: 100%; height: 100%;
  object-fit: cover; opacity: 0; transition: opacity 1.2s ease;
}
.hero-video.active { opacity: 1; }
.hero-mask {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(4, 8, 16, 0.78) 0%, rgba(4, 8, 16, 0.38) 46%, rgba(4, 8, 16, 0.16) 100%),
              linear-gradient(180deg, rgba(4, 8, 16, 0.1), rgba(4, 8, 16, 0.72));
}
.hero-inner {
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 40px 46px 70px;
}
.hero-text { max-width: 560px; display: flex; flex-direction: column; gap: 14px; }
.hero-kicker { color: var(--cyan); font-size: 13px; letter-spacing: 3px; margin: 0; }
.hero-title { font-size: 44px; margin: 0; letter-spacing: 1px; line-height: 1.2; }
.hero-sub { color: #c5d5ea; line-height: 1.9; margin: 0; font-size: 14.5px; }
.hero-actions { display: flex; gap: 12px; margin-top: 8px; flex-wrap: wrap; }
.hero-btn { padding: 11px 26px; font-size: 14px; border-radius: 12px; }

.roll-panel {
  width: 320px;
  flex-shrink: 0;
  border-radius: 16px;
  padding: 16px 18px;
  align-self: stretch;
  max-height: 340px;
  display: flex;
  flex-direction: column;
}
.roll-head {
  font-size: 12.5px; color: var(--cyan); letter-spacing: 1px;
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
  padding-bottom: 10px; border-bottom: 1px dashed rgba(255, 255, 255, 0.12);
}
.roll-dot {
  width: 7px; height: 7px; border-radius: 50%; background: var(--cyan);
  box-shadow: 0 0 10px var(--cyan); animation: pulse 1.8s infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
.roll-window { flex: 1; overflow: hidden; position: relative; }
.roll-window::before, .roll-window::after {
  content: ''; position: absolute; left: 0; right: 0; height: 26px; z-index: 2; pointer-events: none;
}
.roll-window::before { top: 0; background: linear-gradient(180deg, rgba(9, 15, 28, 0.9), transparent); }
.roll-window::after { bottom: 0; background: linear-gradient(0deg, rgba(9, 15, 28, 0.9), transparent); }
.roll-track { animation: roll-down 34s linear infinite; }
.roll-window:hover .roll-track { animation-play-state: paused; }
@keyframes roll-down {
  from { transform: translateY(-50%); }
  to { transform: translateY(0); }
}
.roll-line {
  font-size: 12.5px; line-height: 1.85; color: #b9cbe2;
  margin: 0 0 10px; padding-left: 12px; position: relative;
}
.roll-line::before {
  content: ''; position: absolute; left: 0; top: 8px;
  width: 4px; height: 4px; border-radius: 50%; background: rgba(94, 234, 255, 0.7);
}
.hero-controls {
  position: absolute; left: 46px; bottom: 34px;
  display: flex; align-items: center; gap: 14px; z-index: 3;
}
.ctrl {
  width: 40px; height: 40px; border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(6, 16, 28, 0.55);
  backdrop-filter: blur(4px);
  color: #fff; font-size: 24px; cursor: pointer; line-height: 1;
  transition: background 0.25s, transform 0.25s;
}
.ctrl:hover { background: rgba(94, 234, 255, 0.35); transform: scale(1.08); }
.dots { display: flex; gap: 8px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%; cursor: pointer;
  background: rgba(255, 255, 255, 0.28); transition: all 0.3s;
}
.dot.on { background: var(--cyan); width: 22px; border-radius: 999px; }
.progress { position: absolute; left: 0; right: 0; bottom: 0; height: 3px; background: rgba(255, 255, 255, 0.1); }
.progress .bar {
  display: block; height: 100%; width: 100%;
  background: linear-gradient(90deg, #5eeaff, #7bffd4);
  transform-origin: left; animation: grow 9s linear forwards;
}
@keyframes grow { from { transform: scaleX(0); } to { transform: scaleX(1); } }

.entrance-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 26px; }
.entrance {
  padding: 18px 20px; border-radius: 16px; cursor: pointer;
  display: flex; flex-direction: column; gap: 6px;
}
.entrance .ico { font-size: 22px; }
.entrance b { font-size: 15px; }
.entrance small { color: var(--txt-sub); font-size: 12px; }

/* 智能发现区 */
.discover { display: grid; grid-template-columns: minmax(0, 2.5fr) minmax(240px, 1fr); gap: 18px; margin-top: 26px; }
.discover-main { min-width: 0; }
.discover-main .section-head { margin-top: 0; margin-bottom: 4px; }
.discover-main .more { border: 1px solid var(--border-glass); padding: 5px 12px; border-radius: 999px; background: transparent; cursor: pointer; }
.rec-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 14px; }
.rec-card { position: relative; overflow: hidden; border-radius: 12px; cursor: pointer; }
.rec-img { width: 100%; height: 104px; object-fit: cover; display: block; }
.rec-rank {
  position: absolute; left: 8px; top: 8px; padding: 2px 8px; border-radius: 999px;
  font-size: 10.5px; font-weight: 700; color: #04121f; background: linear-gradient(120deg, #67e8f9, #a78bfa);
}
.fav-btn {
  position: absolute; right: 8px; top: 6px; width: 26px; height: 26px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,.3); background: rgba(4,10,22,.6); color: #cbd5e1;
  font-size: 16px; line-height: 1; cursor: pointer;
}
.fav-btn.on { color: #fbbf24; border-color: rgba(251,191,36,.65); }
.rec-body { padding: 9px 11px 11px; }
.rec-body b { display: block; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-reason { display: block; color: var(--cyan); font-size: 11px; margin: 4px 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-foot { display: flex; justify-content: space-between; align-items: center; color: var(--txt-sub); font-size: 11.5px; }
.rec-foot em { color: #4ade80; font-style: normal; font-weight: 700; font-size: 13px; }
.discover-side { padding: 16px 18px; border-radius: 14px; align-self: start; }
.side-title { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 600; margin-bottom: 12px; }
.pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: #4ade80; animation: pulse-dot 1.6s ease-in-out infinite; }
.feed-list { display: flex; flex-direction: column; gap: 8px; max-height: 216px; overflow: hidden; }
.feed-item { font-size: 12px; color: var(--txt-sub); line-height: 1.55; padding-bottom: 7px; border-bottom: 1px dashed rgba(255,255,255,.08); }
.feed-item b { color: var(--txt); }
.feed-item span { color: var(--cyan); }
.feed-item small { display: block; color: #4ade80; }
.hot-list { display: flex; flex-wrap: wrap; gap: 6px; }
.hot-chip {
  display: inline-flex; align-items: center; gap: 5px; padding: 4px 9px; border-radius: 8px;
  background: rgba(255,255,255,.06); border: 1px solid var(--border-glass);
  font-size: 11.5px; cursor: pointer; color: var(--txt-sub);
}
.hot-chip:hover { color: var(--cyan); border-color: var(--cyan); }
.hot-chip i { font-style: normal; font-size: 10px; color: var(--txt-sub); }
.hot-chip i.top { color: #f87171; font-weight: 700; }

.section-head { display: flex; align-items: baseline; justify-content: space-between; margin-top: 40px; }
.more { color: var(--txt-sub); font-size: 13px; text-decoration: none; }
.more:hover { color: var(--cyan); }

.spot-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-top: 18px; }
.spot-card { overflow: hidden; cursor: pointer; }
.spot-img { height: 180px; transition: transform 0.4s; }
.spot-card:hover .spot-img { transform: scale(1.06); }
.spot-body { padding: 14px 16px 16px; }
.spot-row { display: flex; justify-content: space-between; align-items: center; }
.spot-name { font-weight: 600; font-size: 16px; }
.spot-rating { color: #fbbf24; font-size: 14px; }
.spot-meta { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
.spot-price { margin-left: auto; color: var(--cyan); font-weight: 700; }

.prod-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 18px; }
.prod-card { overflow: hidden; cursor: pointer; }
.prod-img { height: 130px; }
.prod-body { padding: 12px 14px 14px; }
.prod-name { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.prod-desc { color: var(--txt-sub); font-size: 12px; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.prod-row { display: flex; justify-content: space-between; align-items: baseline; margin-top: 10px; }
.prod-price .now { color: var(--cyan); font-weight: 700; font-size: 18px; }
.prod-price .origin { color: var(--txt-sub); text-decoration: line-through; font-size: 12px; margin-left: 6px; }
.sales { color: var(--txt-sub); font-size: 12px; }

.guide-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 18px; }
.guide-card { display: flex; gap: 0; overflow: hidden; cursor: pointer; flex-direction: column; }
.guide-img { width: 100%; height: 160px; object-fit: cover; transition: transform 0.4s; }
.guide-card:hover .guide-img { transform: scale(1.05); }
.guide-body { padding: 14px 16px 16px; }
.guide-title { font-size: 15px; margin: 10px 0 8px; line-height: 1.5; }
.guide-desc { color: var(--txt-sub); font-size: 12.5px; line-height: 1.75; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.guide-meta { color: var(--txt-sub); font-size: 11.5px; margin-top: 10px; opacity: 0.8; }

.media-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 18px; }
.media-item { overflow: hidden; border-radius: 14px; }
.media-content { width: 100%; height: 190px; object-fit: cover; display: block; background: #000; }
.media-desc { margin: 0; padding: 10px 14px 12px; font-size: 12.5px; color: var(--txt-sub); }

/* 数据大屏入口横幅 */
.screen-banner {
  margin-top: 44px;
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 26px 34px; border-radius: 18px; cursor: pointer;
  background: linear-gradient(115deg, rgba(14, 165, 233, 0.16), rgba(45, 212, 191, 0.1) 55%, rgba(139, 92, 246, 0.14));
  transition: box-shadow 0.3s, transform 0.3s;
}
.screen-banner:hover { box-shadow: 0 0 30px rgba(94, 234, 255, 0.25); transform: translateY(-2px); }
.sb-badge {
  display: inline-block; font-size: 11px; letter-spacing: 3px; color: var(--cyan);
  border: 1px solid rgba(94, 234, 255, 0.45); border-radius: 999px; padding: 3px 12px; margin-bottom: 10px;
}
.sb-title { font-size: 26px; margin: 0 0 8px; letter-spacing: 1px; }
.sb-desc { color: var(--txt-sub); font-size: 13.5px; line-height: 1.8; margin: 0; max-width: 760px; }
.sb-btn { flex-shrink: 0; padding: 13px 30px; font-size: 15px; border-radius: 12px; }

.stats-bar { margin-top: 44px; display: grid; grid-template-columns: repeat(4, 1fr); padding: 26px 10px; text-align: center; }
.stat b { display: block; font-size: 30px; }
.stat span { color: var(--txt-sub); font-size: 13px; }

@media (max-width: 1080px) {
  .roll-panel { width: 280px; }
  .hero-title { font-size: 36px; }
  .spot-grid, .prod-grid { grid-template-columns: repeat(2, 1fr); }
  .guide-grid, .media-grid { grid-template-columns: repeat(2, 1fr); }
  .rec-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 860px) {
  .roll-panel { display: none; }
  .hero-inner { padding: 30px 26px 70px; }
  .entrance-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 30px; }
  .stats-bar { grid-template-columns: repeat(2, 1fr); gap: 18px; }
  .discover { grid-template-columns: 1fr; }
  .rec-grid { grid-template-columns: repeat(2, 1fr); }
  .feed-list { max-height: 180px; }
}
</style>
