-- v4：内容加量 —— 各城市补景点/店铺/商品，保证首页与列表页排满无空白
USE guangxi_tourism;

-- ================= 新增景点 =================
INSERT INTO scenic_spot (name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
('大龙潭公园', '柳州', '4A', '/images/spot-liuzhou.jpg', 0.00, 4.6, '喀斯特,城市公园,免费', '柳州市鱼峰区龙潭路 43 号', '雷山与龙潭山水相依，卡斯特石林与镜湖相映，是柳州人周末遛娃散步的后花园。', 88),
('融水苗寨', '柳州', '4A', '/images/rongshuimiaozhai.jpg', 60.00, 4.5, '苗寨,民族风情,梯田', '柳州市融水苗族自治县', '苗家吊脚楼群依山而建，芦笙踩堂、拦门酒、百家宴，一年四季都有苗寨节庆。', 76),
('大明山', '南宁', '4A', '/images/spot-nanning.jpg', 128.00, 4.6, '山地,云海,避暑', '南宁市武鸣区大明山风景区', '北回归线上的绿色明珠，山顶云海、日出与雾凇并称三绝，夏季平均气温仅 22℃。', 82),
('南湖公园', '南宁', '无', '/images/spot-nanning.jpg', 0.00, 4.4, '湖泊,夜跑,免费', '南宁市青秀区双拥路', '环湖 8 公里步道是南宁夜跑圣地，湖畔棕榈园与名树博览园四季皆绿。', 68),
('广西科技馆', '南宁', '无', '/images/02.jpg', 30.00, 4.5, '亲子,科普,室内', '南宁市青秀区民族大道 20 号', '四层展厅涵盖航天、机器人与VR体验，雨天遛娃首选，常设科学表演秀。', 55),
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

-- 补 3 个桂林景点，凑满 12 个（每页 8 × 4 列网格无空白）
INSERT INTO scenic_spot (name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
('芦笛岩', '桂林', '4A', '/images/spot-yinziyan.jpg', 90.00, 4.6, '溶洞,国宾洞,雨天可玩', '桂林市秀峰区芦笛路 1 号', '「国宾洞」大自然的艺术之宫，钟乳石在灯光下如梦似幻，全程室内不惧下雨。', 66),
('七星景区', '桂林', '4A', '/images/spot-guilin.jpg', 55.00, 4.4, '城市公园,七星岩,骆驼山', '桂林市七星区七星路 1 号', '漓江东岸的城市大公园，骆驼山、花桥与七星岩溶洞一次逛完。', 58),
('大圩古镇', '桂林', '无', '/images/spot-qiloucheng.jpg', 0.00, 4.3, '古镇,免费,漓江边', '桂林市灵川县大圩镇', '千年古镇曾是广西四大圩镇之首，青石板街沿漓江而建，安静少人。', 47);

-- 桂林店铺配比修正：shop10 改为饮品店，啤酒鱼/田螺酿挪至龙脊人家，新增 2 个饮品
UPDATE shop SET type='DRINK', name='东西巷·桂花酒酿', cover_img='/images/drink-guihuaniang.jpg', description='东西巷骑楼老宅里的桂花酒酿与冰豆浆，逛完王城正好来一碗' WHERE name='漓江渔火·啤酒鱼';
UPDATE product SET shop_id=(SELECT id FROM shop WHERE name='龙脊人家·壮乡竹筒饭'), spot_id=5 WHERE name IN ('剑骨鱼啤酒鱼（份）','田螺酿');
INSERT INTO product (merchant_id, shop_id, name, type, price, original_price, cover_img, description, stock, sales)
SELECT merchant_id, id, '桂花酒酿小圆子', 'DRINK', 12.00, 15.00, '/images/drink-guihuaniang.jpg', '桂花蜜酒酿配现搓小圆子，桂林人下午茶的老味道', 999, 320 FROM shop WHERE name='东西巷·桂花酒酿';
INSERT INTO product (merchant_id, shop_id, name, type, price, original_price, cover_img, description, stock, sales)
SELECT merchant_id, id, '冰镇鲜豆浆', 'DRINK', 8.00, 10.00, '/images/drink-yezi.jpg', '当天现磨现煮，冰镇后豆香更浓，两块钱加料区随便加', 999, 260 FROM shop WHERE name='东西巷·桂花酒酿';
