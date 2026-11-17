<template>
  <div v-loading="loading">
    <h2 class="page-title">📊 经营看板</h2>
    <p class="page-sub">数据仅统计你自己店铺的订单（商家之间相互隔离）</p>

    <div class="stat-cards">
      <div class="stat-card glass"><span class="label">今日营业额</span><b class="grad-text">¥{{ stats.todayAmount ?? '0.00' }}</b></div>
      <div class="stat-card glass"><span class="label">今日订单数</span><b>{{ stats.todayOrders ?? 0 }}</b></div>
      <div class="stat-card glass"><span class="label">累计营业额</span><b>¥{{ stats.totalAmount ?? '0.00' }}</b></div>
      <div class="stat-card glass"><span class="label">累计已支付订单</span><b>{{ stats.paidOrders ?? 0 }}</b></div>
    </div>

    <h3 class="page-title" style="font-size: 18px; margin-top: 30px">🏆 商品销量排行 TOP10</h3>
    <el-table :data="stats.topProducts || []" style="width: 100%" class="glass" size="large">
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="商品" min-width="220" />
      <el-table-column prop="shopName" label="" width="0" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ { FOOD: '美食', DRINK: '饮品', PACKAGE: '套餐', TICKET: '门票' }[row.type] }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="110">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="sales" label="累计销量" width="110" sortable />
      <el-table-column prop="stock" label="库存" width="100" />
    </el-table>
    <el-empty v-if="!loading && (stats.topProducts || []).length === 0" description="还没有商品，去「商品管理」添加吧" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api.js'

const stats = ref({})
const loading = ref(true)

onMounted(async () => {
  try {
    stats.value = await request.get('/merchant/stats')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 18px; }
.stat-card { padding: 20px 22px; }
.stat-card .label { display: block; color: var(--txt-sub); font-size: 13px; margin-bottom: 8px; }
.stat-card b { font-size: 26px; }
@media (max-width: 820px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } }
</style>
