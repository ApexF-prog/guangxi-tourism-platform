<template>
  <div class="tech-hud" aria-hidden="true">
    <div class="hud-grid"></div>
    <div class="hud-edge"></div>
    <div class="hud-corner hud-corner--tl"></div>
    <div class="hud-corner hud-corner--tr"></div>
    <div class="hud-corner hud-corner--bl"></div>
    <div class="hud-corner hud-corner--br"></div>
    <div class="hud-rail hud-rail--left">GUANGXI · TOURISM DATA PLATFORM</div>
    <div class="hud-rail hud-rail--right">SYSTEM ONLINE <i class="hud-dot"></i></div>
    <div class="hud-ticks"></div>
  </div>
</template>

<script setup>
/**
 * 全局 HUD 装饰层：极细网格 + 四角准星 + 侧边状态轨。
 * 纯装饰、pointer-events: none，不影响任何点击、不改动页面布局。
 * 数据大屏页不挂载此组件，避免与 ECharts 大屏叠加。
 */
</script>

<style scoped>
.tech-hud {
  position: fixed;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
  font-family: "Bahnschrift", "DIN Alternate", Consolas, monospace;
}

/* 细网格：比背景已有的透视网格更密、更淡，形成「屏幕像素」质感 */
.hud-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(120, 220, 255, 0.036) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 220, 255, 0.036) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.85), transparent 65%);
}

/* 四边内发光暗角 */
.hud-edge {
  position: absolute;
  inset: 0;
  box-shadow:
    inset 0 0 1px rgba(120, 220, 255, 0.18),
    inset 0 0 90px rgba(3, 8, 18, 0.42);
}

/* 四角准星 */
.hud-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: rgba(56, 189, 248, 0.5);
  border-style: solid;
  border-width: 0;
}
.hud-corner--tl { top: 12px; left: 12px; border-top-width: 1px; border-left-width: 1px; }
.hud-corner--tr { top: 12px; right: 12px; border-top-width: 1px; border-right-width: 1px; }
.hud-corner--bl { bottom: 12px; left: 12px; border-bottom-width: 1px; border-left-width: 1px; }
.hud-corner--br { bottom: 12px; right: 12px; border-bottom-width: 1px; border-right-width: 1px; }

/* 左右侧边状态轨 */
.hud-rail {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  writing-mode: vertical-rl;
  font-size: 9px;
  letter-spacing: 0.28em;
  color: rgba(140, 210, 255, 0.32);
  white-space: nowrap;
}
.hud-rail--left { left: 10px; }
.hud-rail--right { right: 10px; transform: translateY(-50%) rotate(180deg); }

.hud-dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  margin-top: 8px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 8px #22d3ee;
  animation: hud-breathe 2.2s ease-in-out infinite;
}
@keyframes hud-breathe {
  0%, 100% { opacity: 0.35; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.15); }
}

/* 底部刻度线 */
.hud-ticks {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: repeating-linear-gradient(90deg,
    rgba(56, 189, 248, 0.27) 0 1px, transparent 1px 40px);
  opacity: 0.6;
}

@media (max-width: 900px) {
  .hud-rail { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .hud-dot { animation: none; }
}
</style>
