<template>
  <div class="tech-bg" aria-hidden="true">
    <div class="blob blob-a"></div>
    <div class="blob blob-b"></div>
    <div class="blob blob-c"></div>
    <div class="blob blob-d"></div>
    <div class="blob blob-e"></div>

    <div class="aurora aurora-a"></div>
    <div class="aurora aurora-b"></div>

    <div class="beam beam-a"></div>
    <div class="beam beam-b"></div>
    <div class="beam beam-c"></div>

    <div class="grid-layer"></div>
    <div class="grid-layer grid-layer--back"></div>
    <canvas ref="canvas" class="particles"></canvas>

    <div class="cursor-glow"></div>
    <div class="vignette"></div>
  </div>
</template>

<script setup>
/**
 * 全局动态背景（强化版）
 * 层次：暗色底 → 5 团漂移极光 → 2 条极光带 → 3 道扫描光束
 *       → 双层反向透视网格 → Canvas 粒子 / 连线 / 脉冲环 / 流星
 *       → 鼠标跟随光晕 → 暗角
 * 全部固定在 z-index:-1，组件保持半透明，背景可以透过看到。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvas = ref(null)
let ctx, raf, particles = [], pulses = [], meteors = []
let width = 0, height = 0, dpr = 1
let mx = -9999, my = -9999, tx = -9999, ty = -9999
let reduce = false, frame = 0
let onMove

function resize() {
  const cv = canvas.value
  if (!cv) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  cv.width = width * dpr
  cv.height = height * dpr
  cv.style.width = width + 'px'
  cv.style.height = height + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  const count = Math.min(175, Math.round((width * height) / 11000))
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    r: Math.random() * 2.3 + 0.8,
    vx: (Math.random() - 0.5) * 0.38,
    vy: (Math.random() - 0.5) * 0.38,
    hue: Math.random() > 0.62 ? 268 : 186,
    a: Math.random() * 0.6 + 0.34
  }))
}

function spawnPulse() {
  pulses.push({
    x: Math.random() * width,
    y: Math.random() * height,
    r: 0,
    max: 200 + Math.random() * 240,
    a: 0.42
  })
  if (pulses.length > 7) pulses.shift()
}

function spawnMeteor() {
  const fromLeft = Math.random() > 0.5
  meteors.push({
    x: fromLeft ? -100 : width * (0.3 + Math.random() * 0.7),
    y: Math.random() * height * 0.6,
    len: 110 + Math.random() * 150,
    speed: 8 + Math.random() * 6,
    a: 0.82
  })
  if (meteors.length > 5) meteors.shift()
}

function draw() {
  frame++
  ctx.clearRect(0, 0, width, height)
  // 鼠标平滑跟随，避免光晕抖动
  mx += (tx - mx) * 0.08
  my += (ty - my) * 0.08

  // 周期性脉冲环与流星
  if (frame % 150 === 0) spawnPulse()
  if (frame % 250 === 0) spawnMeteor()

  // 脉冲扩散环
  for (const p of pulses) {
    p.r += 2.7
    p.a *= 0.985
    ctx.beginPath()
    ctx.strokeStyle = `rgba(56, 189, 248, ${Math.max(0, p.a)})`
    ctx.lineWidth = 1.2
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.strokeStyle = `rgba(129, 140, 248, ${Math.max(0, p.a * 0.62)})`
    ctx.arc(p.x, p.y, p.r * 0.66, 0, Math.PI * 2)
    ctx.stroke()
  }
  pulses = pulses.filter(p => p.r < p.max && p.a > 0.012)

  // 流星光迹
  for (const m of meteors) {
    m.x += m.speed
    m.y += m.speed * 0.42
    m.a *= 0.99
    const g = ctx.createLinearGradient(m.x, m.y, m.x - m.len, m.y - m.len * 0.42)
    g.addColorStop(0, `rgba(160, 240, 255, ${Math.max(0, m.a)})`)
    g.addColorStop(1, 'rgba(56, 189, 248, 0)')
    ctx.beginPath()
    ctx.strokeStyle = g
    ctx.lineWidth = 1.6
    ctx.moveTo(m.x, m.y)
    ctx.lineTo(m.x - m.len, m.y - m.len * 0.42)
    ctx.stroke()
  }
  meteors = meteors.filter(m => m.x < width + 260 && m.a > 0.03)

  // 粒子：缓慢漂移 + 鼠标轻微排斥
  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    if (mx > -9000) {
      const dx = p.x - mx, dy = p.y - my, d2 = dx * dx + dy * dy
      if (d2 < 30000 && d2 > 1) {
        const f = (1 - d2 / 30000) * 0.62
        p.x += (dx / Math.sqrt(d2)) * f
        p.y += (dy / Math.sqrt(d2)) * f
      }
    }
    if (p.x < -20) p.x = width + 20
    if (p.x > width + 20) p.x = -20
    if (p.y < -20) p.y = height + 20
    if (p.y > height + 20) p.y = -20
    ctx.beginPath()
    ctx.fillStyle = `hsla(${p.hue}, 92%, 72%, ${p.a})`
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }

  // 粒子之间连线
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const a = particles[i], b = particles[j]
      const dx = a.x - b.x, dy = a.y - b.y
      const d2 = dx * dx + dy * dy
      if (d2 < 30000) {
        ctx.strokeStyle = `rgba(94, 234, 255, ${0.22 * (1 - d2 / 30000)})`
        ctx.lineWidth = 0.8
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }
  }

  // 鼠标 → 附近粒子的数据线
  if (mx > -9000) {
    for (const p of particles) {
      const dx = p.x - mx, dy = p.y - my
      const d2 = dx * dx + dy * dy
      if (d2 < 40000) {
        ctx.strokeStyle = `rgba(129, 230, 255, ${0.34 * (1 - d2 / 40000)})`
        ctx.lineWidth = 0.9
        ctx.beginPath()
        ctx.moveTo(mx, my)
        ctx.lineTo(p.x, p.y)
        ctx.stroke()
      }
    }
  }
  raf = requestAnimationFrame(draw)
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  resize()
  // 用户要求：鼠标移动时背景不要跟着动，固定住；只保留与鼠标无关的自动动画
  window.addEventListener('resize', resize)
  if (!reduce) draw()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.tech-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  background:
    radial-gradient(1300px 760px at 12% -12%, #0d3352 0%, transparent 62%),
    radial-gradient(1100px 660px at 92% 8%, #241554 0%, transparent 58%),
    linear-gradient(180deg, #04070f 0%, #060b18 48%, #03060d 100%);
}

/* ---- 漂移极光团：幅度更大、周期更短 ---- */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(88px);
  opacity: 0.82;
  will-change: transform;
}
.blob-a {
  width: 52vw; height: 52vw; left: -12vw; top: -14vw;
  background: radial-gradient(circle, rgba(64, 196, 255, 0.72), transparent 66%);
  animation: drift-a 14s ease-in-out infinite;
}
.blob-b {
  width: 46vw; height: 46vw; right: -10vw; top: 12vh;
  background: radial-gradient(circle, rgba(160, 92, 255, 0.66), transparent 66%);
  animation: drift-b 17s ease-in-out infinite;
}
.blob-c {
  width: 44vw; height: 44vw; left: 24vw; bottom: -20vw;
  background: radial-gradient(circle, rgba(0, 255, 214, 0.46), transparent 66%);
  animation: drift-c 12s ease-in-out infinite;
}
.blob-d {
  width: 34vw; height: 34vw; right: 16vw; bottom: -12vw;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.52), transparent 66%);
  animation: drift-d 16s ease-in-out infinite;
}
.blob-e {
  width: 30vw; height: 30vw; left: 40vw; top: -18vw;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.32), transparent 66%);
  animation: drift-e 19s ease-in-out infinite;
}
@keyframes drift-a {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(16vw, 12vh, 0) scale(1.26); }
}
@keyframes drift-b {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1.08); }
  50% { transform: translate3d(-18vw, 15vh, 0) scale(0.86); }
}
@keyframes drift-c {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-15vw, -13vh, 0) scale(1.32); }
}
@keyframes drift-d {
  0%, 100% { transform: translate3d(0, 0, 0) scale(0.92); }
  50% { transform: translate3d(-12vw, -16vh, 0) scale(1.2); }
}
@keyframes drift-e {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1.1); }
  50% { transform: translate3d(14vw, 14vh, 0) scale(0.85); }
}

/* ---- 斜向极光带：横向扫过，形成前后视差 ---- */
.aurora {
  position: absolute;
  width: 90vw;
  height: 34vh;
  left: -20vw;
  filter: blur(42px);
  opacity: 0.62;
  transform: rotate(-14deg);
  will-change: transform;
}
.aurora-a {
  top: 16vh;
  background: linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.38) 30%, rgba(129, 140, 248, 0.28) 62%, transparent);
  animation: aurora-sweep 18s ease-in-out infinite;
}
.aurora-b {
  top: 58vh;
  background: linear-gradient(90deg, transparent, rgba(45, 212, 191, 0.32) 34%, rgba(59, 130, 246, 0.26) 66%, transparent);
  animation: aurora-sweep 23s ease-in-out infinite reverse;
}
@keyframes aurora-sweep {
  0%, 100% { transform: translate3d(-10vw, 0, 0) rotate(-14deg) scaleY(0.85); opacity: 0.34; }
  50% { transform: translate3d(34vw, -9vh, 0) rotate(-9deg) scaleY(1.25); opacity: 0.72; }
}

/* ---- 扫描光束：细长渐变条斜穿屏幕 ---- */
.beam {
  position: absolute;
  top: -40vh;
  height: 160vh;
  width: 3px;
  background: linear-gradient(180deg, transparent, rgba(120, 230, 255, 0.70), transparent);
  transform: rotate(18deg);
  filter: blur(1.5px);
  opacity: 0;
  will-change: transform;
}
.beam::after {
  content: '';
  position: absolute;
  inset: 0 -18px;
  background: linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.13), transparent);
}
.beam-a { left: 12vw; animation: beam-pass 11s linear infinite; }
.beam-b { left: 52vw; animation: beam-pass 14s linear infinite 4s; }
.beam-c { left: 84vw; animation: beam-pass 12s linear infinite 7s; }
@keyframes beam-pass {
  0% { transform: translateX(-40vw) rotate(18deg); opacity: 0; }
  12% { opacity: 0.9; }
  70% { opacity: 0.7; }
  100% { transform: translateX(40vw) rotate(18deg); opacity: 0; }
}

/* ---- 双层反向滚动的透视网格 ---- */
.grid-layer {
  position: absolute;
  inset: -20%;
  background-image:
    linear-gradient(rgba(120, 220, 255, 0.11) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 220, 255, 0.11) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(circle at 50% 40%, rgba(0, 0, 0, 0.95), transparent 74%);
  transform: perspective(600px) rotateX(56deg) translateY(-10%);
  animation: grid-move 9s linear infinite;
}
.grid-layer--back {
  background-image:
    linear-gradient(rgba(160, 140, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(160, 140, 255, 0.08) 1px, transparent 1px);
  background-size: 110px 110px;
  opacity: 0.75;
  animation: grid-move-back 16s linear infinite;
}
@keyframes grid-move {
  from { background-position: 0 0, 0 0; }
  to { background-position: 0 64px, 64px 0; }
}
@keyframes grid-move-back {
  from { background-position: 0 0, 0 0; }
  to { background-position: 0 -110px, -110px 0; }
}

.particles { position: absolute; inset: 0; }

/* ---- 鼠标跟随光晕（坐标由 utils/fx.js 广播） ---- */
.cursor-glow {
  position: absolute;
  left: var(--mx, -1000px);
  top: var(--my, -1000px);
  width: 760px;
  height: 760px;
  margin: -380px 0 0 -380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.20) 0%, rgba(99, 102, 241, 0.12) 38%, transparent 70%);
  mix-blend-mode: screen;
  pointer-events: none;
  transition: opacity 0.4s;
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 40%, rgba(3, 6, 12, 0.72) 100%);
}

@media (prefers-reduced-motion: reduce) {
  .blob, .aurora, .beam, .grid-layer { animation: none; }
  .beam { display: none; }
  .cursor-glow { display: none; }
}
</style>
