<template>
  <div class="nf">
    <div class="nf-glow"></div>
    <p class="nf-code">404</p>
    <h1 class="nf-title">这条山路没走通</h1>
    <p class="nf-sub">你访问的页面不存在，可能已经下架或地址输错了。换个入口继续逛广西。</p>
    <div class="nf-actions">
      <button class="nf-btn nf-btn-main" @click="$router.push('/')">回到首页</button>
      <button class="nf-btn" @click="$router.push('/spots')">浏览景点</button>
      <button class="nf-btn" @click="$router.push('/screen')">数据大屏</button>
    </div>
    <p class="nf-tip">如果是从站内链接进来的，可以点右上角返回上一页</p>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  document.title = '页面不存在 · 广西旅游网'
})
</script>

<style scoped>
.nf { position: relative; min-height: 82vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center; text-align: center; padding: 60px 24px; }
.nf-glow { position: absolute; width: 520px; height: 520px; border-radius: 50%;
  background: radial-gradient(circle, rgba(56,189,248,.22) 0%, rgba(56,189,248,0) 68%);
  filter: blur(6px); pointer-events: none; }
.nf-code { position: relative; font-size: clamp(84px, 16vw, 160px); font-weight: 800; line-height: 1;
  margin: 0; letter-spacing: .04em;
  background: linear-gradient(120deg, #38bdf8, #818cf8 46%, #34d399);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  text-shadow: 0 0 60px rgba(56,189,248,.28); }
.nf-title { position: relative; margin: 18px 0 10px; font-size: clamp(22px, 3vw, 32px); color: #eaf6ff; }
.nf-sub { position: relative; max-width: 520px; margin: 0 auto; font-size: 15px;
  line-height: 1.8; color: #c2d6ef; }
.nf-actions { position: relative; display: flex; flex-wrap: wrap; gap: 12px;
  justify-content: center; margin-top: 28px; }
.nf-btn { padding: 11px 26px; border-radius: 999px; font-size: 14px; cursor: pointer;
  color: #dcecff; background: rgba(56,189,248,.10);
  border: 1px solid rgba(125,211,252,.34); transition: transform .18s ease, opacity .18s ease; }
.nf-btn:hover { transform: translateY(-2px); opacity: .9; }
.nf-btn-main { color: #04121f; font-weight: 700; border: none;
  background: linear-gradient(90deg, #38bdf8, #7dd3fc);
  box-shadow: 0 10px 26px rgba(56,189,248,.34); }
.nf-tip { position: relative; margin-top: 26px; font-size: 12px; color: #8fb6dd; }
</style>
