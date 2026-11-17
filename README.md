# 广西旅游平台

> 广西旅游实时数据分析与服务平台 —— 一个前后端分离的旅游电商全栈项目。Vue 3 + Spring Boot 3 + MyBatis-Plus + MySQL 8，集成支付宝沙箱支付、会员体系、跟团游、商家后台、平台管理后台与 ECharts 可视化大屏。

---

## ✨ 项目亮点

- **前后端分离全栈**：Vue 3.5 + Vite 6 + Element Plus + Vue Router + axios；Spring Boot 3.2 + MyBatis-Plus + MySQL 8。统一 `Result<T>` 响应、全局异常处理、JWT 拦截器鉴权。
- **真实支付链路**：支付宝沙箱收银台（EasySDK 2.2.3，默认 `alipay.trade.wap.pay`），服务端异步通知验签 + 金额核验 + 幂等入账，前端主动查单兜底；金额统一用 `BigDecimal` 后端重算，会员折扣由策略类结算。
- **可视化大屏**：ECharts 6 实现实时订单播报、热搜榜、行为画像推荐、客流分析。
- **完整业务闭环**：门票 / 美食 / 饮品 / 团购 / 跟团游 / 评价 / VIP 会员 / AI 智选推荐 / 取餐号 / 全站客服；三角色权限（游客 / 商家 / 管理员）。
- **AI 智能推荐**：接入 DeepSeek Chat，异常时自动降级为本地推荐算法，保证可用性。

---

## 🛠 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、Vite 6、Vue Router、Element Plus、axios、ECharts 6 |
| 后端 | Spring Boot 3.2、MyBatis-Plus、JWT、Hibernate Validator |
| 支付 | 支付宝 EasySDK 2.2.3（沙箱 wap.pay / query / 异步通知） |
| 数据库 | MySQL 8.0，版本化 SQL 迁移脚本 |
| AI | DeepSeek Chat（可选，失败自动降级本地推荐） |

---

## 📁 目录结构

```
.
├── backend/                    Spring Boot 后端
│   └── src/main/
│       ├── java/com/gxtourism/  分层源码
│       │   ├── controller/      认证、商品、订单、支付、商家、管理员、统计、会员、跟团游…
│       │   ├── service/         业务逻辑（事务、折扣、支付同步、AI 降级）
│       │   ├── entity/          数据实体
│       │   ├── mapper/          MyBatis-Plus 数据访问
│       │   ├── config/          JWT / Web / 支付 / 拦截器配置
│       │   └── common/          统一响应、异常处理、会员策略
│       └── resources/
│           ├── application.yml               主配置（密钥走环境变量）
│           ├── application-local.yml.example 本地密钥模板（请复制后填写）
│           └── sql/                          建表 / 迁移 / 种子数据
├── frontend/                   Vue 3 前端
│   ├── src/views/              首页、登录、订单、支付、商家后台、管理后台、会员、跟团游…
│   ├── src/components/         动态背景、客服、页脚、AI 智选
│   ├── src/screen/             可视化大屏
│   ├── src/router/ src/store/  路由与状态管理
│   └── public/                 静态资源（图片等）
└── docs/                       项目文档
```

---

## 🚀 快速启动

### 1. 初始化数据库

```bash
# 新建数据库 guangxi_tourism，然后依次执行：
# backend/src/main/resources/sql/init.sql
# migrate_v3.sql ~ migrate_v8.sql（v3-v7 可重复执行，v8 全新库只执行一次）
# seed_city.sql（可选，城市种子数据）
```

### 2. 配置密钥

```bash
cd backend/src/main/resources
# 复制模板并填写自己的密钥（数据库密码 / JWT 密钥 / 支付宝密钥 / DeepSeek Key）
cp application-local.yml.example application-local.yml
```

> ⚠️ 密钥切勿提交到版本库。`application-local.yml` 已在 `.gitignore` 中排除。

### 3. 启动后端（8081）

```bash
cd backend
mvn spring-boot:run
# 或在 IDEA 中直接运行 GxTourismApplication
```

### 4. 启动前端（5174）

```bash
cd frontend
npm install
npm run dev
```

### 5. 接口文档

启动后端后访问 `http://localhost:8081/swagger-ui/index.html`

---

## 👥 演示账号

| 角色 | 账号 | 说明 |
| --- | --- | --- |
| 游客 | `13800000001` | 下单、支付、评价、开通会员 |
| 商家 | 见 docs | 商品管理、订单履约、收入统计 |
| 管理员 | 见 docs | 平台运营、用户与商家管理、数据大屏 |

---

## 📌 说明

本项目为课程综合实践作品。数据库密码、JWT 密钥、支付私钥、AI Key 等敏感信息均通过环境变量或本地未跟踪文件管理，仓库中不包含任何真实密钥。

**Author：** 卢泳健
