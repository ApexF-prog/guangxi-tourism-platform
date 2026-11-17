USE guangxi_tourism;

-- ============================================================
-- v6：商家归属修复 + 头像本地化 + 管理员账号
-- 执行：mysql -uroot -p -D guangxi_tourism < migrate_v6.sql
-- ============================================================

-- 1. 商品归属统一：一律以「所属店铺的 merchant_id」为唯一来源
--    历史数据里有一批商品挂在 A 商家的店铺下，merchant_id 却写成 4，
--    导致 ownedProduct 校验失败，编辑/删除报「商品不存在或无权操作」。
UPDATE product p JOIN shop s ON p.shop_id = s.id
   SET p.merchant_id = s.merchant_id;

-- 2. 历史订单归属同步修正（避免下单后订单跑到别的商家后台）
UPDATE t_order o JOIN shop s ON o.shop_id = s.id
   SET o.merchant_id = s.merchant_id;

-- 3. 头像全部改为本地图片：picsum 外链在国内经常超时，导致头像空白
UPDATE t_user SET avatar = '/images/01.jpg'                WHERE username = '13800000001';
UPDATE t_user SET avatar = '/images/02.jpg'                WHERE username = '13911112222';
UPDATE t_user SET avatar = '/images/food-zhutongfan.jpg'  WHERE username = 'merchant001';
UPDATE t_user SET avatar = '/images/drink-guihuaniang.jpg' WHERE username = 'merchant002';
UPDATE t_user SET avatar = '/images/food-laoyoufen.jpg'   WHERE username = 'merchant003';
UPDATE t_user SET avatar = '/images/drink-ningmengcha.jpg' WHERE username = 'merchant004';
UPDATE t_user SET avatar = '/images/food-haixian.jpg'     WHERE username = 'merchant005';
UPDATE t_user SET avatar = '/images/drink-yezi.jpg'       WHERE username = 'merchant006';
UPDATE t_user SET avatar = '/images/sanjiangyoucha.jpg'   WHERE username = 'merchant007';

-- 其余用户（含以后注册的）兜底，凡外链/空头像一律换成默认头像
UPDATE t_user SET avatar = '/images/avatar-default.svg'
 WHERE avatar IS NULL OR avatar = '' OR avatar LIKE 'http%';

-- 4. 平台管理员账号（角色 ADMIN，密码与演示账号一致：123456）
INSERT INTO t_user (username, password, nickname, avatar, role, vip_level)
VALUES ('admin', MD5('123456'), '平台管理员', '/images/avatar-admin.svg', 'ADMIN', 0)
ON DUPLICATE KEY UPDATE role = 'ADMIN', nickname = '平台管理员',
                        avatar = '/images/avatar-admin.svg';
