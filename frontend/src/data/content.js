/** 各城市玩法 / 资讯 / 服务中心内容（与数据库景点、商品联动） */

export const PLAYS = [
  // ======== 桂林（9） ========
  { city: '桂林', title: '漓江竹筏漂流', img: '/images/play-cruise.jpg', tag: '必玩榜首', desc: '从杨堤到兴坪，20 元人民币背景实景，九马画山一路看过去', price: 215, productId: 1, days: '约 4 小时' },
  { city: '桂林', title: '两江四湖夜游', img: '/images/play-night.jpg', tag: '夜景王牌', desc: '日月双塔、玻璃桥、沿岸灯光秀，坐船看桂林城最美的一面', price: 185, productId: 3, days: '约 1.5 小时' },
  { city: '桂林', title: '龙脊梯田徒步', tag: '摄影必去', img: '/images/play-hike.jpg', desc: '平安寨—金坑大寨徒步线，云雾里的千层梯田，日出日落都值得等', price: 80, productId: 5, days: '1 天' },
  { city: '桂林', title: '遇龙河竹筏慢漂', tag: '亲水休闲', img: '/images/spot-yulonghe.jpg', desc: '人工撑筏，水浅流缓，两岸是阳朔最典型的田园山水', price: 168, productId: 6, days: '约 2 小时' },
  { city: '桂林', title: '阳朔西街夜生活', tag: '年轻聚集地', img: '/images/spot-xijie.jpg', desc: '西街酒吧、手作小店、啤酒鱼，晚上 8 点后最有味道', price: 0, spotId: 7, days: '建议住一晚' },
  { city: '桂林', title: '银子岩溶洞探秘', tag: '亲子友好', img: '/images/spot-yinziyan.jpg', desc: '钟乳石长廊，恒温 20℃，雨天也能玩的室内景点', price: 65, productId: 7, days: '约 1.5 小时' },
  { city: '桂林', title: '象鼻山城市地标打卡', tag: '经典必打卡', img: '/images/spot-xiangbishan.jpg', desc: '桂林城徽，水月洞里看象山水月，江边漫步半小时就能拍满内存卡', price: 55, productId: 2, days: '约 1.5 小时' },
  { city: '桂林', title: '独秀峰·王城怀古', tag: '人文历史', img: '/images/spot-duxiufeng.jpg', desc: '靖江王城里的「桂林山水甲天下」摩崖石刻真迹就在这里', price: 100, productId: 4, days: '约 2 小时' },
  { city: '桂林', title: '世外桃源田园游', tag: '田园慢游', img: '/images/spot-shiwaitaoyuan.jpg', desc: '坐船穿过燕子湖，侗寨歌舞与稻田风光，像走进课本里的桃花源', price: 60, productId: 8, days: '半天' },

  // ======== 柳州（9） ========
  { city: '柳州', title: '百里柳江夜游', tag: '城市名片', img: '/images/liujiangyejing.jpg', desc: '江面音乐喷泉 + 两岸灯光，柳州最出片的夜景线路', price: 0, spotId: 22, days: '约 1 小时' },
  { city: '柳州', title: '螺蛳粉嗦粉之旅', tag: '吃货必打卡', img: '/images/liuzhouluosifen.jpg', desc: '汤底现熬、酸笋管够，配一份鸭脚和腐竹才地道', price: 26, productId: 67, days: '随时' },
  { city: '柳州', title: '程阳八寨侗族风情', tag: '民族文化', img: '/images/sanjiangchengyangbazhai.jpg', desc: '世界四大历史名桥程阳永济桥，百家宴、侗族大歌都能体验', price: 60, productId: 16, days: '1 天' },
  { city: '柳州', title: '三江油茶体验', tag: '非遗味道', img: '/images/sanjiangyoucha.jpg', desc: '茶叶、生姜、糯米捶煮，配米花花生，一碗下去全身暖', price: 18, shopId: 8, days: '约 1 小时' },
  { city: '柳州', title: '马鞍山看柳州全景', tag: '免费机位', img: '/images/maanshan.jpg', desc: '登顶 15 分钟，柳州半岛全景尽收眼底，日落最佳', price: 0, days: '约 2 小时' },
  { city: '柳州', title: '大龙潭喀斯特山水', tag: '城市绿肺', img: '/images/spot-liuzhou.jpg', desc: '雷山镜湖、石林栈道，柳州人周末遛娃的后花园，免费开放', price: 0, spotId: 33, days: '半天' },
  { city: '柳州', title: '融水苗寨风情两日', tag: '民族深度游', img: '/images/rongshuimiaozhai.jpg', desc: '拦门酒、芦笙踩堂、百家宴，住一晚吊脚楼才算来过苗寨', price: 60, spotId: 34, days: '1-2 天' },
  { city: '柳州', title: '螺蛳粉小镇工业游', tag: '亲子研学', img: '/images/luosifenxiaozhen.jpg', desc: '看一袋螺蛳粉怎么从生产线到你手里，还能现场试吃打包', price: 30, productId: 76, days: '半天' },
  { city: '柳州', title: '窑埠古镇滨江慢行', tag: '文艺路线', img: '/images/spot-liuzhou.jpg', desc: '骑楼街、文创小店配江景咖啡，晚上接夜游柳江正好', price: 0, spotId: 25, days: '约 2 小时' },

  // ======== 南宁（9） ========
  { city: '南宁', title: '青秀山登高赏花', tag: '城市绿肺', img: '/images/spot-qingxiushan.jpg', desc: '苏铁园、兰园四季有花，山顶可俯瞰邕江与东盟商务区', price: 20, productId: 9, days: '半天' },
  { city: '南宁', title: '广西民族博物馆', tag: '人文必看', img: '/images/spot-minzubowuguan.jpg', desc: '铜鼓、壮锦、干栏式建筑，一次看懂广西十二个世居民族', price: 0, spotId: 11, days: '约 3 小时' },
  { city: '南宁', title: '中山路夜市扫街', tag: '深夜食堂', img: '/images/tesemeishi.jpg', desc: '老友粉、卷筒粉、酸野、烤生蚝，南宁夜宵一条街', price: 45, shopId: 4, days: '晚上 6 点后' },
  { city: '南宁', title: '方特东盟神画一日', tag: '亲子首选', img: '/images/spot-nanning.jpg', desc: '一天「走遍」东盟十国，室内项目多，雨天也能玩尽兴', price: 199, productId: 77, days: '全天' },
  { city: '南宁', title: '邕江夜游赏两岸', tag: '江景夜色', img: '/images/spot-nanning.jpg', desc: '从民生码头出发，青山大桥、东盟商务区灯光一路铺开', price: 88, productId: 78, days: '约 1 小时' },
  { city: '南宁', title: '大明山避暑云海', tag: '山岳度假', img: '/images/spot-nanning.jpg', desc: '北回归线上的绿色明珠，夏季 22℃，云海日出雾凇三绝', price: 128, spotId: 35, days: '1 天' },
  { city: '南宁', title: '三街两巷糖水之旅', tag: '老街甜品', img: '/images/drink-yangzhiganlu.jpg', desc: '骑楼老宅里喝一碗槐花粉，逛完直接接中山路夜市', price: 10, shopId: 14, days: '约 2 小时' },
  { city: '南宁', title: '水街老味早餐线', tag: '本地吃法', img: '/images/food-laoyoufen.jpg', desc: '卷筒粉、生榨米粉、老友粉，跟着南宁土著从水街吃到八点', price: 9, shopId: 13, days: '早上 7-10 点' },
  { city: '南宁', title: '南湖公园环湖夜跑', tag: '免费休闲', img: '/images/spot-nanning.jpg', desc: '8 公里环湖步道，棕榈园与名树博览园四季皆绿', price: 0, spotId: 36, days: '傍晚' },

  // ======== 北海（9） ========
  { city: '北海', title: '涠洲岛环岛一日', tag: '海岛首选', img: '/images/spot-weizhoudao.jpg', desc: '火山口、滴水丹屏、五彩滩，租一台电动车慢慢逛', price: 98, productId: 10, days: '1-2 天' },
  { city: '北海', title: '银滩赶海踏浪', tag: '亲子玩水', img: '/images/spot-yintan.jpg', desc: '沙细如粉，退潮时能捡到小螃蟹和贝壳，日落很美', price: 0, spotId: 12, days: '半天' },
  { city: '北海', title: '北海老街骑楼', tag: '老街漫步', img: '/images/spot-beihailaojie.jpg', desc: '百年骑楼、虾饼、糖水铺，一条街吃遍北海小吃', price: 0, spotId: 14, days: '约 2 小时' },
  { city: '北海', title: '德天跨国瀑布一日游', tag: '周边必去', img: '/images/spot-detian.jpg', desc: '亚洲第一大跨国瀑布，水声轰隆，对面就是越南板约瀑布', price: 115, productId: 11, days: '1 天' },
  { city: '北海', title: '海底世界亲子游', tag: '室内遛娃', img: '/images/spot-yintan.jpg', desc: '海底隧道、人鲨共舞表演，雨天和暴晒天的首选去处', price: 138, productId: 79, days: '约 2.5 小时' },
  { city: '北海', title: '金海湾红树林观鸟', tag: '生态湿地', img: '/images/spot-yintan.jpg', desc: '栈道穿过万亩红树林，退潮时赶海挖螺，白鹭一路相伴', price: 60, productId: 80, days: '半天' },
  { city: '北海', title: '侨港风情街觅食', tag: '吃货路线', img: '/images/tesemeishi.jpg', desc: '糖水、卷粉、炒螺一条街，晚上十点依旧烟火升腾', price: 0, spotId: 39, days: '晚上' },
  { city: '北海', title: '冠头岭看海上日落', tag: '日落机位', img: '/images/04.jpg', desc: '北海市区最高点，森林步道尽头就是海，日落拍到手软', price: 0, spotId: 38, days: '傍晚' },
  { city: '北海', title: '外沙岛疍家海鲜宴', tag: '海鲜大餐', img: '/images/food-haixian.jpg', desc: '疍家渔民直供，车螺芥菜汤、白灼沙虫，鲜到不加味精', price: 28, shopId: 16, days: '午餐/晚餐' }
]

export const NEWS = [
  // ======== 桂林（7） ========
  { city: '桂林', title: '桂林 3 天 2 晚经典路线｜漓江 + 阳朔 + 龙脊一次玩透', img: '/images/03.jpg', date: '2026-10-28', views: '1.8 万', tag: '路线攻略', desc: '第一天漓江竹筏到兴坪、住阳朔；第二天遇龙河骑行、西街夜游；第三天龙脊梯田看日出，返程桂林。' },
  { city: '桂林', title: '漓江游船怎么买票最划算？本地人给你的 5 条建议', img: '/images/play-cruise.jpg', date: '2026-10-20', views: '1.2 万', tag: '购票指南', desc: '竹筏与游船的区别、上船码头怎么选、学生票核验规则、旺季提前几天订，一篇讲清。' },
  { city: '桂林', title: '龙脊梯田最佳观赏期：灌水期、金黄期、雪景期全解析', img: '/images/06.jpg', date: '2026-10-12', views: '9800', tag: '时令提醒', desc: '4-6 月灌水如镜、9-10 月金黄稻浪、1-2 月偶有雪景，附各寨观景台机位。' },
  { city: '桂林', title: '桂林米粉指南：卤菜粉、马肉粉到底怎么点才内行', img: '/images/food-jianandan.jpg', date: '2026-10-08', views: '8600', tag: '美食地图', desc: '先干拌后加汤、锅烧要不要单点、免费加的酸笋在哪里，米粉店潜规则一次说明白。' },
  { city: '桂林', title: '带爸妈游桂林：不赶路、少爬山的 4 天舒缓版行程', img: '/images/spot-xiangbishan.jpg', date: '2026-09-30', views: '7500', tag: '亲子孝心游', desc: '两江四湖夜游 + 象鼻山 + 大圩古镇，每天只安排一个重点景点，节奏刚刚好。' },
  { city: '桂林', title: '雨天桂林怎么玩？溶洞、博物馆、米粉店一条线', img: '/images/spot-yinziyan.jpg', date: '2026-09-22', views: '6300', tag: '天气攻略', desc: '银子岩恒温 20℃、桂海碑林、王城博物馆，下雨天照样玩得有滋有味。' },
  { city: '桂林', title: '阳朔住宿怎么选？西街、十里画廊、兴坪三大片区对比', img: '/images/spot-xijie.jpg', date: '2026-09-15', views: '9100', tag: '住宿指南', desc: '夜生活选西街、安静看山选十里画廊、看日出漓江弯选兴坪，附预算参考。' },

  // ======== 柳州（7） ========
  { city: '柳州', title: '螺蛳粉到底哪家强？柳州本地人私藏 6 家店', img: '/images/liuzhouluosifen.jpg', date: '2026-10-25', views: '2.3 万', tag: '美食地图', desc: '从老牌国营风到新派鸳鸯锅，酸笋、腐竹、鸭脚怎么加才地道。' },
  { city: '柳州', title: '百里柳江夜游全攻略：喷泉时间、最佳机位、船票', img: '/images/liujiangyejing.jpg', date: '2026-10-18', views: '1.5 万', tag: '夜游指南', desc: '音乐喷泉每晚 20:30 开演，文惠桥与蟠龙山是两个最佳观赏点。' },
  { city: '柳州', title: '程阳八寨两天一夜：侗寨住宿、百家宴与篝火晚会', img: '/images/sanjiangchengyangbazhai.jpg', date: '2026-10-08', views: '8600', tag: '深度游', desc: '住寨子里的木楼，清晨看雾起，晚上参加篝火晚会，比一日游值太多。' },
  { city: '柳州', title: '柳州 24 小时嗦粉路线：从青云夜市到螺蛳粉小镇', img: '/images/luosifenxiaozhen.jpg', date: '2026-10-05', views: '1.1 万', tag: '吃货路线', desc: '早上卷粉、中午螺蛳粉、下午参观粉厂、晚上鸭脚煲，一天吃透柳州。' },
  { city: '柳州', title: '三江油茶为什么是「一杯苦二杯甲」？', img: '/images/sanjiangyoucha.jpg', date: '2026-09-28', views: '7200', tag: '非遗味道', desc: '侗家油茶的捶煮工艺、配茶点讲究，以及怎么喝才算懂礼数。' },
  { city: '柳州', title: '马鞍山 vs 鹅山：拍柳州全景到底去哪个山头？', img: '/images/maanshan.jpg', date: '2026-09-20', views: '5800', tag: '摄影机位', desc: '马鞍山拍半岛全景，鹅山拍柳江大转弯，附两个机位的最佳时间。' },
  { city: '柳州', title: '融水苗寨节庆日历：芦笙节、打同年、新禾节时间表', img: '/images/rongshuimiaozhai.jpg', date: '2026-09-12', views: '6600', tag: '节庆活动', desc: '想去苗寨凑热闹的看这里，一年四季哪个寨子过什么节都列清了。' },

  // ======== 南宁（7） ========
  { city: '南宁', title: '南宁两日游：青秀山 + 民族博物馆 + 中山路夜市', img: '/images/spot-qingxiushan.jpg', date: '2026-10-22', views: '1.1 万', tag: '路线攻略', desc: '白天看山水人文，晚上扫街吃夜宵，地铁 1 号线基本能串起来。' },
  { city: '南宁', title: '老友粉为什么是南宁人的早餐信仰？', img: '/images/tesemeishi.jpg', date: '2026-10-15', views: '7200', tag: '城市味道', desc: '酸笋、豆豉、辣椒爆香，一碗粉唤醒一座城，附 4 家老店地址。' },
  { city: '南宁', title: '方特东盟神画游玩攻略：项目推荐与错峰时间', img: '/images/spot-nanning.jpg', date: '2026-10-02', views: '9800', tag: '亲子攻略', desc: '十国主题馆怎么排、哪几个室内项目最值得、几点入园人最少。' },
  { city: '南宁', title: '邕江夜游新体验：民生码头出发看三桥六岸灯光', img: '/images/spot-nanning.jpg', date: '2026-09-26', views: '6400', tag: '夜游指南', desc: '船票班次、甲板最佳位置、沿途能看到哪些地标建筑。' },
  { city: '南宁', title: '水街早餐扫街图鉴：卷筒粉、生榨米粉、粉饺', img: '/images/food-laoyoufen.jpg', date: '2026-09-18', views: '8200', tag: '美食地图', desc: '水街是南宁老味道的源头，早上 7 点前到的都是本地人。' },
  { city: '南宁', title: '大明山避暑周未行：云海、日出、雾凇怎么追', img: '/images/spot-nanning.jpg', date: '2026-09-10', views: '5900', tag: '山岳攻略', desc: '山上住宿怎么订、看云海要几点起、冬季雾凇一般哪几天有。' },
  { city: '南宁', title: '三街两巷 City Walk：骑楼、糖水与新派文创', img: '/images/drink-yangzhiganlu.jpg', date: '2026-09-03', views: '5400', tag: '城市漫步', desc: '一条路线串起老骑楼、广式糖水铺和年轻人爱逛的文创小店。' },

  // ======== 北海（7） ========
  { city: '北海', title: '涠洲岛 2 天 1 夜：船票、住宿、环岛路线一次说清', img: '/images/spot-weizhoudao.jpg', date: '2026-10-26', views: '1.6 万', tag: '海岛攻略', desc: '北海国际客运港上岛 70 分钟，岛上租电动车 60 元/天，日落去滴水丹屏。' },
  { city: '北海', title: '银滩赶海时间表：退潮前后 2 小时最好玩', img: '/images/spot-yintan.jpg', date: '2026-10-10', views: '9300', tag: '玩法推荐', desc: '带小桶和铲子，退潮时能捡到螃蟹、贝壳，注意看潮汐表别走太远。' },
  { city: '北海', title: '北海吃海鲜防坑指南：哪个市场买、哪家加工靠谱', img: '/images/food-haixian.jpg', date: '2026-10-01', views: '1.2 万', tag: '美食避坑', desc: '外沙岛与侨港市场对比、加工费行价、时令海鲜月份表。' },
  { city: '北海', title: '侨港风情街觅食清单：糖水、卷粉、炒螺三件套', img: '/images/tesemeishi.jpg', date: '2026-09-24', views: '8700', tag: '美食地图', desc: '越南归侨带来的风味街，从晚上 7 点吃到 11 点刚刚好。' },
  { city: '北海', title: '金海湾红树林：退潮赶海与观鸟的正确打开方式', img: '/images/spot-yintan.jpg', date: '2026-09-16', views: '6100', tag: '生态游', desc: '栈道漫步、滩涂抓蟹，带娃认识红树林生态的天然课堂。' },
  { city: '北海', title: '冠头岭日落机位实测：三个观景点拍摄对比', img: '/images/04.jpg', date: '2026-09-08', views: '5200', tag: '摄影机位', desc: '主峰观景台、南坡礁石、森林公园步道，各有各的出片角度。' },
  { city: '北海', title: '北海 3 天 2 晚懒人行程：老街 + 银滩 + 涠洲岛', img: '/images/spot-beihailaojie.jpg', date: '2026-09-01', views: '7800', tag: '路线攻略', desc: '第一天老街骑楼与夜景，第二天上岛环游，第三天银滩收尾返程。' }
]

/** 服务中心：常见问题 / 预订支付 / 会员权益 / 跟团游 / 美食外带 / 服务保障 */
export const SERVICE_GROUPS = [
  {
    title: '常见问题',
    items: [
      '门票下单后多久出票？',
      '美食订单的取餐号在哪里看？',
      '儿童 / 学生票怎么核验？',
      '景点临时闭园怎么退款？',
      '可以改期或转让门票吗？',
      '订单里找不到我的预订？'
    ]
  },
  {
    title: '预订支付',
    items: [
      '支持哪些支付方式？',
      '支付宝沙箱支付怎么用？',
      '支付成功但订单未更新？',
      '发票如何申请？',
      '退款多久到账？',
      '为什么金额和页面显示不一样？'
    ]
  },
  {
    title: '会员权益',
    items: [
      'VIP 会员有哪些折扣？',
      '会员有效期怎么计算？',
      '续费会叠加时长吗？',
      '会员价和优惠券能叠加吗？',
      '会员订单退款怎么算？',
      '如何查看会员到期时间？'
    ]
  },
  {
    title: '跟团游报名',
    items: [
      '跟团游覆盖哪些城市？',
      '报名后怎么联系导游？',
      '可以变更出行日期吗？',
      '儿童报名有什么优惠？',
      '成团人数不够怎么办？',
      '报名费包含哪些项目？'
    ]
  },
  {
    title: '美食外带',
    items: [
      '取餐号保留多久？',
      '可以指定打包口味吗？',
      '到店找不到取餐柜台？',
      '食品出问题怎么投诉？',
      '能开发票或收据吗？',
      '奶茶可以备注少糖吗？'
    ]
  },
  {
    title: '服务保障',
    items: [
      '商家资质审核机制',
      '先行赔付承诺',
      '评价真实性保障',
      '7×12 小时在线客服',
      '个人信息保护说明',
      '商家数据隔离说明'
    ]
  }
]

/** 服务说明长块：预订 / 取餐 / 支付 / 保障 / 会员 / 跟团 */
export const SERVICE_BLOCKS = [
  {
    icon: '🎫',
    title: '门票预订说明',
    lines: [
      '1. 门票由平台直营出票，下单后需在支付宝沙箱完成支付，支付成功即出票。',
      '2. 学生票入园需出示本人学生证，未携带需现场补差价。',
      '3. 游玩日期可选今天及以后 30 天内，未使用可在游玩日前一天 18:00 前申请退款。'
    ]
  },
  {
    icon: '🍜',
    title: '取餐号说明',
    lines: [
      '1. 美食、奶茶饮品下单并支付成功后，系统会自动生成 4 位取餐号（如 A-3271）。',
      '2. 在「我的订单」中可以看到取餐号，也可以出示订单页给店员扫码。',
      '3. 到店报取餐号即可取餐，无需现场排队点单；超过 30 分钟未取请联系店铺。'
    ]
  },
  {
    icon: '💳',
    title: '支付与退款',
    lines: [
      '1. 目前接入支付宝沙箱「电脑网站支付」，用于课程项目演示，不会产生真实扣款。',
      '2. 支付完成后系统通过主动查单确认结果，订单状态自动更新为「已支付」。',
      '3. 退款原路退回支付宝账户，演示环境即时到账。'
    ]
  },
  {
    icon: '👑',
    title: '会员说明',
    lines: [
      '1. VIP 会员在门票、美食、奶茶、团购套餐、跟团游全部商品统一享 9 折。',
      '2. 会员身份支付成功即刻生效，到期后下单自动恢复原价，可随时续费。',
      '3. 连续包月 / 包年到期前 3 天会站内消息提醒，可随时关闭自动续费。'
    ]
  },
  {
    icon: '🚌',
    title: '跟团游说明',
    lines: [
      '1. 跟团游目前覆盖桂林、柳州、南宁、北海四城的精选线路，持续上新中。',
      '2. 报名时请填写真实联系电话，成团后导游会提前一天短信联系集合事宜。',
      '3. 会员报名享 9 折优惠；出行日前 3 天可免费改期或全额退款。'
    ]
  },
  {
    icon: '🛡',
    title: '服务保障',
    lines: [
      '1. 商家入驻需提交营业执照与食品经营许可证，平台人工审核后上线。',
      '2. 商家之间数据完全隔离，只能查看自己店铺的商品、订单与销量。',
      '3. 评价需真实消费后才能发布，每个商品最多评价 2 次，杜绝刷分。'
    ]
  }
]

export const FRIEND_LINKS = [
  '广西文化和旅游厅', '桂林市文化广电和旅游局', '柳州市文化广电和旅游局',
  '南宁市文化广电和旅游局', '北海市旅游文体局', '广西旅游发展集团',
  '桂林旅游股份', '漓江景区官网', '涠洲岛旅游区', '龙脊梯田景区',
  '广西民族博物馆', '广西旅游协会', '中国旅游报', '携程旅行', '去哪儿网',
  '马蜂窝', '飞猪旅行', '同程旅行', '广西日报', '南宁新闻网'
]
