<template>
  <section class="panel">
    <i class="panel__corner tl"></i>
    <i class="panel__corner tr"></i>
    <i class="panel__corner bl"></i>
    <i class="panel__corner br"></i>

    <header class="panel__head">
      <span class="panel__dot"></span>
      <h2 class="panel__title">{{ title }}</h2>
      <em v-if="subtitle" class="panel__sub">{{ subtitle }}</em>
    </header>

    <div class="panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
// 通用面板外壳：只负责边框、标题栏、插槽，图表由外部传进来
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>
