-- =============================================================
-- 广西旅游网 v8：三角色权限治理 / 管理员运营后台 / 个性化玩法
-- 前置：init.sql + migrate_v3 ~ migrate_v7；本文件只需执行一次
--   mysql -uroot -p -D guangxi_tourism < migrate_v8.sql
-- =============================================================
USE guangxi_tourism;

-- 1) 用户表：商家入驻审核状态 + 主体名称
ALTER TABLE t_user
  ADD COLUMN audit_status  VARCHAR(16)  NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING待审 / APPROVED通过 / REJECTED驳回',
  ADD COLUMN audit_remark  VARCHAR(255) NOT NULL DEFAULT '' COMMENT '审核备注',
  ADD COLUMN merchant_name VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '商家主体/品牌名',
  ADD COLUMN phone         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '联系电话',
  ADD COLUMN email         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '联系邮箱';

-- 已有商家一律视为审核通过，保证原有 7 个商家账号照常登录使用
UPDATE t_user SET audit_status = 'APPROVED' WHERE role = 'MERCHANT';
UPDATE t_user SET merchant_name = CONCAT(nickname, ' · 广西旗舰店')
  WHERE role = 'MERCHANT' AND merchant_name = '';

-- 2) 平台配置（管理员可在线修改）
CREATE TABLE IF NOT EXISTS sys_config (
  config_key   VARCHAR(64) PRIMARY KEY,
  config_value VARCHAR(500) NOT NULL DEFAULT '',
  description  VARCHAR(200) NOT NULL DEFAULT '',
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '平台配置';

INSERT INTO sys_config (config_key, config_value, description) VALUES
  ('platform_name',   '广西旅游网',            '平台名称'),
  ('service_phone',   '19175657645',          '客服咨询电话'),
  ('service_wechat',  'waxrl0911',            '客服微信号'),
  ('vip_discount',    '0.9',                  '会员折扣（0.9 = 九折）'),
  ('site_notice',     '欢迎来到广西旅游网，四城美景一网打尽！', '全站公告'),
  ('order_cancel_min','30',                   '未支付订单自动取消分钟数'),
  ('audit_switch',    'ON',                   '商家入驻审核开关 ON/OFF')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 3) 投诉工单（游客提交 → 管理员处理）
CREATE TABLE IF NOT EXISTS complaint (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  user_name   VARCHAR(64) NOT NULL DEFAULT '',
  order_no    VARCHAR(32) NOT NULL DEFAULT '',
  target_type VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT 'SHOP / PRODUCT / TOUR / PLATFORM',
  target_name VARCHAR(128) NOT NULL DEFAULT '',
  category    VARCHAR(32) NOT NULL DEFAULT '其他',
  content     VARCHAR(1000) NOT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / PROCESSING / RESOLVED / REJECTED',
  reply       VARCHAR(1000) NOT NULL DEFAULT '',
  handler_id  BIGINT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at  DATETIME NULL,
  KEY idx_complaint_user (user_id),
  KEY idx_complaint_status (status)
) COMMENT '投诉工单';

-- 4) 收藏 / 想去
CREATE TABLE IF NOT EXISTS favorite (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  item_type  VARCHAR(16) NOT NULL COMMENT 'SPOT / PRODUCT / TOUR / SHOP',
  item_id    BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fav (user_id, item_type, item_id)
) COMMENT '收藏 / 想去';

-- 5) 浏览足迹（个性化推荐与热搜统计的数据源）
CREATE TABLE IF NOT EXISTS t_browse_log (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  item_type  VARCHAR(16) NOT NULL,
  item_id    BIGINT NOT NULL,
  city       VARCHAR(32) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_browse_user (user_id),
  KEY idx_browse_item (item_type, item_id)
) COMMENT '浏览足迹';

-- 6) 管理员操作日志（可审计）
CREATE TABLE IF NOT EXISTS admin_log (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id   BIGINT NOT NULL,
  admin_name VARCHAR(64) NOT NULL DEFAULT '',
  action     VARCHAR(64) NOT NULL,
  detail     VARCHAR(500) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_log_time (created_at)
) COMMENT '管理员操作日志';

-- 7) 演示种子：投诉、收藏、浏览足迹（用子查询取真实游客 id，避免写死）
INSERT INTO complaint (user_id, user_name, order_no, target_type, target_name, category, content, status, reply)
SELECT u.id, u.nickname, '', 'PLATFORM', '平台客服', '服务咨询',
       '想咨询一下跟团游出发前能否改期，需要提前几天说明？', 'RESOLVED', '出行日前 3 天可免费改期，已邮件回复您。'
FROM t_user u WHERE u.role = 'CUSTOMER' ORDER BY u.id LIMIT 1;

INSERT INTO complaint (user_id, user_name, order_no, target_type, target_name, category, content, status)
SELECT u.id, u.nickname, '', 'SHOP', '龙脊人家', '商品问题',
       '上次买的竹筒饭分量很足，希望上架一人份的小份装。', 'PENDING'
FROM t_user u WHERE u.role = 'CUSTOMER' ORDER BY u.id LIMIT 1;

INSERT INTO complaint (user_id, user_name, order_no, target_type, target_name, category, content, status, reply)
SELECT u.id, u.nickname, '', 'TOUR', '桂林山水三日游', '退款',
       '咨询一下成团失败的退款到账时间。', 'PROCESSING', '已受理，正在与计调核实。'
FROM t_user u WHERE u.role = 'CUSTOMER' ORDER BY u.id DESC LIMIT 1;

INSERT IGNORE INTO favorite (user_id, item_type, item_id)
SELECT u.id, 'SPOT', s.id FROM t_user u
JOIN scenic_spot s ON s.city = '桂林'
WHERE u.role = 'CUSTOMER' ORDER BY u.id, s.id LIMIT 3;

INSERT IGNORE INTO favorite (user_id, item_type, item_id)
SELECT u.id, 'PRODUCT', p.id FROM t_user u
JOIN product p ON p.type = 'TICKET'
WHERE u.role = 'CUSTOMER' ORDER BY u.id, p.id LIMIT 2;

INSERT INTO t_browse_log (user_id, item_type, item_id, city)
SELECT u.id, 'SPOT', s.id, s.city FROM t_user u
JOIN scenic_spot s ON s.city IN ('桂林', '南宁', '北海', '柳州')
WHERE u.role = 'CUSTOMER' ORDER BY u.id, s.id LIMIT 12;

-- 8) 给全部商品补齐默认状态，保证管理员「上架/下架」有明确初值
UPDATE product SET status = 1 WHERE status IS NULL;
