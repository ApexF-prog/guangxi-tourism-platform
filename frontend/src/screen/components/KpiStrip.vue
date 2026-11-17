<template>
  <div class="kpi-strip">
    <div v-for="item in data.kpiList" :key="item.label" class="kpi" :class="`kpi--${item.tone}`">
      <span class="kpi__label">{{ item.label }}</span>
      <span class="kpi__value">
        {{ item.value }}<em class="kpi__unit">{{ item.unit }}</em>
      </span>
    </div>
  </div>
</template>

<script setup>
import { data } from '@screen/data/tourism'
</script>
