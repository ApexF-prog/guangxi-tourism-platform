<template>
  <main class="page">
    <h1 class="page-title">服务中心</h1>
    <p class="page-sub">常见问题 · 预订支付 · 会员权益 · 跟团游 · 美食外带 · 服务保障</p>

    <!-- 联系横条 -->
    <div class="contact-bar glass">
      <div class="cb-item">
        <span class="cb-label">服务热线</span>
        <b class="phone">400-861-0773</b>
        <small>08:00 - 22:00（7×12 小时）</small>
      </div>
      <div class="cb-item">
        <span class="cb-label">跟团游咨询</span>
        <b class="phone">19175657645</b>
        <small>微信同号 · waxrl0911</small>
      </div>
      <div class="cb-item">
        <span class="cb-label">在线客服</span>
        <b>客服 QQ 9519990</b>
        <small>邮箱 service@gx-tourism.demo</small>
      </div>
      <div class="cb-qr">
        <img src="/images/wechat-qr.svg" alt="公众号二维码" />
        <span>扫码关注公众号<br />获取实时退改通知</span>
      </div>
    </div>

    <div class="svc-grid">
      <div v-for="g in pagedGroups" :key="g.title" class="svc-card glass">
        <h3>{{ g.title }}</h3>
        <ul>
          <li v-for="i in g.items" :key="i" @click="openFaq(g.title, i)">{{ i }}</li>
        </ul>
      </div>
    </div>

    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="SERVICE_GROUPS.length" :page-size="3"
                     :current-page="page" @current-change="p => (page = p)" />
    </div>

    <div class="blocks">
      <section v-for="b in SERVICE_BLOCKS" :key="b.title" class="glass block">
        <h3>{{ b.icon }} {{ b.title }}</h3>
        <p v-for="l in b.lines" :key="l">{{ l }}</p>
      </section>
    </div>

    <el-dialog v-model="visible" :title="faqTitle" width="460px">
      <p style="color: var(--txt-sub); line-height: 1.9">{{ faqAnswer }}</p>
      <template #footer>
        <el-button type="primary" @click="visible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue'
import { SERVICE_GROUPS, SERVICE_BLOCKS } from '../data/content.js'

const visible = ref(false)
const faqTitle = ref('')
const faqAnswer = ref('')
const page = ref(1)

const pagedGroups = computed(() => SERVICE_GROUPS.slice((page.value - 1) * 3, page.value * 3))

function openFaq(group, item) {
  faqTitle.value = item
  faqAnswer.value = `【${group}】${item}：本项目为课程实践作品，遇到该问题时可在「我的订单」中自助处理；`
    + '如仍有疑问，请拨打服务热线 400-861-0773 或联系在线客服，我们会尽快为你解决。'
  visible.value = true
}
</script>

<style scoped>
.contact-bar {
  display: flex; align-items: center; gap: 20px;
  padding: 20px 26px; margin-bottom: 20px; flex-wrap: wrap;
}
.cb-item { flex: 1; min-width: 180px; display: flex; flex-direction: column; gap: 4px; }
.cb-label { color: var(--txt-sub); font-size: 12px; letter-spacing: 1px; }
.cb-item b { font-size: 19px; }
.cb-item .phone { color: var(--cyan); font-weight: 800; }
.cb-item small { color: var(--txt-sub); font-size: 11.5px; }
.cb-qr { display: flex; gap: 12px; align-items: center; }
.cb-qr img { width: 72px; height: 72px; border-radius: 8px; background: #fff; padding: 4px; }
.cb-qr span { color: var(--txt-sub); font-size: 12px; line-height: 1.7; }

.svc-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; min-height: 300px; }
.svc-card { padding: 20px 22px; }
.svc-card h3 { margin: 0 0 14px; font-size: 15px; color: var(--cyan); }
.svc-card ul { list-style: none; margin: 0; padding: 0; }
.svc-card li {
  color: var(--txt-sub); font-size: 13px; line-height: 2.1; cursor: pointer;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.07); padding: 2px 0;
}
.svc-card li:last-child { border-bottom: none; }
.svc-card li:hover { color: var(--cyan); }
.pager { display: flex; justify-content: center; margin: 22px 0 6px; }

.blocks { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
.block { padding: 20px 24px; }
.block h3 { margin: 0 0 12px; font-size: 15px; }
.block p { color: var(--txt-sub); font-size: 13px; line-height: 1.95; margin: 0 0 6px; }
@media (max-width: 960px) {
  .svc-grid { grid-template-columns: 1fr; }
  .blocks { grid-template-columns: 1fr; }
}
</style>
