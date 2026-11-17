# 密钥与部署安全说明

## 一、密钥放在哪里

`application.yml` 只保留**环境变量占位符**，不含任何明文密钥：

```yaml
spring:
  profiles:
    include: local          # 自动加载 application-local.yml
```

真实值放在 `backend/src/main/resources/application-local.yml`，
该文件已被根目录 `.gitignore` 排除，**不会进入版本库**。
首次拉取代码后：

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
# 填写自己的 DB_PASSWORD / JWT_SECRET / AI_API_KEY / 支付宝密钥
```

需要覆盖的配置项：

| 环境变量 | 用途 |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接 |
| `JWT_SECRET` | JWT 签名密钥 |
| `AI_API_KEY` / `AI_BASE_URL` / `AI_MODEL` | DeepSeek AI 智选（可选） |
| `ALIPAY_APP_ID` / `ALIPAY_GATEWAY` | 沙箱应用与网关 |
| `ALIPAY_MERCHANT_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` | RSA2 密钥对 |
| `ALIPAY_RETURN_URL` | 支付完成浏览器跳回地址 |
| `ALIPAY_NOTIFY_URL` | **服务端异步通知地址，必须公网 HTTPS 可达** |

## 二、推送到公开仓库之前

1. 确认 `git status` 里没有 `application-local.yml`、`.env`、`*.pem`、`*.key`。
2. 执行 `git log -p -- backend/src/main/resources/application.yml` 确认历史提交里也没有明文密钥。
3. 若曾经提交过密钥，**立即到支付宝开放平台重置密钥对**，并重新生成 DeepSeek API Key；仅删除文件无法撤销泄露。
4. 沙箱应用与沙箱买家账号只用于演示，切勿配置成生产网关 `openapi.alipay.com`。

## 三、在线演示版的安全边界

公网演示链接里**不包含任何私钥**：演示版是纯静态前端 + 一份脱敏的数据库快照，支付环节为模拟收银台，不会发起真实支付宝交易，也不会写回本机 MySQL。

## 四、当前已知的工程取舍（面试可主动说明）

| 项 | 现状 | 生产环境应改为 |
|---|---|---|
| 密码存储 | MD5（无盐） | BCrypt / Argon2 |
| 令牌失效 | JWT 72 小时，退出登录仅前端清除 | 黑名单 / 刷新令牌 / 短期过期 |
| 支付回调 | 需公网 HTTPS，本地 `localhost` 不可达 | 部署时配 `ALIPAY_NOTIFY_URL` |
| 接口鉴权 | 拦截器白名单 | Spring Security + 方法级权限 |
| 并发扣库存 | 事务 + 库存扣减 | 乐观锁 / 条件更新防超卖 |

这些都是课程作品范围内有意为之的简化，在面试中主动讲清楚「现状 + 生产该怎么做」比回避更加分。
