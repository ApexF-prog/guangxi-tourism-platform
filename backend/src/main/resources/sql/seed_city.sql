-- 补充南宁 / 柳州 / 北海 的景点、商家、店铺与商品（城市分区完整化）
USE guangxi_tourism;

-- ===== 新增景点 =====
INSERT INTO scenic_spot (id, name, city, level, cover_img, price, rating, tags, address, description, hot) VALUES
(24, '马鞍山公园', '柳州', '3A', '/images/maanshan.jpg', 0, 4.6, '夜景,登高,机位', '柳州市鱼峰区屏山大道马鞍山', '柳州古八景之一，登顶约 15 分钟，可俯瞰柳州半岛全景，傍晚看日落与城市灯光依次亮起。', 72),
(25, '窑埠古镇', '柳州', '4A', '/images/spot-liuzhou.jpg', 0, 4.4, '古镇,夜市,江景', '柳州市城中区窑埠街', '柳江东岸的仿古街区，晚上灯一亮就很好看，小吃与手作店集中，适合散步。', 64),
(26, '柳州工业博物馆', '柳州', '无', '/images/spot-liuzhou.jpg', 0, 4.5, '工业风,博物馆,免费', '柳州市柳北区跃进路', '老厂房改造的工业博物馆，蒸汽机车、机床与老照片，工业风照片很出片。', 52),
(27, '螺蛳粉小镇', '柳州', '4A', '/images/luosifenxiaozhen.jpg', 30, 4.5, '美食,研学,亲子', '柳州市柳南区螺蛳粉产业园', '螺蛳粉的发源地，可参观生产线、亲手做一碗粉，闻着酸笋味了解一座城市的产业。', 61),
(28, '中山路美食街', '南宁', '无', '/images/tesemeishi.jpg', 0, 4.5, '夜市,小吃,扫街', '南宁市青秀区中山路', '南宁最有名的夜宵街，老友粉、卷筒粉、酸野、烤生蚝一条街吃到底。', 66),
(29, '方特东盟神画', '南宁', '4A', '/images/spot-nanning.jpg', 199, 4.6, '主题乐园,亲子,夜场', '南宁市青秀区民族大道延长线', '以东盟十国文化为主题的大型乐园，室内项目多，雨天也能玩，适合亲子。', 58),
(30, '邕江夜游', '南宁', '无', '/images/spot-nanning.jpg', 88, 4.4, '夜游,游船,江景', '南宁市青秀区民生码头', '乘船游邕江，两岸是南宁的城市天际线与桥梁灯光，全程约 60 分钟。', 49),
(31, '北海海底世界', '北海', '4A', '/images/spot-yintan.jpg', 138, 4.3, '亲子,海洋馆,雨天可选', '北海市海城区茶亭路', '海底隧道、企鹅馆与海狮表演，带小孩来北海的备选行程。', 57),
(32, '金海湾红树林', '北海', '4A', '/images/spot-yintan.jpg', 60, 4.5, '红树林,赶海,生态', '北海市银海区金海湾', '成片红树林与滩涂，退潮时可以跟着渔民赶海，看招潮蟹与弹涂鱼。', 54);

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
