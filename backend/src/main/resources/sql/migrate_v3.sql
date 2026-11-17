USE guangxi_tourism;
ALTER TABLE t_user
  ADD COLUMN vip_level TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1会员',
  ADD COLUMN vip_expire DATETIME NULL COMMENT '会员到期时间';
ALTER TABLE t_order
  ADD COLUMN vip_price TINYINT NOT NULL DEFAULT 0 COMMENT '1=会员价订单';
CREATE TABLE IF NOT EXISTS vip_order (
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
CREATE TABLE IF NOT EXISTS tour (
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
CREATE TABLE IF NOT EXISTS tour_booking (
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
SHOW TABLES;
