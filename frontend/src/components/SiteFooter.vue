<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <!-- 三列服务说明 + 热线/二维码 -->
      <div class="footer-top">
        <div class="faq-col" v-for="g in SERVICE_GROUPS" :key="g.title">
          <h3>{{ g.title }}</h3>
          <a v-for="i in g.items" :key="i" href="#" @click.prevent>{{ i }}</a>
        </div>
        <div class="hotline">
          <h3>免费服务热线</h3>
          <p class="phone">400-861-0773</p>
          <p class="small">服务时间 08:00 - 22:00</p>
          <div class="qr-box">
            <img src="/images/wechat-qr.svg" alt="微信公众号二维码" class="qr" />
            <span>扫码关注 · 广西旅游网</span>
          </div>
        </div>
      </div>

      <!-- 友情链接 -->
      <div class="friend-links">
        <span class="label">友情链接：</span>
        <a v-for="l in FRIEND_LINKS" :key="l" href="#" @click.prevent>{{ l }}</a>
      </div>

      <!-- 站点导航列 -->
      <div class="footer-cols">
        <div class="brand-col">
          <div class="brand">GX 广西旅游网</div>
          <p>桂林 · 柳州 · 南宁 · 北海<br />门票 / 美食 / 奶茶 / 团购一站预订</p>
          <p class="small">Vue3 + Spring Boot + MyBatis + MySQL<br />支付宝沙箱支付 · 个人全栈作品</p>
        </div>
        <div class="nav-col">
          <h3>关于我们</h3>
          <router-link to="/service">关于我们</router-link>
          <router-link to="/service">联系我们</router-link>
          <router-link to="/service">支付方式</router-link>
          <router-link to="/service">商务合作</router-link>
        </div>
        <div class="nav-col">
          <h3>快捷导航</h3>
          <router-link to="/travel">旅游资讯</router-link>
          <router-link to="/play">玩转广西</router-link>
          <router-link to="/spots">景点门票</router-link>
          <router-link to="/shops">美食推荐</router-link>
          <router-link to="/screen">广西旅游可视化大屏</router-link>
          <router-link to="/tours">跟团游报名</router-link>
        </div>
        <div class="nav-col">
          <h3>新手入门</h3>
          <router-link to="/login">注册用户</router-link>
          <router-link to="/service">用户帮助中心</router-link>
          <router-link to="/orders">订单查询</router-link>
          <router-link to="/service">取餐号说明</router-link>
        </div>
        <div class="nav-col">
          <h3>网站说明</h3>
          <router-link to="/service">网站服务协议</router-link>
          <router-link to="/service">网站注册协议</router-link>
          <router-link to="/service">网站版权声明</router-link>
          <router-link to="/service">隐私与安全</router-link>
        </div>
      </div>

      <div class="copyright">
        <p>Copyright © 2026 广西旅游网 · 个人学习实践项目，非商业运营 · 支付为支付宝沙箱环境演示</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { SERVICE_GROUPS, FRIEND_LINKS } from '../data/content.js'
</script>

<style scoped>
.site-footer {
  margin-top: 60px;
  border-top: 1px solid rgba(120, 220, 255, 0.14);
  background: linear-gradient(180deg, rgba(8, 14, 26, 0.72), rgba(4, 8, 16, 0.94));
  backdrop-filter: blur(14px);
}
.footer-inner { max-width: 1240px; margin: 0 auto; padding: 38px 20px 20px; }
.footer-top {
  display: grid;
  grid-template-columns: repeat(3, 1fr) 1.15fr;
  gap: 26px;
  padding-bottom: 26px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.faq-col h3, .hotline h3, .nav-col h3 {
  font-size: 14px;
  margin: 0 0 12px;
  color: var(--txt);
  letter-spacing: 0.5px;
}
.faq-col a {
  display: block;
  color: var(--txt-sub);
  font-size: 12.5px;
  line-height: 2.05;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.faq-col a:hover { color: var(--cyan); }
.hotline .phone { font-size: 26px; font-weight: 800; margin: 0; letter-spacing: 1px; color: var(--cyan); }
.hotline .small { color: var(--txt-sub); font-size: 12px; margin: 6px 0 12px; }
.qr-box { display: flex; align-items: center; gap: 12px; }
.qr {
  width: 92px;
  height: 92px;
  flex-shrink: 0;
  border-radius: 8px;
  background: #fff;
  padding: 4px;
  object-fit: contain;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}
.qr-box span { color: var(--txt-sub); font-size: 12px; line-height: 1.7; }

.friend-links {
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  font-size: 12px;
  line-height: 2;
}
.friend-links .label { color: var(--txt); font-weight: 600; margin-right: 6px; }
.friend-links a {
  color: var(--txt-sub);
  text-decoration: none;
  margin-right: 14px;
  white-space: nowrap;
}
.friend-links a:hover { color: var(--cyan); }

.footer-cols {
  display: grid;
  grid-template-columns: 1.5fr repeat(4, 1fr);
  gap: 20px;
  padding: 24px 0 14px;
}
.brand-col .brand { font-weight: 800; font-size: 17px; margin-bottom: 10px; }
.brand-col p { color: var(--txt-sub); font-size: 12.5px; line-height: 1.9; margin: 0 0 8px; }
.brand-col .small { font-size: 11.5px; opacity: 0.75; }
.nav-col a {
  display: block;
  color: var(--txt-sub);
  font-size: 12.5px;
  line-height: 2.1;
  text-decoration: none;
}
.nav-col a:hover { color: var(--cyan); }
.copyright {
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  padding-top: 16px;
  text-align: center;
  color: var(--txt-sub);
  font-size: 12px;
  opacity: 0.8;
}
@media (max-width: 980px) {
  .footer-top { grid-template-columns: 1fr 1fr; }
  .footer-cols { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 620px) {
  .footer-top, .footer-cols { grid-template-columns: 1fr; }
}
</style>
