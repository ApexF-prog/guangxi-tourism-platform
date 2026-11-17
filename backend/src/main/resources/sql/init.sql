-- =============================================================
-- 广西旅游网 数据库初始化 v2（跑一次即可）
-- 商品统一模型：门票/餐饮/饮品/团购套餐；店铺；评价(限购2次)；商家隔离
-- =============================================================
DROP DATABASE IF EXISTS guangxi_tourism;
CREATE DATABASE guangxi_tourism DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE guangxi_tourism;

-- 用户（role: CUSTOMER 客户 / MERCHANT 商家）
CREATE TABLE t_user (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(32)  NOT NULL UNIQUE,
  password    VARCHAR(64)  NOT NULL COMMENT 'MD5',
  nickname    VARCHAR(32)  NOT NULL DEFAULT '',
  avatar      VARCHAR(255) NOT NULL DEFAULT '',
  role        VARCHAR(16)  NOT NULL DEFAULT 'CUSTOMER',
  vip_level   TINYINT      NOT NULL DEFAULT 0 COMMENT '0普通 1会员',
  vip_expire  DATETIME     NULL COMMENT '会员到期时间',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '用户';

-- 景点
CREATE TABLE scenic_spot (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  city        VARCHAR(32)  NOT NULL,
  level       VARCHAR(8)   NOT NULL DEFAULT '',
  cover_img   VARCHAR(255) NOT NULL,
  price       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '门票起价',
  rating      DECIMAL(2,1) NOT NULL DEFAULT 4.5,
  tags        VARCHAR(128) NOT NULL DEFAULT '',
  address     VARCHAR(128) NOT NULL DEFAULT '',
  description TEXT,
  hot         INT NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '景点';

-- 店铺（FOOD 餐饮 / DRINK 奶茶饮品），挂靠景区，含到店指引
CREATE TABLE shop (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id  BIGINT NOT NULL COMMENT '所属商家 user id',
  name         VARCHAR(64) NOT NULL,
  type         VARCHAR(8)  NOT NULL,
  spot_id      BIGINT NOT NULL,
  address      VARCHAR(255) NOT NULL COMMENT '到店指引',
  phone        VARCHAR(20) NOT NULL DEFAULT '',
  cover_img    VARCHAR(255) NOT NULL DEFAULT '',
  description  VARCHAR(500) NOT NULL DEFAULT '',
  rating       DECIMAL(2,1) NOT NULL DEFAULT 4.5,
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1营业 0歇业',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_merchant (merchant_id)
) COMMENT '店铺';

-- 商品统一表（TICKET 门票[平台直营] / FOOD 餐饮 / DRINK 饮品 / PACKAGE 团购套餐）
CREATE TABLE product (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id    BIGINT NULL COMMENT '空=平台直营门票',
  shop_id        BIGINT NULL,
  spot_id        BIGINT NULL,
  name           VARCHAR(64) NOT NULL,
  type           VARCHAR(16) NOT NULL,
  price          DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NULL COMMENT '划线原价(套餐用)',
  cover_img      VARCHAR(255) NOT NULL DEFAULT '',
  description    VARCHAR(500) NOT NULL DEFAULT '',
  stock          INT NOT NULL DEFAULT 999,
  sales          INT NOT NULL DEFAULT 0,
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '1在售 0下架',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_shop (shop_id),
  KEY idx_type (type)
) COMMENT '商品';

-- 套餐组成
CREATE TABLE package_item (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity   INT NOT NULL DEFAULT 1,
  KEY idx_pkg (package_id)
) COMMENT '套餐组成';

-- 订单（商品模型）
CREATE TABLE t_order (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no     VARCHAR(32)  NOT NULL UNIQUE,
  user_id      BIGINT NOT NULL,
  product_id   BIGINT NOT NULL,
  product_name VARCHAR(64) NOT NULL,
  product_type VARCHAR(16) NOT NULL,
  shop_id      BIGINT NULL,
  merchant_id  BIGINT NULL,
  spot_id      BIGINT NULL,
  spot_name    VARCHAR(64) NULL,
  quantity     INT NOT NULL DEFAULT 1,
  unit_price   DECIMAL(10,2) NOT NULL,
  total        DECIMAL(10,2) NOT NULL,
  visit_date   DATE NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/CANCELED',
  pay_type     VARCHAR(16) NOT NULL DEFAULT 'ALIPAY',
  trade_no     VARCHAR(64) NOT NULL DEFAULT '',
  pick_no      VARCHAR(16) NULL COMMENT '取餐号（美食/饮品）',
  vip_price    TINYINT NOT NULL DEFAULT 0 COMMENT '1=会员价订单',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at      DATETIME NULL,
  KEY idx_user (user_id),
  KEY idx_merchant (merchant_id),
  KEY idx_status (status)
) COMMENT '订单';

-- 评价（买过才能评；每用户每商品最多 2 条）
CREATE TABLE review (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  shop_id    BIGINT NULL,
  order_no   VARCHAR(32) NOT NULL DEFAULT '',
  rating     TINYINT NOT NULL DEFAULT 5,
  content    VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id)
) COMMENT '评价';

-- 支付流水
CREATE TABLE payment_record (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no   VARCHAR(32) NOT NULL,
  trade_no   VARCHAR(64) NOT NULL DEFAULT '',
  amount     DECIMAL(10,2) NOT NULL,
  status     VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_no (order_no)
) COMMENT '支付流水';

-- ================= 种子数据 =================
INSERT INTO t_user (username, password, nickname, avatar, role) VALUES
('13800000001', 'e10adc3949ba59abbe56e057f20f883e', '演示用户',   'https://picsum.photos/seed/gxa1/100/100', 'CUSTOMER'),
('13911112222', 'e10adc3949ba59abbe56e057f20f883e', '旅行家小林', 'https://picsum.photos/seed/gxa2/100/100', 'CUSTOMER'),
('merchant001', 'e10adc3949ba59abbe56e057f20f883e', '龙脊人家老板', 'https://picsum.photos/seed/gxa3/100/100', 'MERCHANT'),
('merchant002', 'e10adc3949ba59abbe56e057f20f883e', '漓畔茶饮老板', 'https://picsum.photos/seed/gxa4/100/100', 'MERCHANT');

INSERT INTO scenic_spot (name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
('漓江景区', '桂林', '5A', '/images/03.jpg', 0.00, 4.9, '山水,竹筏,精华游', '桂林市象山区漓江码头', '桂林山水甲天下，漓江是桂林风光的精华。乘船顺流而下，两岸奇峰倒影、翠竹如带，九马画山与黄布倒影更是 20 元人民币背景图案的取景地。', 9800),
('象鼻山景区', '桂林', '4A', 'https://picsum.photos/seed/gx2/800/500', 0.00, 4.7, '地标,喀斯特,摄影', '桂林市象山区滨江路', '桂林城徽与地标，山形酷似一头站在江边伸鼻豪饮漓江水的巨象，水月洞与倒影构成"象山水月"奇观。', 8600),
('两江四湖', '桂林', '5A', '/images/01.jpg', 0.00, 4.6, '夜游,游船,城市景观', '桂林市秀峰区榕湖北路', '漓江、桃花江与杉湖、榕湖、桂湖、木龙湖构成的城市水系环城游，夜游灯光璀璨，日月双塔是最佳机位。', 7200),
('独秀峰·王城景区', '桂林', '5A', 'https://picsum.photos/seed/gx4/800/500', 100.00, 4.5, '历史,靖江王府,摩崖石刻', '桂林市秀峰区王城路', '"桂林山水甲天下"真迹题刻所在地，明代靖江王府与独秀峰合二为一，登顶可俯瞰桂林全城。', 5400),
('龙脊梯田', '龙胜', '4A', '/images/06.jpg', 80.00, 4.8, '梯田,壮寨,摄影胜地', '桂林市龙胜各族自治县龙脊镇', '从山脚盘绕至山顶、层级高达千级的壮美梯田，春如银带、夏滚绿浪、秋铺金毯，平安壮寨与金坑瑶寨各具风情。', 9100),
('遇龙河景区', '阳朔', '4A', '/images/04.jpg', 0.00, 4.8, '竹筏漂流,田园,慢生活', '桂林市阳朔县白沙镇', '有"小漓江"之称的遇龙河，竹筏漂流是阳朔最经典的体验，两岸稻田与孤峰相映，水清见底。', 8800),
('阳朔西街', '阳朔', '无', 'https://picsum.photos/seed/gx7/800/500', 0.00, 4.6, '夜生活,美食,步行街', '桂林市阳朔县西街', '1400 余年历史的中外文化交汇街区，酒吧、啤酒鱼与手工艺店铺云集，夜晚最是热闹。', 7700),
('银子岩', '阳朔', '4A', 'https://picsum.photos/seed/gx8/800/500', 65.00, 4.5, '溶洞,钟乳石,避暑', '桂林市荔浦市', '典型的喀斯特溶洞，贯穿十二座山峰，晶莹的钟乳石在灯光下如银河倾泻，"银子岩一世不缺钱"的彩头广为流传。', 6100),
('世外桃源', '阳朔', '4A', 'https://picsum.photos/seed/gx9/800/500', 60.00, 4.4, '田园,游船,民族风情', '桂林市阳朔县白沙镇', '根据陶渊明《桃花源记》意境打造的田园风光景区，乘船穿过水洞豁然开朗，感受桂北民居与侗歌壮舞。', 4900),
('青秀山', '南宁', '5A', 'https://picsum.photos/seed/gx10/800/500', 20.00, 4.7, '城市绿肺,登高,兰园', '南宁市青秀区青山路', '邕江之畔的"南宁绿肺"，龙象塔登高望远，兰园与雨林大观四季常青，是本地人周末首选。', 6800),
('广西民族博物馆', '南宁', '无', 'https://picsum.photos/seed/gx11/800/500', 0.00, 4.6, '博物馆,铜鼓,免费', '南宁市青秀区青环路', '全国收藏铜鼓最多的博物馆，外形成铜鼓造型，壮锦、瑶绣与十二世居民族文化展陈丰富。', 4500),
('北海银滩', '北海', '无', 'https://picsum.photos/seed/gx12/800/500', 0.00, 4.7, '沙滩,日落,免费', '北海市银海区银滩大道', '绵延约 24 公里的石英砂沙滩，沙白细腻、海水清澈，"滩长平、沙细白"冠绝全国，看日落的绝佳之地。', 8900),
('涠洲岛', '北海', '5A', 'https://picsum.photos/seed/gx13/800/500', 98.00, 4.8, '海岛,火山地质,潜水', '北海市海城区涠洲镇', '中国最年轻的火山岛，五彩滩的火山岩地貌、鳄鱼山灯塔与滴水丹屏的日落构成南部海岛风情。', 9500),
('北海老街', '北海', '无', 'https://picsum.photos/seed/gx14/800/500', 0.00, 4.4, '骑楼,历史,小吃', '北海市海城区珠海路', '始建于 1883 年的百年骑楼老街，中西合璧的雕花窗棂下藏着虾饼、糖水等地道北部湾小吃。', 5200),
('德天跨国瀑布', '崇左', '5A', 'https://picsum.photos/seed/gx15/800/500', 110.00, 4.8, '跨国瀑布,中越界河,观景', '崇左市大新县硕龙镇', '亚洲第一大跨国瀑布，横跨中国与越南，三级跌落气势磅礴，《花千骨》取景地让这里热度更上一层。', 9300),
('明仕田园', '崇左', '4A', 'https://picsum.photos/seed/gx16/800/500', 0.00, 4.5, '田园,竹筏,山水画廊', '崇左市大新县堪圩乡', '翠竹环抱的喀斯特田园山水，"山水画廊"名不虚传，竹筏漫游明仕河最是惬意。', 5600),
('黄姚古镇', '贺州', '5A', 'https://picsum.photos/seed/gx17/800/500', 66.00, 4.6, '古镇,明清建筑,慢生活', '贺州市昭平县黄姚镇', '发祥于宋朝的千年古镇，青石板路、亭台楼阁与古榕树交织，被誉为"梦境家园"。', 7100),
('通灵大峡谷', '百色', '4A', 'https://picsum.photos/seed/gx18/800/500', 109.00, 4.5, '峡谷,瀑布,原始森林', '百色市靖西市湖润镇', '藏于桂西南的天然峡谷秘境，188 米高的单级瀑布落差亚洲罕见，峡谷内古生代植物繁茂。', 4700),
('三娘湾', '钦州', '4A', 'https://picsum.photos/seed/gx19/800/500', 0.00, 4.3, '白海豚,渔村,赶海', '钦州市钦南区犀牛脚镇', '以"中华白海豚之乡"闻名，运气好时出海可见粉白色海豚跃出海面，渔家风情浓郁。', 4200),
('白浪滩', '防城港', '4A', 'https://picsum.photos/seed/gx20/800/500', 0.00, 4.4, '黑金沙滩,赶海,大平坡', '防城港市防城区江山半岛', '因钛矿砂呈灰黑色得名"黑金沙滩"，滩面宽阔平坦如镜，涨潮白浪滚滚，是自驾看海的热门地。', 4800),
('程阳八寨', '三江', '4A', '/images/sanjiangchengyangbazhai.jpg', 80.00, 4.6, '侗寨,风雨桥,百家宴', '柳州市三江侗族自治县林溪镇', '侗族聚居的八个寨子，程阳风雨桥是侗族木构建筑的巅峰之作，百家宴与侗族大歌不容错过。', 5000),
('百里柳江', '柳州', '无', '/images/liujiangyejing.jpg', 0.00, 4.5, '夜景,喷泉,工业城市', '柳州市柳江北岸滨江公园', '柳江如玉带穿城而过，夜景音乐喷泉高达百米，江边夜骑与夜游船是柳州人的日常浪漫。', 4600),
('骑楼城', '梧州', '4A', 'https://picsum.photos/seed/gx23/800/500', 0.00, 4.3, '骑楼,岭南文化,老城', '梧州市万秀区', '国内现存规模最大的骑楼建筑群之一，22 条老街串起百年商埠记忆，龟苓膏与纸包鸡香气满城。', 3900);

-- 平台直营门票（每个收费景点：成人票 + 学生票）
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales)
SELECT NULL, NULL, id, CONCAT(name, ' 成人票'), 'TICKET', price, price,
       cover_img, CONCAT(name, '成人门票，凭码入园'), 999, FLOOR(50 + hot/100)
FROM scenic_spot WHERE price > 0;
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales)
SELECT NULL, NULL, id, CONCAT(name, ' 学生票'), 'TICKET', ROUND(price*0.5,0), ROUND(price*0.5,0),
       cover_img, CONCAT(name, '学生优惠票（需出示学生证）'), 999, FLOOR(20 + hot/200)
FROM scenic_spot WHERE price > 0;

-- 店铺（merchant001 开两家餐饮店，merchant002 开奶茶店 —— 演示商家隔离）
INSERT INTO shop (merchant_id, name, type, spot_id, address, phone, cover_img, description, rating) VALUES
(3, '龙脊人家·壮乡竹筒饭', 'FOOD', 5, '进入龙脊梯田景区大寨门后，沿主石板路步行约 200 米，大寨停车场东侧红木招牌即是；从二号观景台下行约 10 分钟可达', '0773-7583001', '/images/tesemeishi.jpg', '壮家老灶竹筒饭，选用龙脊高山香糯米与泉水焖制，配自腊土鸡与山野菜，景区内口碑老店', 4.8),
(3, '螺蛳粉小镇·柳州直营店', 'FOOD', 22, '百里柳江景观带滨江公园东门内，音乐喷泉广场往南 150 米（看到大螺蛳雕塑即到）', '0772-3168002', '/images/liuzhouluosifen.jpg', '柳州本土直营，汤底每日凌晨用螺蛳与筒骨现熬，酸笋爽脆，嗦粉首选', 4.7),
(4, '漓畔茶饮·桂花酿', 'DRINK', 6, '阳朔遇龙河景区工农桥码头旁，竹筏下客点向南 30 米，临河玻璃房', '0773-8822003', '/images/luosifenxiaozhen.jpg', '现摘桂花与手作米酿打底，坐在河边喝一杯属于阳朔的秋天', 4.9);

-- 龙脊人家 商品（FOOD）
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales) VALUES
(3, 1, 5, '壮家竹筒饭', 'FOOD', 25.00, NULL, '/images/tesemeishi.jpg', '高山香糯米+泉水竹筒现烤，竹香四溢', 200, 486),
(3, 1, 5, '壮家土鸡汤', 'FOOD', 48.00, NULL, '/images/tesemeishi.jpg', '龙脊散养土鸡慢炖 2 小时，汤色金黄', 80, 215),
(3, 1, 5, '腊味拼盘', 'FOOD', 58.00, NULL, 'https://picsum.photos/seed/gxf3/800/500', '自腊腊肉+腊肠+腊鸭三拼', 60, 132),
(3, 1, 5, '野韭菜煎蛋', 'FOOD', 28.00, NULL, 'https://picsum.photos/seed/gxf4/800/500', '梯田埂上现摘野韭菜，土鸡蛋煎制', 100, 178);
-- 螺蛳粉店 商品
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales) VALUES
(3, 2, 22, '柳州螺蛳粉(经典)', 'FOOD', 15.00, NULL, '/images/liuzhouluosifen.jpg', '酸笋+腐竹+花生，一口上头', 300, 652),
(3, 2, 22, '鸳鸯螺蛳粉', 'FOOD', 20.00, NULL, '/images/tesemeishi1.jpg', '一半汤粉一半干捞，双倍快乐', 200, 388),
(3, 2, 22, '炒螺蛳粉', 'FOOD', 18.00, NULL, 'https://picsum.photos/seed/gxf7/800/500', '猛火快炒，锅气十足', 150, 240);
-- 奶茶店 商品
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales) VALUES
(4, 3, 6, '桂花酿奶茶', 'DRINK', 18.00, NULL, 'https://picsum.photos/seed/gxd1/800/500', '现摘桂花+手作米酿，秋日限定', 300, 520),
(4, 3, 6, '罗汉果清润茶', 'DRINK', 15.00, NULL, 'https://picsum.photos/seed/gxd2/800/500', '桂林罗汉果，甘甜润喉', 300, 344),
(4, 3, 6, '杨枝甘露', 'DRINK', 22.00, NULL, 'https://picsum.photos/seed/gxd3/800/500', '芒果+西柚+西米，经典港式', 200, 297),
(4, 3, 6, '招牌手打柠檬茶', 'DRINK', 12.00, NULL, 'https://picsum.photos/seed/gxd4/800/500', '香水柠檬现捶，解腻神器', 400, 465);

-- 团购套餐（套餐价 < 组成单买合计）
-- P1: 龙脊梯田成人票(15) + 竹筒饭(16) + 土鸡汤(17) = 153 → 118
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales) VALUES
(3, 1, 5, '【团购】龙脊梯田门票+壮乡风味套餐', 'PACKAGE', 118.00, 153.00, '/images/06.jpg', '龙脊梯田成人票 + 竹筒饭 + 土鸡汤，一票吃玩全搞定', 100, 96),
(3, 2, 22, '【团购】嗦粉快乐套餐', 'PACKAGE', 42.00, 57.00, '/images/luosifenxiaozhen.jpg', '经典螺蛳粉 + 鸳鸯粉 + 杨枝甘露，粉与甜一次满足', 100, 152);
INSERT INTO package_item (package_id, product_id, quantity) VALUES
(31, 15, 1), (31, 16, 1), (31, 17, 1),
(32, 30, 1), (32, 31, 1), (32, 35, 1);

-- 历史评价（真实购买过；每用户每商品 ≤2 条）
INSERT INTO review (user_id, product_id, shop_id, order_no, rating, content, created_at) VALUES
(1, 16, 1, 'HX0001', 5, '竹筒饭太香了！竹子的清香渗进每一粒米里，配鸡汤绝了，爬山下来吃一碗直接复活', NOW() - INTERVAL 2 DAY),
(2, 16, 1, 'HX0002', 4, '米饭软糯，就是旺季要排队等位 15 分钟，建议错峰', NOW() - INTERVAL 1 DAY),
(1, 30, 2, 'HX0003', 5, '在柳江边嗦粉是什么神仙体验！汤底鲜得眉毛掉下来，酸笋给得很大方', NOW() - INTERVAL 1 DAY),
(2, 35, 3, 'HX0004', 5, '桂花酿奶茶不齁甜，米酿的加入让口感很特别，河边喝一杯太惬意了', NOW() - INTERVAL 3 DAY),
(1, 15, 1, 'HX0005', 5, '套餐比单买划算不少，票和饭一起搞定，带爸妈玩很省心', NOW() - INTERVAL 4 DAY);

-- 历史已支付订单（让商家后台立即有销量数据）
INSERT INTO t_order (order_no, user_id, product_id, product_name, product_type, shop_id, merchant_id, spot_id, spot_name, quantity, unit_price, total, visit_date, status, pay_type, trade_no, created_at, paid_at) VALUES
('HX0001', 1, 16, '壮家竹筒饭', 'FOOD', 1, 3, 5, '龙脊梯田', 2, 25.00, 50.00, CURDATE(), 'PAID', 'ALIPAY', '2026110522001400001', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
('HX0002', 2, 16, '壮家竹筒饭', 'FOOD', 1, 3, 5, '龙脊梯田', 1, 25.00, 25.00, CURDATE(), 'PAID', 'ALIPAY', '2026110522001400002', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
('HX0003', 1, 30, '柳州螺蛳粉(经典)', 'FOOD', 2, 3, 22, '百里柳江', 2, 15.00, 30.00, CURDATE(), 'PAID', 'ALIPAY', '2026110522001400003', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
('HX0004', 2, 35, '桂花酿奶茶', 'DRINK', 3, 4, 6, '遇龙河景区', 1, 18.00, 18.00, CURDATE(), 'PAID', 'ALIPAY', '2026110522001400004', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
('HX0005', 1, 15, '【团购】龙脊梯田门票+壮乡风味套餐', 'PACKAGE', 1, 3, 5, '龙脊梯田', 1, 118.00, 118.00, CURDATE(), 'PAID', 'ALIPAY', '2026110522001400005', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY);

-- ===== 城市补充数据（南宁/柳州/北海景点、商家、店铺、商品）=====
-- 补充南宁 / 柳州 / 北海 的景点、商家、店铺与商品（城市分区完整化）

-- ===== 新增景点 =====
INSERT INTO scenic_spot (id, name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
(24, '马鞍山公园', '柳州', '3A', '/images/maanshan.jpg', 0, 4.6, '夜景,登高,机位', '柳州市鱼峰区屏山大道马鞍山', '柳州古八景之一，登顶约 15 分钟，可俯瞰柳州半岛全景，傍晚看日落与城市灯光依次亮起。', 72),
(25, '窑埠古镇', '柳州', '4A', '/images/spot-liuzhou.jpg', 0, 4.4, '古镇,夜市,江景', '柳州市城中区窑埠街', '柳江东岸的仿古街区，晚上灯一亮就很好看，小吃与手作店集中，适合散步。', 64),
(26, '柳州工业博物馆', '柳州', '无', '/images/spot-liuzhou.jpg', 0, 4.5, '工业风,博物馆,免费', '柳州市柳北区跃进路', '老厂房改造的工业博物馆，蒸汽机车、机床与老照片，工业风照片很出片。', 52),
(27, '螺蛳粉小镇', '柳州', '4A', '/images/luosifenxiaozhen.jpg', 0.00, 4.5, '美食,研学,亲子', '柳州市柳南区螺蛳粉产业园', '螺蛳粉的发源地，可参观生产线、亲手做一碗粉，闻着酸笋味了解一座城市的产业。', 61),
(28, '中山路美食街', '南宁', '无', '/images/tesemeishi.jpg', 0, 4.5, '夜市,小吃,扫街', '南宁市青秀区中山路', '南宁最有名的夜宵街，老友粉、卷筒粉、酸野、烤生蚝一条街吃到底。', 66),
(29, '方特东盟神画', '南宁', '4A', '/images/spot-nanning.jpg', 199, 4.6, '主题乐园,亲子,夜场', '南宁市青秀区民族大道延长线', '以东盟十国文化为主题的大型乐园，室内项目多，雨天也能玩，适合亲子。', 58),
(30, '邕江夜游', '南宁', '无', '/images/spot-nanning.jpg', 88, 4.4, '夜游,游船,江景', '南宁市青秀区民生码头', '乘船游邕江，两岸是南宁的城市天际线与桥梁灯光，全程约 60 分钟。', 49),
(31, '北海海底世界', '北海', '4A', '/images/spot-yintan.jpg', 166.00, 4.3, '亲子,海洋馆,雨天可选', '北海市海城区茶亭路', '海底隧道、企鹅馆与海狮表演，带小孩来北海的备选行程。', 57),
(32, '金海湾红树林', '北海', '4A', '/images/spot-yintan.jpg', 90.00, 4.5, '红树林,赶海,生态', '北海市银海区金海湾', '成片红树林与滩涂，退潮时可以跟着渔民赶海，看招潮蟹与弹涂鱼。', 54);

-- ===== 新商家账号（密码均为 123456）=====
INSERT INTO t_user (id, username, password, nickname, avatar, role) VALUES
(5, 'merchant003', 'e10adc3949ba59abbe56e057f20f883e', '老友记老板', '/images/food-laoyoufen.jpg', 'MERCHANT'),
(6, 'merchant004', 'e10adc3949ba59abbe56e057f20f883e', '邕城茶事老板', '/images/drink-ningmengcha.jpg', 'MERCHANT'),
(7, 'merchant005', 'e10adc3949ba59abbe56e057f20f883e', '北海渔港老板', '/images/food-haixian.jpg', 'MERCHANT'),
(8, 'merchant006', 'e10adc3949ba59abbe56e057f20f883e', '涠洲椰语老板', '/images/drink-yezi.jpg', 'MERCHANT'),
(9, 'merchant007', 'e10adc3949ba59abbe56e057f20f883e', '三江油茶老板', '/images/sanjiangyoucha.jpg', 'MERCHANT');

-- ===== 新增店铺 =====
INSERT INTO shop (id, merchant_id, name, type, spot_id, address, phone, cover_img, description, rating, status) VALUES
(4, 5, '老友记·南宁老友粉', 'FOOD', 28, '南宁市青秀区中山路 18 号（地铁 1 号线朝阳广场站 B 口步行 400 米）', '0771-2621888', '/images/food-laoyoufen.jpg', '酸笋豆豉爆香的老友粉，南宁人从小吃到大的早餐味道。', 4.8, 1),
(5, 6, '邕城茶事·手打柠檬茶', 'DRINK', 28, '南宁市青秀区中山路 32 号（老友记斜对面，绿色招牌）', '0771-2621999', '/images/drink-ningmengcha.jpg', '鸭屎香茶底 + 手打香水柠檬，配夜宵解腻刚好。', 4.6, 1),
(6, 7, '北海渔港·海鲜大排档', 'FOOD', 14, '北海市海城区北海老街尽头渔港码头旁（认准蓝色遮阳棚）', '0779-3881234', '/images/food-haixian.jpg', '每天渔船直供，椒盐皮皮虾和蒜蓉生蚝是招牌。', 4.7, 1),
(7, 8, '涠洲椰语·椰子冻', 'DRINK', 13, '北海市涠洲岛滴水丹屏景区入口旁（白色小木屋）', '0779-3885678', '/images/drink-yezi.jpg', '现开椰青做底，椰子冻 Q 弹，环岛骑行途中的补给站。', 4.5, 1),
(8, 9, '三江油茶铺', 'DRINK', 22, '柳州市城中区滨江路百里柳江景区游客中心旁', '0772-3168899', '/images/sanjiangyoucha.jpg', '侗族打油茶，茶叶生姜糯米现捶现煮，配米花与花生。', 4.6, 1);

-- ===== 新增商品 =====
INSERT INTO product (merchant_id, shop_id, spot_id, name, type, price, original_price, cover_img, description, stock, sales, status) VALUES
-- 门票（直营）
(NULL, NULL, 27, '螺蛳粉小镇 成人票', 'TICKET', 30, 40, '/images/luosifenxiaozhen.jpg', '含产业园区参观与一碗现煮螺蛳粉，凭票入园', 200, 321, 1),
(NULL, NULL, 29, '方特东盟神画 全天票', 'TICKET', 199, 260, '/images/spot-nanning.jpg', '含园区所有常规项目，1.1 米以下儿童免票', 300, 486, 1),
(NULL, NULL, 30, '邕江夜游 船票', 'TICKET', 88, 108, '/images/spot-nanning.jpg', '民生码头登船，全程约 60 分钟，含讲解', 150, 233, 1),
(NULL, NULL, 31, '北海海底世界 成人票', 'TICKET', 138, 168, '/images/spot-yintan.jpg', '含海底隧道、企鹅馆与海狮表演', 200, 275, 1),
(NULL, NULL, 32, '金海湾红树林 门票', 'TICKET', 60, 80, '/images/spot-yintan.jpg', '含红树林栈道与赶海体验区', 180, 164, 1),
-- 老友记
(5, 4, 28, '招牌老友粉', 'FOOD', 18, 22, '/images/food-laoyoufen.jpg', '酸笋、豆豉、辣椒爆香，汤浓粉滑，微辣开胃', 200, 512, 1),
(5, 4, 28, '卷筒粉（三条）', 'FOOD', 8, 10, '/images/food-laoyoufen.jpg', '现蒸米皮卷肉末木耳，配黄皮酱', 150, 268, 1),
(5, 4, 28, '酸野拼盘', 'FOOD', 12, 15, '/images/tesemeishi.jpg', '芒果、萝卜、黄瓜腌酸，南宁人的解腻小食', 100, 143, 1),
-- 邕城茶事
(6, 5, 28, '鸭屎香手打柠檬茶', 'DRINK', 16, 20, '/images/drink-ningmengcha.jpg', '鸭屎香茶底现打香水柠檬，少冰更清爽', 200, 388, 1),
(6, 5, 28, '芒果冰沙', 'DRINK', 18, 22, '/images/drink-yangzhiganlu.jpg', '大果芒果现打，加椰浆，甜度可选', 150, 205, 1),
-- 北海渔港
(7, 6, 14, '椒盐皮皮虾', 'FOOD', 88, 108, '/images/food-haixian.jpg', '当日渔船直供，椒盐现炒，一斤约 12 只', 80, 356, 1),
(7, 6, 14, '蒜蓉炭烤生蚝', 'FOOD', 58, 68, '/images/food-haixian.jpg', '一份 10 只，蒜蓉加小米辣，现开现烤', 120, 421, 1),
(7, 6, 14, '海鲜炒饭', 'FOOD', 32, 38, '/images/food-chaoluosifen.jpg', '虾仁、鱿鱼、瑶柱同炒，锅气足', 100, 187, 1),
-- 涠洲椰语
(8, 7, 13, '招牌椰子冻', 'DRINK', 22, 28, '/images/drink-yezi.jpg', '现开椰青做底，配芒果与椰脆', 120, 296, 1),
(8, 7, 13, '鲜榨椰青', 'DRINK', 15, 18, '/images/drink-yezi.jpg', '冰镇现开，插吸管直接喝', 200, 344, 1),
-- 三江油茶铺
(9, 8, 22, '侗家打油茶', 'DRINK', 15, 18, '/images/sanjiangyoucha.jpg', '茶叶生姜糯米现捶，配米花、花生、葱花', 150, 178, 1),
(9, 8, 22, '侗家糯米酒', 'DRINK', 20, 25, '/images/sanjiangyoucha.jpg', '自酿低度甜酒，冰镇更好喝', 80, 96, 1);

-- ================= v3：会员制 + 跟团游 =================
-- 会员开通订单
CREATE TABLE vip_order (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no   VARCHAR(32) NOT NULL UNIQUE,
  user_id    BIGINT NOT NULL,
  plan_id    BIGINT NOT NULL,
  plan_name  VARCHAR(32) NOT NULL,
  months     INT NOT NULL,
  amount     DECIMAL(10,2) NOT NULL,
  status     VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  trade_no   VARCHAR(64) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at    DATETIME NULL,
  KEY idx_user (user_id)
) COMMENT '会员开通订单';

-- 跟团游线路（只做广西：桂林/柳州/南宁/北海）
CREATE TABLE tour (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  city        VARCHAR(32) NOT NULL COMMENT '目的地城市',
  title       VARCHAR(128) NOT NULL,
  cover_img   VARCHAR(255) NOT NULL,
  days        TINYINT NOT NULL DEFAULT 1,
  departure   VARCHAR(32) NOT NULL DEFAULT '南宁',
  highlights  VARCHAR(255) NOT NULL DEFAULT '' COMMENT '亮点，逗号分隔',
  itinerary   TEXT COMMENT '行程安排',
  price       DECIMAL(10,2) NOT NULL COMMENT '成人原价',
  stock       INT NOT NULL DEFAULT 30 COMMENT '余位',
  status      TINYINT NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_city (city)
) COMMENT '跟团游线路';

-- 跟团游报名单
CREATE TABLE tour_booking (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_no    VARCHAR(32) NOT NULL UNIQUE,
  user_id       BIGINT NOT NULL,
  tour_id       BIGINT NOT NULL,
  tour_title    VARCHAR(128) NOT NULL,
  travel_date   DATE NOT NULL,
  people        INT NOT NULL DEFAULT 1,
  unit_price    DECIMAL(10,2) NOT NULL COMMENT '成交单价（会员 9 折后）',
  total         DECIMAL(10,2) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  pay_type      VARCHAR(16) NOT NULL DEFAULT 'ALIPAY',
  trade_no      VARCHAR(64) NOT NULL DEFAULT '',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at       DATETIME NULL,
  KEY idx_user (user_id)
) COMMENT '跟团游报名';

INSERT INTO tour (city, title, cover_img, days, departure, highlights, itinerary, price, stock, status) VALUES
('桂林', '漓江精华 2 日游（兴坪筏游+阳朔西街）', '/images/spot-guilin.jpg', 2, '桂林',
 '兴坪漓江,20元人民币打卡,遇龙河竹筏,阳朔西街',
 'D1 兴坪码头乘竹筏游漓江精华段，打卡 20 元人民币背景，下午阳朔西街自由活动；D2 遇龙河竹筏漂流，午后返回桂林散团。',
 599, 20, 1),
('桂林', '龙脊梯田+两江四湖 1 日游', '/images/spot-liangjiangsihu.jpg', 1, '桂林',
 '龙脊梯田,金坑索道,两江四湖,日月双塔',
 '上午车赴龙胜龙脊梯田，金坑大寨乘索道观景；下午返桂林，傍晚两江四湖游船看日月双塔夜景。',
 268, 25, 1),
('柳州', '百里柳江夜游+马鞍山 1 日游', '/images/liujiangyejing.jpg', 1, '柳州',
 '百里柳江,音乐喷泉,马鞍山全景,窑埠古镇',
 '下午马鞍山公园登顶拍柳州全景，晚上乘船夜游百里柳江，看音乐喷泉与两岸灯火，散团前逛窑埠古镇。',
 168, 30, 1),
('柳州', '三江侗寨程阳风雨桥 2 日游', '/images/sanjiangchengyangbazhai.jpg', 2, '柳州',
 '程阳永济桥,侗族大歌,百家宴,鼓楼',
 'D1 车赴三江，游程阳八寨与程阳永济桥，晚上百家宴+侗族大歌表演；D2 参观鼓楼与侗族博物馆，下午返回柳州。',
 428, 20, 1),
('南宁', '青秀山+民族博物馆 1 日游', '/images/spot-qingxiushan.jpg', 1, '南宁',
 '青秀山,龙象塔,广西民族博物馆,铜鼓',
 '上午游青秀山，登龙象塔俯瞰邕江；下午参观广西民族博物馆，看铜鼓与壮锦，傍晚中山路夜市自由觅食。',
 158, 30, 1),
('南宁', '方特东盟神画亲子 1 日游', '/images/spot-nanning.jpg', 1, '南宁',
 '方特东盟神画,十国文化,室内项目,亲子友好',
 '全天方特东盟神画自由行，含往返接送，室内项目多，雨天可玩，适合亲子家庭。',
 299, 25, 1),
('北海', '北海银滩+百年老街 2 日游', '/images/spot-beihailaojie.jpg', 2, '北海',
 '北海银滩,骑楼老街,虾饼糖水,侨港夜市',
 'D1 北海老街骑楼漫步，虾饼糖水一路吃过去，傍晚银滩看日落；D2 侨港风情街早餐后赶海体验，午后散团。',
 398, 20, 1),
('北海', '涠洲岛纯玩 3 日游（含船票上岛）', '/images/spot-weizhoudao.jpg', 3, '北海',
 '涠洲岛,火山口地质公园,滴水丹屏,五彩滩日出',
 'D1 乘船上岛，环岛骑行；D2 火山口地质公园+滴水丹屏，晚上海鲜大餐；D3 五彩滩看日出后乘船返回北海。',
 899, 15, 1);

-- v4：内容加量 —— 各城市补景点/店铺/商品，保证首页与列表页排满无空白
USE guangxi_tourism;

-- ================= 新增景点 =================
INSERT INTO scenic_spot (name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
('大龙潭公园', '柳州', '4A', '/images/spot-liuzhou.jpg', 0.00, 4.6, '喀斯特,城市公园,免费', '柳州市鱼峰区龙潭路 43 号', '雷山与龙潭山水相依，卡斯特石林与镜湖相映，是柳州人周末遛娃散步的后花园。', 88),
('融水苗寨', '柳州', '4A', '/images/rongshuimiaozhai.jpg', 50.00, 4.5, '苗寨,民族风情,梯田', '柳州市融水苗族自治县', '苗家吊脚楼群依山而建，芦笙踩堂、拦门酒、百家宴，一年四季都有苗寨节庆。', 76),
('大明山', '南宁', '4A', '/images/spot-nanning.jpg', 65.00, 4.6, '山地,云海,避暑', '南宁市武鸣区大明山风景区', '北回归线上的绿色明珠，山顶云海、日出与雾凇并称三绝，夏季平均气温仅 22℃。', 82),
('南湖公园', '南宁', '无', '/images/spot-nanning.jpg', 0.00, 4.4, '湖泊,夜跑,免费', '南宁市青秀区双拥路', '环湖 8 公里步道是南宁夜跑圣地，湖畔棕榈园与名树博览园四季皆绿。', 68),
('广西科技馆', '南宁', '无', '/images/02.jpg', 0.00, 4.5, '亲子,科普,室内', '南宁市青秀区民族大道 20 号', '四层展厅涵盖航天、机器人与VR体验，雨天遛娃首选，常设科学表演秀。', 55),
('冠头岭国家森林公园', '北海', '无', '/images/04.jpg', 0.00, 4.5, '山海,日落,免费', '北海市银海区冠头岭', '北海市区最高点，森林步道尽头就是海，是拍海上日落的最佳机位。', 71),
('侨港风情街', '北海', '无', '/images/tesemeishi.jpg', 0.00, 4.6, '美食街,糖水,越南风味', '北海市银海区侨港镇', '越南归侨聚居地，糖水、卷粉、炒螺一条街吃到扶墙，晚上十点依旧烟火升腾。', 84),
('北部湾广场', '北海', '无', '/images/spot-qiloucheng.jpg', 0.00, 4.2, '地标,夜景,免费', '北海市海城区北部湾中路', '巨型珍珠贝雕塑「南珠魂」是北海城市地标，周边商圈与老街步行可达。', 49);

-- ================= 新增店铺（每城 2 家） =================
INSERT INTO shop (merchant_id, name, type, spot_id, address, phone, cover_img, description, rating)
SELECT (SELECT id FROM t_user WHERE username = s.uname), s.name, s.type, s.spot_id, s.addr, s.phone, s.img, s.descr, s.rating
FROM (
  SELECT 'merchant002' uname, '尚水街·桂林米粉' name, 'FOOD' type, 2 spot_id, '桂林市秀峰区尚水街 12 号（象鼻山景区北门步行 500 米，认准黄底红字招牌）' addr, '0773-2233111' phone, '/images/food-jianandan.jpg' img, '老卤锅底现熬，锅烧脆皮现切，桂林本地人从小吃到大的味道' descr, 4.7 rating
  UNION ALL SELECT 'merchant003', '东西巷·桂花酒酿', 'DRINK', 4, '桂林市秀峰区东西巷历史文化街区 6 号（王城正阳门斜对面）', '0773-6677888', '/images/drink-guihuaniang.jpg', '东西巷骑楼老宅里的桂花酒酿与冰豆浆，逛完王城正好来一碗', 4.6
  UNION ALL SELECT 'merchant004', '青云美食城·柳州小吃', 'FOOD', 22, '柳州市城中区青云路青云美食城 A 区 8 号铺', '0772-2233445', '/images/liuzhouluosifen.jpg', '柳州老牌小吃集散地，螺蛳鸭脚煲、炒田螺、卷粉一站吃齐', 4.6
  UNION ALL SELECT 'merchant005', '谷埠街·鲜榨果汁铺', 'DRINK', 25, '柳州市鱼峰区谷埠街国际商城二期一层', '0772-5566778', '/images/drink-guihuaniang.jpg', '现切现榨不加水，芒果汁与百香果双拼是招牌', 4.5
  UNION ALL SELECT 'merchant006', '水街老字号·卷筒粉', 'FOOD', 28, '南宁市兴宁区水街市场旁（人民路入口左手第三家）', '0771-3344556', '/images/food-laoyoufen.jpg', '米浆现磨现蒸，黄皮酱是灵魂，南宁人从小吃到大的早餐铺', 4.7
  UNION ALL SELECT 'merchant007', '三街两巷·糖水铺', 'DRINK', 10, '南宁市兴宁区三街两巷历史文化街区 9 号', '0771-6677889', '/images/drink-yangzhiganlu.jpg', '骑楼老宅里的广式糖水，杨枝甘露与槐花粉每天现做', 4.6
  UNION ALL SELECT 'merchant002', '侨港·糖水铺', 'DRINK', 14, '北海市银海区侨港镇风情街 21 号', '0779-8899000', '/images/drink-yezi.jpg', '越南归侨配方，椰奶清补凉与糖水脆圆是招牌', 4.7
  UNION ALL SELECT 'merchant003', '外沙海鲜岛·疍家菜', 'FOOD', 12, '北海市海城区外沙海鲜岛东桥头第一排', '0779-1122334', '/images/food-haixian.jpg', '疍家渔民直供，车螺芥菜汤与沙虫汤每天限量', 4.6
) s;

-- ================= 新增商品（每家新店 2 个，凑满每城 8+ 美食/饮品） =================
INSERT INTO product (merchant_id, shop_id, name, type, price, original_price, cover_img, description, stock, sales)
SELECT (SELECT id FROM t_user WHERE username = 'merchant002'), s.shop_id, s.name, s.type, s.price, s.orig, s.img, s.descr, 999, s.sales
FROM (
  -- 桂林 尚水街米粉
  SELECT (SELECT id FROM shop WHERE name='尚水街·桂林米粉') shop_id, '桂林卤菜粉' name, 'FOOD' type, 12.00 price, 15.00 orig, '/images/food-jianandan.jpg' img, '先干拌后加汤，锅烧脆皮配酸豆角，桂林米粉的标准吃法' descr, 530 sales
  UNION ALL SELECT (SELECT id FROM shop WHERE name='尚水街·桂林米粉'), '马肉米粉（冬季限定）', 'FOOD', 22.00, 26.00, '/images/tesemeishi.jpg', '桂林传统名吃，马肉汤底鲜香不膻，冬天来一碗全身暖', 210
  UNION ALL SELECT (SELECT id FROM shop WHERE name='漓江渔火·啤酒鱼'), '剑骨鱼啤酒鱼（份）', 'FOOD', 88.00, 108.00, '/images/play-food.jpg', '漓江剑骨鱼无细刺，啤酒焖足 20 分钟，酱汁拌饭一绝', 380
  UNION ALL SELECT (SELECT id FROM shop WHERE name='漓江渔火·啤酒鱼'), '田螺酿', 'FOOD', 32.00, 38.00, '/images/food-yaocai.jpg', '田螺肉与猪肉糜酿回壳中，阳朔名菜，一口一个停不下来', 260
  -- 柳州 青云美食城 / 谷埠街
  UNION ALL SELECT (SELECT id FROM shop WHERE name='青云美食城·柳州小吃'), '螺蛳鸭脚煲（份）', 'FOOD', 68.00, 78.00, '/images/food-chaoluosifen.jpg', '鸭脚炸到起虎皮再进螺蛳汤煲，配芋头与鹌鹑蛋，柳州宵夜之王', 420
  UNION ALL SELECT (SELECT id FROM shop WHERE name='青云美食城·柳州小吃'), '柳州卷粉（三条）', 'FOOD', 10.00, 12.00, '/images/food-jianandan.jpg', '米皮薄如纸，肉馅饱满，淋上黄皮酱与辣椒水', 350
  UNION ALL SELECT (SELECT id FROM shop WHERE name='谷埠街·鲜榨果汁铺'), '现榨芒果汁', 'DRINK', 13.00, 16.00, '/images/drink-ningmengcha.jpg', '台农芒果现切现榨不加一滴水，浓稠到插吸管能立住', 300
  UNION ALL SELECT (SELECT id FROM shop WHERE name='谷埠街·鲜榨果汁铺'), '百香果双拼果汁', 'DRINK', 12.00, 15.00, '/images/drink-yangzhiganlu.jpg', '百香果+橙子双拼，酸甜解腻，逛吃百里柳江必备', 280
  -- 南宁 水街 / 三街两巷
  UNION ALL SELECT (SELECT id FROM shop WHERE name='水街老字号·卷筒粉'), '水街卷筒粉（三条）', 'FOOD', 9.00, 11.00, '/images/food-laoyoufen.jpg', '现磨米浆现蒸，肉末木耳馅，黄皮酱多加一勺', 460
  UNION ALL SELECT (SELECT id FROM shop WHERE name='水街老字号·卷筒粉'), '生榨米粉', 'FOOD', 11.00, 13.00, '/images/tesemeishi.jpg', '米浆发酵微酸是精髓，配紫苏与碎肉臊，老南宁的乡愁', 310
  UNION ALL SELECT (SELECT id FROM shop WHERE name='三街两巷·糖水铺'), '槐花粉糖水', 'DRINK', 10.00, 12.00, '/images/drink-yangzhiganlu.jpg', '米浆漏成小鱼状，红糖水冰镇，南宁夏天的限定快乐', 390
  UNION ALL SELECT (SELECT id FROM shop WHERE name='三街两巷·糖水铺'), '椰奶清补凉', 'DRINK', 14.00, 16.00, '/images/drink-yezi.jpg', '十几种料码进椰奶里，一碗顶半顿饭', 340
  -- 北海 侨港糖水铺 / 外沙海鲜岛
  UNION ALL SELECT (SELECT id FROM shop WHERE name='侨港·糖水铺'), '椰奶清补凉', 'DRINK', 12.00, 15.00, '/images/drink-yezi.jpg', '侨港越南归侨配方，椰奶+绿豆+芋圆+西瓜粒', 410
  UNION ALL SELECT (SELECT id FROM shop WHERE name='侨港·糖水铺'), '糖水脆圆', 'DRINK', 10.00, 12.00, '/images/drink-guihuaniang.jpg', '芋圆现搓现煮，姜糖水打底，冬天点热的更舒服', 280
  UNION ALL SELECT (SELECT id FROM shop WHERE name='外沙海鲜岛·疍家菜'), '车螺芥菜汤', 'FOOD', 28.00, 32.00, '/images/food-tujitang.jpg', '外沙港车螺吐净泥沙，与芥菜同滚，汤鲜到眉毛掉', 240
  UNION ALL SELECT (SELECT id FROM shop WHERE name='外沙海鲜岛·疍家菜'), '白灼沙虫（份）', 'FOOD', 58.00, 68.00, '/images/food-haixian.jpg', '北海特产沙虫白灼最能吃出鲜甜，蘸芥末酱油', 190
) s;
-- ================= v5：跟团游每城补 1 条（共 12 条，3 列整排无空白） =================
INSERT INTO tour (city, title, cover_img, days, departure, highlights, itinerary, price, stock, status) VALUES
('桂林', '遇龙河竹筏+十里画廊 1 日游', '/images/spot-yulonghe.jpg', 1, '桂林',
 '遇龙河竹筏,十里画廊,月亮山,大榕树',
 '早上车赴阳朔，遇龙河人工竹筏漂流（双人筏），下午骑行或电瓶车游十里画廊，打卡月亮山与大榕树，傍晚返回桂林。全程纯玩无购物。',
 258, 25, 1),
('柳州', '融水苗寨风情+双龙沟 1 日游', '/images/rongshuimiaozhai.jpg', 1, '柳州',
 '融水苗寨,芦笙踩堂,双龙沟玻璃桥,苗家油茶',
 '早上车赴融水苗寨，看芦笙踩堂迎宾表演，中餐苗家油茶宴；下午游双龙沟原始森林，挑战玻璃悬索桥，傍晚返回柳州。',
 228, 25, 1),
('南宁', '大明山养生+伊岭岩 1 日游', '/images/spot-nanning.jpg', 1, '南宁',
 '大明山,天然氧吧,伊岭岩溶洞,壮族风情',
 '上午登大明山天然氧吧步道（含景交车），山巅观云海；下午探伊岭岩喀斯特溶洞，感受壮族迎客礼，傍晚返回南宁市区。',
 198, 30, 1),
('北海', '金海湾红树林+赶海体验 1 日游', '/images/spot-sanniangwan.jpg', 1, '北海',
 '金海湾红树林,疍家赶海,挖沙虫,海鲜餐',
 '上午游金海湾红树林生态区，疍家阿姐带赶海挖沙虫拾贝；中餐海鲜餐（含赶海所得加工）；下午自由活动后返回市区散团。',
 188, 30, 1);

-- ============================================================
-- 价格校真补丁（2026-11-08，与运行库同步）
-- 上方 INSERT 中的门票由 scenic_spot 旧价生成，本段按 2026 年公示价统一校准。
-- 免费景区 price=0，项目票另付、自拟线路/团购为演示参考价；本段可重复执行。
-- ============================================================

UPDATE scenic_spot SET price = CASE name
  WHEN '漓江景区' THEN 0.00
  WHEN '象鼻山景区' THEN 0.00
  WHEN '两江四湖' THEN 0.00
  WHEN '独秀峰·王城景区' THEN 100.00
  WHEN '龙脊梯田' THEN 80.00
  WHEN '遇龙河景区' THEN 0.00
  WHEN '阳朔西街' THEN 0.00
  WHEN '银子岩' THEN 65.00
  WHEN '世外桃源' THEN 60.00
  WHEN '青秀山' THEN 20.00
  WHEN '广西民族博物馆' THEN 0.00
  WHEN '北海银滩' THEN 0.00
  WHEN '涠洲岛' THEN 98.00
  WHEN '北海老街' THEN 0.00
  WHEN '德天跨国瀑布' THEN 110.00
  WHEN '明仕田园' THEN 0.00
  WHEN '黄姚古镇' THEN 66.00
  WHEN '通灵大峡谷' THEN 109.00
  WHEN '三娘湾' THEN 0.00
  WHEN '白浪滩' THEN 0.00
  WHEN '程阳八寨' THEN 80.00
  WHEN '百里柳江' THEN 0.00
  WHEN '骑楼城' THEN 0.00
  WHEN '马鞍山公园' THEN 0.00
  WHEN '窑埠古镇' THEN 0.00
  WHEN '柳州工业博物馆' THEN 0.00
  WHEN '螺蛳粉小镇' THEN 0.00
  WHEN '中山路美食街' THEN 0.00
  WHEN '方特东盟神画' THEN 199.00
  WHEN '邕江夜游' THEN 88.00
  WHEN '北海海底世界' THEN 166.00
  WHEN '金海湾红树林' THEN 90.00
  WHEN '大龙潭公园' THEN 0.00
  WHEN '融水苗寨' THEN 50.00
  WHEN '大明山' THEN 65.00
  WHEN '南湖公园' THEN 0.00
  WHEN '广西科技馆' THEN 0.00
  WHEN '冠头岭国家森林公园' THEN 0.00
  WHEN '侨港风情街' THEN 0.00
  WHEN '北部湾广场' THEN 0.00
  WHEN '芦笛岩' THEN 88.00
  WHEN '七星景区' THEN 55.00
  WHEN '大圩古镇' THEN 0.00
  ELSE price END;

UPDATE tour SET price = CASE title
  WHEN '漓江精华 2 日游（兴坪筏游+阳朔西街）' THEN 498.00
  WHEN '龙脊梯田+两江四湖 1 日游' THEN 238.00
  WHEN '百里柳江夜游+马鞍山 1 日游' THEN 138.00
  WHEN '三江侗寨程阳风雨桥 2 日游' THEN 388.00
  WHEN '青秀山+民族博物馆 1 日游' THEN 128.00
  WHEN '方特东盟神画亲子 1 日游' THEN 268.00
  WHEN '北海银滩+百年老街 2 日游' THEN 358.00
  WHEN '涠洲岛纯玩 3 日游（含船票上岛）' THEN 788.00
  WHEN '遇龙河竹筏+十里画廊 1 日游' THEN 228.00
  WHEN '融水苗寨风情+双龙沟 1 日游' THEN 198.00
  WHEN '大明山养生+伊岭岩 1 日游' THEN 178.00
  WHEN '金海湾红树林+赶海体验 1 日游' THEN 168.00
  ELSE price END;

UPDATE product SET price = CASE name
  WHEN '漓江景区 成人票' THEN 215.00
  WHEN '象鼻山景区 成人票' THEN 0.00
  WHEN '两江四湖 成人票' THEN 210.00
  WHEN '独秀峰·王城景区 成人票' THEN 100.00
  WHEN '龙脊梯田 成人票' THEN 80.00
  WHEN '遇龙河景区 成人票' THEN 140.00
  WHEN '银子岩 成人票' THEN 65.00
  WHEN '世外桃源 成人票' THEN 60.00
  WHEN '青秀山 成人票' THEN 20.00
  WHEN '涠洲岛 成人票' THEN 98.00
  WHEN '德天跨国瀑布 成人票' THEN 110.00
  WHEN '明仕田园 成人票' THEN 110.00
  WHEN '黄姚古镇 成人票' THEN 66.00
  WHEN '通灵大峡谷 成人票' THEN 109.00
  WHEN '三娘湾 成人票' THEN 138.00
  WHEN '程阳八寨 成人票' THEN 80.00
  WHEN '漓江景区 学生票' THEN 108.00
  WHEN '象鼻山景区 学生票' THEN 0.00
  WHEN '两江四湖 学生票' THEN 105.00
  WHEN '独秀峰·王城景区 学生票' THEN 50.00
  WHEN '龙脊梯田 学生票' THEN 40.00
  WHEN '遇龙河景区 学生票' THEN 120.00
  WHEN '银子岩 学生票' THEN 33.00
  WHEN '世外桃源 学生票' THEN 30.00
  WHEN '青秀山 学生票' THEN 10.00
  WHEN '涠洲岛 学生票' THEN 49.00
  WHEN '德天跨国瀑布 学生票' THEN 75.00
  WHEN '明仕田园 学生票' THEN 60.00
  WHEN '黄姚古镇 学生票' THEN 33.00
  WHEN '通灵大峡谷 学生票' THEN 55.00
  WHEN '三娘湾 学生票' THEN 60.00
  WHEN '程阳八寨 学生票' THEN 40.00
  WHEN '壮家竹筒饭' THEN 20.00
  WHEN '壮家土鸡汤' THEN 48.00
  WHEN '腊味拼盘' THEN 58.00
  WHEN '野韭菜煎蛋' THEN 28.00
  WHEN '柳州螺蛳粉(经典)' THEN 15.00
  WHEN '鸳鸯螺蛳粉' THEN 20.00
  WHEN '炒螺蛳粉' THEN 18.00
  WHEN '桂花酿奶茶' THEN 18.00
  WHEN '罗汉果清润茶' THEN 15.00
  WHEN '杨枝甘露' THEN 22.00
  WHEN '招牌手打柠檬茶' THEN 12.00
  WHEN '【团购】龙脊梯田门票+壮乡风味套餐' THEN 118.00
  WHEN '【团购】嗦粉快乐套餐' THEN 42.00
  WHEN '螺蛳粉小镇 成人票' THEN 0.00
  WHEN '方特东盟神画 全天票' THEN 199.00
  WHEN '邕江夜游 船票' THEN 88.00
  WHEN '北海海底世界 成人票' THEN 166.00
  WHEN '金海湾红树林 门票' THEN 90.00
  WHEN '招牌老友粉' THEN 18.00
  WHEN '卷筒粉（三条）' THEN 8.00
  WHEN '酸野拼盘' THEN 12.00
  WHEN '鸭屎香手打柠檬茶' THEN 16.00
  WHEN '芒果冰沙' THEN 18.00
  WHEN '椒盐皮皮虾' THEN 88.00
  WHEN '蒜蓉炭烤生蚝' THEN 58.00
  WHEN '海鲜炒饭' THEN 32.00
  WHEN '招牌椰子冻' THEN 22.00
  WHEN '鲜榨椰青' THEN 15.00
  WHEN '侗家打油茶' THEN 15.00
  WHEN '侗家糯米酒' THEN 20.00
  WHEN '桂林卤菜粉' THEN 12.00
  WHEN '马肉米粉（冬季限定）' THEN 22.00
  WHEN '剑骨鱼啤酒鱼（份）' THEN 88.00
  WHEN '田螺酿' THEN 32.00
  WHEN '螺蛳鸭脚煲（份）' THEN 68.00
  WHEN '柳州卷粉（三条）' THEN 10.00
  WHEN '现榨芒果汁' THEN 13.00
  WHEN '百香果双拼果汁' THEN 12.00
  WHEN '水街卷筒粉（三条）' THEN 9.00
  WHEN '生榨米粉' THEN 11.00
  WHEN '槐花粉糖水' THEN 10.00
  WHEN '椰奶清补凉' THEN 14.00
  WHEN '椰奶清补凉' THEN 12.00
  WHEN '糖水脆圆' THEN 10.00
  WHEN '车螺芥菜汤' THEN 28.00
  WHEN '白灼沙虫（份）' THEN 58.00
  WHEN '桂花酒酿小圆子' THEN 12.00
  WHEN '冰镇鲜豆浆' THEN 8.00
  ELSE price END,
  original_price = CASE name
  WHEN '漓江景区 成人票' THEN 215.00
  WHEN '象鼻山景区 成人票' THEN 0.00
  WHEN '两江四湖 成人票' THEN 210.00
  WHEN '独秀峰·王城景区 成人票' THEN 100.00
  WHEN '龙脊梯田 成人票' THEN 80.00
  WHEN '遇龙河景区 成人票' THEN 140.00
  WHEN '银子岩 成人票' THEN 65.00
  WHEN '世外桃源 成人票' THEN 60.00
  WHEN '青秀山 成人票' THEN 20.00
  WHEN '涠洲岛 成人票' THEN 98.00
  WHEN '德天跨国瀑布 成人票' THEN 110.00
  WHEN '明仕田园 成人票' THEN 110.00
  WHEN '黄姚古镇 成人票' THEN 66.00
  WHEN '通灵大峡谷 成人票' THEN 109.00
  WHEN '三娘湾 成人票' THEN 138.00
  WHEN '程阳八寨 成人票' THEN 80.00
  WHEN '漓江景区 学生票' THEN 108.00
  WHEN '象鼻山景区 学生票' THEN 0.00
  WHEN '两江四湖 学生票' THEN 105.00
  WHEN '独秀峰·王城景区 学生票' THEN 50.00
  WHEN '龙脊梯田 学生票' THEN 40.00
  WHEN '遇龙河景区 学生票' THEN 120.00
  WHEN '银子岩 学生票' THEN 33.00
  WHEN '世外桃源 学生票' THEN 30.00
  WHEN '青秀山 学生票' THEN 10.00
  WHEN '涠洲岛 学生票' THEN 49.00
  WHEN '德天跨国瀑布 学生票' THEN 75.00
  WHEN '明仕田园 学生票' THEN 60.00
  WHEN '黄姚古镇 学生票' THEN 33.00
  WHEN '通灵大峡谷 学生票' THEN 55.00
  WHEN '三娘湾 学生票' THEN 60.00
  WHEN '程阳八寨 学生票' THEN 40.00
  WHEN '壮家竹筒饭' THEN NULL
  WHEN '壮家土鸡汤' THEN NULL
  WHEN '腊味拼盘' THEN NULL
  WHEN '野韭菜煎蛋' THEN NULL
  WHEN '柳州螺蛳粉(经典)' THEN NULL
  WHEN '鸳鸯螺蛳粉' THEN NULL
  WHEN '炒螺蛳粉' THEN NULL
  WHEN '桂花酿奶茶' THEN NULL
  WHEN '罗汉果清润茶' THEN NULL
  WHEN '杨枝甘露' THEN NULL
  WHEN '招牌手打柠檬茶' THEN NULL
  WHEN '【团购】龙脊梯田门票+壮乡风味套餐' THEN 153.00
  WHEN '【团购】嗦粉快乐套餐' THEN 57.00
  WHEN '螺蛳粉小镇 成人票' THEN 0.00
  WHEN '方特东盟神画 全天票' THEN 260.00
  WHEN '邕江夜游 船票' THEN 108.00
  WHEN '北海海底世界 成人票' THEN 166.00
  WHEN '金海湾红树林 门票' THEN NULL
  WHEN '招牌老友粉' THEN 22.00
  WHEN '卷筒粉（三条）' THEN 10.00
  WHEN '酸野拼盘' THEN 15.00
  WHEN '鸭屎香手打柠檬茶' THEN 20.00
  WHEN '芒果冰沙' THEN 22.00
  WHEN '椒盐皮皮虾' THEN 108.00
  WHEN '蒜蓉炭烤生蚝' THEN 68.00
  WHEN '海鲜炒饭' THEN 38.00
  WHEN '招牌椰子冻' THEN 28.00
  WHEN '鲜榨椰青' THEN 18.00
  WHEN '侗家打油茶' THEN 18.00
  WHEN '侗家糯米酒' THEN 25.00
  WHEN '桂林卤菜粉' THEN 15.00
  WHEN '马肉米粉（冬季限定）' THEN 26.00
  WHEN '剑骨鱼啤酒鱼（份）' THEN 108.00
  WHEN '田螺酿' THEN 38.00
  WHEN '螺蛳鸭脚煲（份）' THEN 78.00
  WHEN '柳州卷粉（三条）' THEN 12.00
  WHEN '现榨芒果汁' THEN 16.00
  WHEN '百香果双拼果汁' THEN 15.00
  WHEN '水街卷筒粉（三条）' THEN 11.00
  WHEN '生榨米粉' THEN 13.00
  WHEN '槐花粉糖水' THEN 12.00
  WHEN '椰奶清补凉' THEN 16.00
  WHEN '椰奶清补凉' THEN 15.00
  WHEN '糖水脆圆' THEN 12.00
  WHEN '车螺芥菜汤' THEN 32.00
  WHEN '白灼沙虫（份）' THEN 68.00
  WHEN '桂花酒酿小圆子' THEN 15.00
  WHEN '冰镇鲜豆浆' THEN 10.00
  ELSE original_price END;

-- 同名商品「椰奶清补凉」分属两家店铺，CASE 会把两条合并成同一价，按 shop_id 校正回来
UPDATE product SET price = 12.00, original_price = 15.00
  WHERE name = '椰奶清补凉' AND shop_id = 15;
