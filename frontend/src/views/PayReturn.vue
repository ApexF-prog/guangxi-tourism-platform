<template>
  <main class="page pay-page">
    <div class="pay-card glass" style="text-align: center; padding: 50px 30px">
      <template v-if="state === 'checking'">
        <el-icon class="is-loading" :size="42" color="#38bdf8"><Loading /></el-icon>
        <h2 style="margin: 18px 0 6px">支付完成，正在确认结果…</h2>
        <p style="color: var(--txt-sub)">正在向支付宝核实交易状态，请稍候</p>
      </template>
      <template v-else-if="state === 'paid'">
        <div class="success-icon">✓</div>
        <h2>支付成功！</h2>
        <p style="color: var(--txt-sub)">订单已确认，感谢您的预订</p>
        <button class="btn-grad" style="padding: 10px 30px" @click="$router.push('/orders')">查看我的订单</button>
      </template>
      <template v-else>
        <h2>暂未查询到支付结果</h2>
        <p style="color: var(--txt-sub)">如果你已完成付款，稍后在「我的订单」中会自动更新</p>
        <button class="btn-ghost" style="padding: 10px 26px" @click="$router.push('/orders')">前往订单页</button>
      </template>
    </div>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import request from '../api.js'

const route = useRoute()
const state = ref('checking')

onMounted(async () => {
  const orderNo = route.query.out_trade_no
  if (!orderNo) return (state.value = 'unknown')
  for (let i = 0; i < 5; i++) {
    try {
      const o = await request.get('/pay/alipay/query/' + orderNo, { silent: true })
      if (o.status === 'PAID') return (state.value = 'paid')
    } catch { /* 继续重试 */ }
    await new Promise(r => setTimeout(r, 2500))
  }
  state.value = 'unknown'
})
</script>

<style scoped>
.pay-page { display: flex; justify-content: center; }
.pay-card { width: 100%; max-width: 520px; }
.success-icon {
  width: 74px; height: 74px; margin: 0 auto 18px;
  display: grid; place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #22d3ee);
  color: #04121f; font-size: 38px; font-weight: 800;
}
</style>
