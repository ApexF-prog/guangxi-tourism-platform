<template>
  <div>
    <h2 class="page-title">🧾 订单管理</h2>
    <p class="page-sub">仅显示你自己店铺产生的订单（其他商家的订单不可见）</p>

    <el-radio-group v-model="status" style="margin-bottom: 16px" @change="load">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="PAID">已支付</el-radio-button>
      <el-radio-button value="PENDING">待支付</el-radio-button>
      <el-radio-button value="CANCELED">已取消</el-radio-button>
    </el-radio-group>

    <el-table :data="orders" v-loading="loading" style="width: 100%" size="large">
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="productName" label="商品" min-width="200" />
      <el-table-column prop="quantity" label="数量" width="70" />
      <el-table-column prop="total" label="金额" width="100">
        <template #default="{ row }">¥{{ row.total }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="{ PENDING: 'warning', PAID: 'success', CANCELED: 'info' }[row.status]" effect="dark" round size="small">
            {{ { PENDING: '待支付', PAID: '已支付', CANCELED: '已取消' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: center; margin-top: 20px">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api.js'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const status = ref('')
const loading = ref(false)

async function load(p = 1) {
  loading.value = true
  page.value = p
  try {
    const data = await request.get('/merchant/orders', { params: { page: p, size, status: status.value || undefined } })
    orders.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
