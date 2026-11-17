# 接口清单（离线版）

在线可交互文档：启动后端后访问 <http://localhost:8081/swagger-ui/index.html>
OpenAPI JSON：<http://localhost:8081/v3/api-docs>

统一响应结构：

```json
{ "code": 200, "msg": "success", "data": ... }
```

- `code = 200` 为成功；业务失败返回 `code = 400 / 500` 与中文 `msg`。
- 需登录的接口在请求头带 `Authorization: Bearer <token>`。
- 所有金额字段为 `BigDecimal`，由后端重算，前端传入的价格一律不信任。

---

## 认证 `/api/auth`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 账号密码登录，返回 `{token, user}` |
| POST | `/register` | 注册游客账号 |
| GET | `/demo-accounts` | 全部演示账号与名下店铺清单 |

## 景点 `/api/spot`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/page` | 分页查询，支持 city / keyword / page / size |
| GET | `/{id}` | 景点详情 |
| GET | `/hot` | 热门景点 |
| GET | `/cities` | 城市列表 |

## 店铺 `/api/shop`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 店铺列表，支持 spotId / city / type |
| GET | `/{id}` | 店铺详情（含在售商品） |

## 商品 `/api/product`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/hot` | 热门商品 |
| GET | `/{id}` | 商品详情 |

## 评价 `/api/review`

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| GET | `/list` | 否 | 商品评价列表 |
| POST | `` | 是 | 发表评价（买过才能评、限次防刷） |

## 订单 `/api/order`（需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/create` | 下单，后端重算金额、扣库存、写归属与取餐号 |
| GET | `/my` | 我的订单（分页） |
| GET | `/{orderNo}` | 订单详情 |
| POST | `/{orderNo}/cancel` | 取消订单 |

## 支付 `/api/pay/alipay`

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| POST | `/create` | 是 | 生成沙箱收银台自动提交表单 HTML（默认 `alipay.trade.wap.pay`，含 `notify_url`） |
| GET | `/query/{orderNo}` | 是 | 主动查单同步状态（前端轮询兜底） |
| POST | `/notify` | **否** | 支付宝服务端异步通知：RSA2 验签 → appId/金额校验 → 条件更新幂等 → 销量/流水；返回纯文本 `success` / `failure` |

## 商家后台 `/api/merchant`（需登录 + MERCHANT 角色）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/shops` | 我的店铺 |
| POST | `/shop` | 新增店铺 |
| GET | `/shops/{shopId}/products` | 本店商品（隔离） |
| POST | `/shops/{shopId}/products` | 新增商品（同名查重） |
| PUT | `/products/{id}` | 修改商品 |
| DELETE | `/products/{id}` | 删除商品 |
| GET | `/stats` | 经营看板：营业额、销量排行 |
| GET | `/orders` | 本店订单（分页、隔离） |

## 平台管理后台 `/api/admin`（需登录 + ADMIN 角色）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/overview` | 平台概览 |
| GET / POST | `/spots` | 景点列表 / 新增 |
| PUT / DELETE | `/spots/{id}` | 修改 / 删除景点 |
| GET / POST | `/tours` | 跟团游线路列表 / 新增 |
| PUT / DELETE | `/tours/{id}` | 修改 / 删除线路 |
| GET / POST | `/shops` | 店铺列表 / 新增 |
| PUT / DELETE | `/shops/{id}` | 修改 / 删除店铺（同步商品归属） |
| GET / POST | `/users` | 用户列表 / 新增 |
| PUT / DELETE | `/users/{id}` | 修改 / 删除用户 |
| GET | `/orders` | 全站订单 |
| PUT | `/orders/{id}/status` | 改订单状态 |
| DELETE | `/orders/{id}` | 删除订单 |

> 删除均带「存在子数据不允许删」保护；非 ADMIN 访问由 `RoleInterceptor` 返回 HTTP 403（见下方「权限门禁」）。

## 会员 `/api/vip`

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| GET | `/plans` | 否 | 会员套餐 |
| GET | `/status` | 是 | 我的会员状态 |
| POST | `/create` | 是 | 开通会员下单 |
| GET | `/query/{orderNo}` | 是 | 会员订单查单 |

## 跟团游 `/api/tour`

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| GET | `/list` | 否 | 广西区内线路列表 |
| GET | `/{id}` | 否 | 线路详情 |
| GET | `/contact` | 否 | 咨询电话 / 微信 |
| POST | `/book` | 是 | 报名 |
| GET | `/my` | 是 | 我的报名 |
| POST | `/pay` | 是 | 报名支付 |
| GET | `/query/{bookingNo}` | 是 | 报名查单 |
| POST | `/cancel/{bookingNo}` | 是 | 取消报名 |

## AI 与统计

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| POST | `/api/ai/recommend` | 否 | AI 智选推荐，异常自动降级本地推荐 |
| GET | `/api/stats/screen` | 否 | **可视化大屏统计**：KPI / 门票价格 / 业态构成 / 景点点位 / 城市热度 / 客源结构 / 月度趋势，全部由 MySQL SQL 聚合 |

### `/api/stats/screen` 返回结构

```json
{
  "kpiList":      [{ "label": "平台累计订单", "value": 9, "unit": "笔", "tone": "cyan" }],
  "priceCompare": [{ "name": "遇龙河景区", "city": "阳朔", "value": 240 }],
  "typeShare":    [{ "name": "景区门票", "value": 37 }],
  "scenicSpots":  [{ "name": "...", "city": "...", "star": "5A", "price": 215, "heat": 96 }],
  "cityHeat":     [{ "name": "桂林市", "value": 12800, "spots": 46 }],
  "sourceStructure": [{ "city": "桂林", "inside": 7420, "outside": 5380 }],
  "months":       ["1月", "..."],
  "monthlyTrend": { "2026年": [4920, "..."], "2025年": [4490, "..."] },
  "meta":         { "orderCount": 9, "todayOrders": 2, "revenue": 371.5, "updatedAt": "11:34:42" }
}
```

数据口径：`kpiList / priceCompare / typeShare / scenicSpots` 来自真实业务表，下单后立即变化；`cityHeat / sourceStructure / monthlyTrend` 来自 `t_visit_stat` 客流事实表（历史 24 个月为演示种子数据），当月会叠加真实订单数。前端接口不可用时自动退回本地基线，大屏不会空白。

---

## 权限门禁（v8）

`JwtInterceptor` 负责「是否登录」，`RoleInterceptor` 负责「是不是对应角色」，两者都注册在 `WebConfig`，按注册顺序执行。

| 路径 | 登录 | 角色 | 附加条件 |
|---|---|---|---|
| `/api/discovery/**` | 否 | 无 | 公开 |
| `/api/user/**` | 是 | 任意 | — |
| `/api/merchant/**` | 是 | `MERCHANT` | `audit_status = APPROVED` |
| `/api/admin/**` | 是 | `ADMIN` | — |
| `/api/order/**`、`/api/pay/alipay/create`、`/api/vip/*`、`/api/tour/book` 等 | 是 | 任意 | — |

失败返回：无 token / token 失效 → **HTTP 401** `请先登录`；角色不符 → **HTTP 403** `当前账号无权访问该后台`；商家待审 → **403** `商家入驻审核尚未通过`。前端 `api.js` 统一处理 401（跳登录页并带 `?redirect=`）与 403（提示并退回首页），`main.js` 的 `router.beforeEach` 再做一层前端路由守卫。

## 发现接口 `/api/discovery`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/hot-search` | 热搜榜，来自 `t_browse_log` 按景点分组计数，不足补默认词 |
| GET | `/suggest?q=` | 搜索联想，景点 / 商品 / 线路名称模糊匹配 |
| GET | `/live-feed` | 最近 8 笔已支付订单，昵称脱敏 |
| GET | `/config` | 公开平台配置（公告、客服电话、会员折扣等） |
| GET | `/recommend?limit=8` | 个性化推荐 |

`recommend` 返回 `{ personalized, prefCity, title, items[] }`。打分逻辑：浏览足迹中的高频城市 +45 分，收藏过的类型 +10 分，销量/热度归一化最高 30 分，已收藏降权 −25 分，稳定微扰避免每次完全一致；无登录或无行为时降级为全局热度榜，并给每条附 `reason` 推荐理由。

## 个人中心 `/api/user`（需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET / PUT | `/profile` | 查看资料与统计 / 修改昵称、头像、手机、邮箱、主体名 |
| PUT | `/password` | 修改密码（校验原密码 MD5） |
| GET | `/favorites` | 我的收藏（带标题、图片、城市、跳转链接） |
| POST | `/favorites/toggle` | 收藏 / 取消，返回 `{ favorited }` |
| GET | `/favorites/check?itemType=&itemId=` | 查询单项是否已收藏 |
| GET / POST | `/browse` | 最近 30 条浏览足迹 / 记录一次浏览 |
| GET / POST | `/complaints` | 我的工单 / 提交投诉 |

## 商家收入 `/api/merchant`（需登录 + MERCHANT + 审核通过）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/income` | `summary`（累计/今日/客单价）+ `trend`（近 7 日）+ `byShop` + `byProduct` |

## 平台运营 `/api/admin`（需登录 + ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/merchants` | 全部商家：店铺数/商品数/订单数/营业额/审核状态 |
| PUT | `/merchants/{id}/audit` | 审核通过 / 驳回（body `{status, remark}`） |
| GET | `/customers` | 全部游客：订单数、累计消费、VIP、注册时间 |
| GET | `/customer-orders?userId=` | 指定游客订单（不传则全部游客订单） |
| GET | `/products?merchantId=&status=&keyword=` | 全部商品，带店铺/商家/景点名 |
| PUT | `/products/{id}/status?status=0\|1` | 上架 / 下架 |
| GET | `/merchant-orders?merchantId=` | 商家订单 |
| GET / PUT | `/configs`、`/configs/{key}` | 平台配置列表 / 修改 |
| GET / PUT | `/complaints`、`/complaints/{id}` | 投诉列表 / 处理与回复 |
| GET | `/logs` | 最近 100 条管理员操作日志 |
| GET | `/export?type=orders\|customers\|merchants\|products\|complaints` | CSV 文本（前端 Blob 下载） |

所有运营写操作都会写入 `admin_log`。非 ADMIN 访问以上路径一律 **HTTP 403**。

> 在线演示版 `online-demo/server.js` 实现了完全相同的接口契约（数据来自 `demo-data.json` 真实快照，写操作落在进程内存），因此本地与发布后的演示版行为一致。
