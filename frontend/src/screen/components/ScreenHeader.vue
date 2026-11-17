<template>
  <header class="screen-header">
    <div class="header-side">
      <span class="header-badge"><i></i>{{ meta.live ? '数据实时接入' : '离线基线数据' }}</span>
      <span class="header-clock">{{ clock }}</span>
    </div>
    <div class="title-wrap">
      <h1 class="screen-title">广西旅游可视化大屏</h1>
      <p class="screen-subtitle">GUANGXI TOURISM VISUALIZATION DASHBOARD</p>
    </div>
    <div class="header-side header-side--right">
      <span>{{ dateText }} {{ weekText }}</span>
      <span class="header-badge">{{ meta.live ? '数据更新 ' + meta.updatedAt : '等待后端数据' }}</span>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { meta } from '@screen/data/tourism'

const now = ref(new Date())
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onBeforeUnmount(() => clearInterval(timer))

const pad = (n) => String(n).padStart(2, '0')

const clock = computed(() => {
  const d = now.value
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

const dateText = computed(() => {
  const d = now.value
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
})

const weekText = computed(
  () => '星期' + ['日', '一', '二', '三', '四', '五', '六'][now.value.getDay()])
</script>
