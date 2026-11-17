<template>
  <main class="page admin-page">
    <div class="head-row">
      <div>
        <h2 class="page-title">🛠 平台管理后台</h2>
        <p class="page-sub">内容管理 · 商家审核 · 游客全景 · 商品上下架 · 配置 · 投诉 · 审计 · 仅 ADMIN 可见</p>
      </div>
      <el-tag type="warning" effect="dark" round size="large">管理员：{{ user?.nickname }}</el-tag>
    </div>

    <div class="stat-cards">
      <div class="stat-card glass" v-for="s in statCards" :key="s.label">
        <span class="label">{{ s.label }}</span><b class="grad-text">{{ s.value }}</b>
      </div>
    </div>

    <div v-if="pendingCount" class="notice glass">
      ⏳ 有 <b>{{ pendingCount }}</b> 个商家等待入驻审核
      <el-button size="small" type="warning" plain @click="tab = 'merchants'">去审核</el-button>
    </div>

    <el-tabs v-model="tab" class="admin-tabs">
      <el-tab-pane v-for="r in allTabs" :key="r.key" :label="r.title" :name="r.key" />
    </el-tabs>

    <div class="tool-row">
      <el-input v-if="cfg?.search" v-model="keyword" placeholder="输入关键词搜索" clearable
                style="width: 220px" @keyup.enter="load" />
      <el-select v-if="tab === 'orders'" v-model="status" style="width: 150px" @change="page = 1; load()">
        <el-option label="全部状态" value="" />
        <el-option label="待支付" value="PENDING" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已取消" value="CANCELED" />
      </el-select>
      <el-select v-if="tab === 'products'" v-model="filterMerchant" clearable placeholder="按商家筛选" style="width: 180px" @change="load">
        <el-option v-for="m in merchantOpts" :key="m.id" :label="m.nickname" :value="m.id" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <el-button v-if="basicCfg && tab !== 'orders'" type="primary" @click="openAdd">＋ 新增{{ basicCfg.title.replace('管理', '') }}</el-button>
      <el-button v-if="exportType" @click="exportCsv(exportType)">⬇ 导出 CSV</el-button>
    </div>

    <!-- 基础 CRUD 表格 -->
    <el-table v-if="basicCfg" :data="list" v-loading="loading" style="width: 100%" size="large" class="glass">
      <el-table-column v-for="c in basicCfg.columns" :key="c.prop" :prop="c.prop" :label="c.label"
                       :width="c.width" :min-width="c.minWidth" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="c.prop === 'status' && tab !== 'orders'" :class="row[c.prop] === 1 ? 'on' : 'off'">
            {{ row[c.prop] === 1 ? '启用' : '停用' }}
          </span>
          <el-tag v-else-if="c.prop === 'status'" :type="statusType(row[c.prop])" effect="dark" round size="small">
            {{ { PENDING: '待支付', PAID: '已支付', CANCELED: '已取消' }[row[c.prop]] || row[c.prop] }}
          </el-tag>
          <span v-else-if="c.prop === 'role'">{{ { CUSTOMER: '游客', MERCHANT: '商家', ADMIN: '管理员' }[row[c.prop]] }}</span>
          <span v-else-if="c.prop === 'type'">{{ { FOOD: '美食', DRINK: '饮品' }[row[c.prop]] || row[c.prop] }}</span>
          <span v-else-if="c.prop === 'merchantId'">{{ merchantName(row[c.prop]) }}</span>
          <span v-else-if="c.prop === 'spotId'">{{ spotName(row[c.prop]) }}</span>
          <span v-else-if="c.prop === 'total'">¥{{ row[c.prop] }}</span>
          <span v-else>{{ row[c.prop] }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <template v-if="tab === 'orders'">
            <el-select :model-value="row.status" size="small" style="width: 104px"
                       @change="v => changeOrderStatus(row, v)">
              <el-option label="待支付" value="PENDING" />
              <el-option label="已支付" value="PAID" />
              <el-option label="已取消" value="CANCELED" />
            </el-select>
            <el-button size="small" type="danger" plain style="margin-left: 6px" @click="del(row)">删除</el-button>
          </template>
          <template v-else>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="del(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="tab === 'orders'" class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10"
                     :current-page="page" @current-change="p => { page = p; load() }" />
    </div>

    <!-- 运营面板表格 -->
    <el-table v-else-if="opsCfg" :data="list" v-loading="loading" style="width: 100%" size="large" class="glass">
      <el-table-column v-for="c in opsCfg.columns" :key="c.prop" :prop="c.prop" :label="c.label"
                       :width="c.width" :min-width="c.minWidth" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="c.prop === 'auditStatus'" :type="auditType(row.auditStatus)" effect="dark" round size="small">
            {{ { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[row.auditStatus] || row.auditStatus }}
          </el-tag>
          <el-tag v-else-if="c.prop === 'status' && tab === 'complaints'" :type="complaintType(row.status)" effect="dark" round size="small">
            {{ { PENDING: '待受理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' }[row.status] || row.status }}
          </el-tag>
          <span v-else-if="c.prop === 'status' && tab === 'products'" :class="row.status === 1 ? 'on' : 'off'">
            {{ row.status === 1 ? '在售' : '已下架' }}
          </span>
          <span v-else-if="c.money">¥{{ Number(row[c.prop] || 0).toFixed(2) }}</span>
          <span v-else>{{ row[c.prop] }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="opsCfg.opWidth || 230" fixed="right">
        <template #default="{ row }">
          <template v-if="tab === 'merchants'">
            <el-button size="small" type="success" plain @click="audit(row, 'APPROVED')" :disabled="row.auditStatus === 'APPROVED'">通过</el-button>
            <el-button size="small" type="danger" plain @click="audit(row, 'REJECTED')">驳回</el-button>
            <el-button size="small" @click="drill('merchant', row)">产品/订单</el-button>
          </template>
          <el-button v-else-if="tab === 'customers'" size="small" @click="drill('customer', row)">查看订单</el-button>
          <el-button v-else-if="tab === 'products'" size="small" :type="row.status === 1 ? 'warning' : 'success'" plain
                     @click="toggleProduct(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button v-else-if="tab === 'configs'" size="small" type="primary" plain @click="editConfig(row)">修改</el-button>
          <el-button v-else-if="tab === 'complaints'" size="small" type="primary" plain @click="openHandle(row)">处理</el-button>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 基础新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog" :title="(editing ? '编辑' : '新增') + (basicCfg?.title || '').replace('管理', '')" width="560px">
      <el-form label-width="100px">
        <el-form-item v-for="f in basicCfg?.fields || []" :key="f.key" :label="f.label" :required="f.required">
          <el-select v-if="f.type === 'select'" v-model="form[f.key]" style="width: 100%">
            <el-option v-for="o in optionList(f)" :key="String(o.value)" :label="o.label" :value="o.value" />
          </el-select>
          <el-input-number v-else-if="f.type === 'number'" v-model="form[f.key]" :min="0" :precision="f.precision ?? 0" style="width: 100%" />
          <el-input v-else-if="f.type === 'textarea'" v-model="form[f.key]" type="textarea" :rows="3" />
          <el-input v-else v-model="form[f.key]" :placeholder="f.hint || ''" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 配置修改 -->
    <el-dialog v-model="configDialog" title="修改平台配置" width="460px">
      <p class="dlg-hint">{{ editingConfig?.description }}</p>
      <el-input v-model="configValue" type="textarea" :rows="3" />
      <template #footer>
        <el-button @click="configDialog = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 投诉处理 -->
    <el-dialog v-model="handleDialog" title="处理投诉工单" width="520px">
      <p class="dlg-hint">{{ editingComplaint?.content }}</p>
      <el-select v-model="handleForm.status" style="width: 100%; margin-bottom: 12px">
        <el-option label="标记处理中" value="PROCESSING" />
        <el-option label="已解决" value="RESOLVED" />
        <el-option label="驳回" value="REJECTED" />
        <el-option label="重新待受理" value="PENDING" />
      </el-select>
      <el-input v-model="handleForm.reply" type="textarea" :rows="4" placeholder="填写给游客的回复" />
      <template #footer>
        <el-button @click="handleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveHandle">提交处理</el-button>
      </template>
    </el-dialog>

    <!-- 商家产品/订单、游客订单下钻 -->
    <el-dialog v-model="drillDialog" :title="drillTitle" width="880px">
      <el-tabs v-model="drillTab" @tab-change="loadDrill">
        <el-tab-pane label="产品" name="products" />
        <el-tab-pane label="订单" name="orders" />
      </el-tabs>
      <el-table :data="drillRows" size="small" max-height="380">
        <el-table-column v-for="c in drillColumns" :key="c.prop" :prop="c.prop" :label="c.label"
                         :min-width="c.minWidth" :width="c.width" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!drillRows.length" description="暂无数据" :image-size="60" />
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { authUser } from '../../api.js'

const user = authUser
const CITIES = ['桂林', '柳州', '南宁', '北海']
const cityOpts = CITIES.map(c => ({ label: c, value: c }))

// ===== 基础 CRUD（原有能力）=====
const basic = {
  spots: {
    key: 'spots', title: '景点管理', api: '/admin/spots', search: true,
    columns: [
      { prop: 'id', label: 'ID', width: 70 }, { prop: 'name', label: '景点名称', minWidth: 160 },
      { prop: 'city', label: '城市', width: 90 }, { prop: 'level', label: '等级', width: 80 },
      { prop: 'price', label: '票价', width: 90 }, { prop: 'rating', label: '评分', width: 80 },
      { prop: 'hot', label: '热度', width: 80 }, { prop: 'address', label: '地址', minWidth: 160 }
    ],
    fields: [
      { key: 'name', label: '景点名称', required: true },
      { key: 'city', label: '所在城市', type: 'select', options: cityOpts, default: '桂林' },
      { key: 'level', label: '景区等级', type: 'select', options: ['5A', '4A', '3A', '其他'].map(v => ({ label: v, value: v })), default: '4A' },
      { key: 'price', label: '参考票价', type: 'number', precision: 2, default: 0 },
      { key: 'rating', label: '评分', type: 'number', precision: 1, default: 4.5 },
      { key: 'hot', label: '热度', type: 'number', default: 0 },
      { key: 'coverImg', label: '封面图', default: '/images/spot-guilin.jpg', hint: '/images/xxx.jpg' },
      { key: 'tags', label: '标签' }, { key: 'address', label: '地址' },
      { key: 'description', label: '简介', type: 'textarea' }
    ]
  },
  tours: {
    key: 'tours', title: '跟团游线路', api: '/admin/tours', search: true,
    columns: [
      { prop: 'id', label: 'ID', width: 70 }, { prop: 'title', label: '线路名称', minWidth: 220 },
      { prop: 'city', label: '城市', width: 90 }, { prop: 'days', label: '天数', width: 80 },
      { prop: 'departure', label: '出发地', width: 90 }, { prop: 'price', label: '成人价', width: 100 },
      { prop: 'stock', label: '余位', width: 80 }, { prop: 'status', label: '状态', width: 90 }
    ],
    fields: [
      { key: 'title', label: '线路名称', required: true },
      { key: 'city', label: '目的地城市', type: 'select', options: cityOpts, default: '桂林' },
      { key: 'days', label: '行程天数', type: 'number', default: 1 },
      { key: 'departure', label: '出发城市', default: '南宁' },
      { key: 'price', label: '成人价', type: 'number', precision: 2, default: 0 },
      { key: 'stock', label: '余位', type: 'number', default: 30 },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '上架', value: 1 }, { label: '下架', value: 0 }], default: 1 },
      { key: 'coverImg', label: '封面图', default: '/images/spot-guilin.jpg' },
      { key: 'highlights', label: '行程亮点' }, { key: 'itinerary', label: '行程安排', type: 'textarea' }
    ]
  },
  shops: {
    key: 'shops', title: '店铺管理', api: '/admin/shops',
    columns: [
      { prop: 'id', label: 'ID', width: 70 }, { prop: 'name', label: '店铺名称', minWidth: 180 },
      { prop: 'merchantId', label: '所属商家', width: 150 }, { prop: 'type', label: '类型', width: 90 },
      { prop: 'spotId', label: '挂靠景点', width: 150 }, { prop: 'rating', label: '评分', width: 80 },
      { prop: 'status', label: '状态', width: 90 }
    ],
    fields: [
      { key: 'name', label: '店铺名称', required: true },
      { key: 'merchantId', label: '所属商家', type: 'select', dynamic: 'merchants', required: true },
      { key: 'spotId', label: '挂靠景点', type: 'select', dynamic: 'spots', required: true },
      { key: 'type', label: '店铺类型', type: 'select', options: [{ label: '美食', value: 'FOOD' }, { label: '饮品', value: 'DRINK' }], default: 'FOOD' },
      { key: 'address', label: '地址' }, { key: 'phone', label: '联系电话' },
      { key: 'coverImg', label: '封面图', default: '/images/food-zhutongfan.jpg' },
      { key: 'rating', label: '评分', type: 'number', precision: 1, default: 4.5 },
      { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], default: 1 },
      { key: 'description', label: '店铺简介', type: 'textarea' }
    ]
  },
  users: {
    key: 'users', title: '用户管理', api: '/admin/users',
    columns: [
      { prop: 'id', label: 'ID', width: 70 }, { prop: 'username', label: '账号', minWidth: 150 },
      { prop: 'nickname', label: '昵称', minWidth: 140 }, { prop: 'role', label: '角色', width: 100 },
      { prop: 'vipLevel', label: 'VIP', width: 80 }, { prop: 'avatar', label: '头像', minWidth: 170 }
    ],
    fields: [
      { key: 'username', label: '账号', required: true },
      { key: 'password', label: '密码', hint: '编辑时留空表示不修改' },
      { key: 'nickname', label: '昵称' },
      { key: 'role', label: '角色', type: 'select', options: [
        { label: '游客', value: 'CUSTOMER' }, { label: '商家', value: 'MERCHANT' }, { label: '管理员', value: 'ADMIN' }
      ], default: 'CUSTOMER' },
      { key: 'vipLevel', label: 'VIP 等级', type: 'number', default: 0 },
      { key: 'avatar', label: '头像', default: '/images/avatar-default.svg' }
    ]
  },
  orders: {
    key: 'orders', title: '订单管理', api: '/admin/orders',
    columns: [
      { prop: 'orderNo', label: '订单号', minWidth: 190 }, { prop: 'productName', label: '商品', minWidth: 170 },
      { prop: 'quantity', label: '数量', width: 70 }, { prop: 'total', label: '金额', width: 100 },
      { prop: 'status', label: '状态', width: 100 }, { prop: 'createdAt', label: '下单时间', width: 170 }
    ],
    fields: []
  }
}

// ===== 新增运营面板 =====
const ops = {
  merchants: {
    key: 'merchants', title: '商家审核', api: '/admin/merchants', opWidth: 250, exportType: 'merchants',
    columns: [
      { prop: 'id', label: 'ID', width: 65 }, { prop: 'nickname', label: '商家', minWidth: 130 },
      { prop: 'merchantName', label: '主体名称', minWidth: 160 }, { prop: 'phone', label: '电话', width: 130 },
      { prop: 'shopCount', label: '店铺', width: 75 }, { prop: 'productCount', label: '商品', width: 75 },
      { prop: 'orderCount', label: '订单', width: 75 }, { prop: 'turnover', label: '营业额', width: 110, money: true },
      { prop: 'auditStatus', label: '审核状态', width: 110 }
    ]
  },
  customers: {
    key: 'customers', title: '游客全景', api: '/admin/customers', opWidth: 130, exportType: 'customers',
    columns: [
      { prop: 'id', label: 'ID', width: 65 }, { prop: 'username', label: '账号', minWidth: 140 },
      { prop: 'nickname', label: '昵称', minWidth: 130 }, { prop: 'phone', label: '手机', width: 130 },
      { prop: 'vipLevel', label: 'VIP', width: 70 }, { prop: 'orderCount', label: '订单数', width: 90 },
      { prop: 'totalSpent', label: '累计消费', width: 120, money: true },
      { prop: 'createdAt', label: '注册时间', width: 170 }
    ]
  },
  products: {
    key: 'products', title: '商品上下架', api: '/admin/products', opWidth: 130, exportType: 'products',
    columns: [
      { prop: 'id', label: 'ID', width: 65 }, { prop: 'name', label: '商品', minWidth: 190 },
      { prop: 'type', label: '类型', width: 95 }, { prop: 'price', label: '价格', width: 95, money: true },
      { prop: 'sales', label: '销量', width: 80 }, { prop: 'stock', label: '库存', width: 80 },
      { prop: 'shopName', label: '店铺', minWidth: 140 }, { prop: 'merchantName', label: '商家', minWidth: 130 },
      { prop: 'city', label: '城市', width: 85 }, { prop: 'status', label: '状态', width: 95 }
    ]
  },
  configs: {
    key: 'configs', title: '平台配置', api: '/admin/configs', opWidth: 120,
    columns: [
      { prop: 'configKey', label: '配置键', minWidth: 180 },
      { prop: 'configValue', label: '配置值', minWidth: 260 },
      { prop: 'description', label: '说明', minWidth: 180 },
      { prop: 'updatedAt', label: '更新时间', width: 170 }
    ]
  },
  complaints: {
    key: 'complaints', title: '投诉工单', api: '/admin/complaints', opWidth: 120, exportType: 'complaints',
    columns: [
      { prop: 'id', label: 'ID', width: 65 }, { prop: 'userName', label: '提交人', width: 110 },
      { prop: 'category', label: '分类', width: 100 }, { prop: 'targetName', label: '涉及对象', minWidth: 140 },
      { prop: 'content', label: '内容', minWidth: 240 }, { prop: 'status', label: '状态', width: 100 },
      { prop: 'reply', label: '回复', minWidth: 180 }, { prop: 'createdAt', label: '提交时间', width: 170 }
    ]
  },
  logs: {
    key: 'logs', title: '操作日志', api: '/admin/logs',
    columns: [
      { prop: 'id', label: 'ID', width: 70 }, { prop: 'adminName', label: '操作人', width: 120 },
      { prop: 'action', label: '动作', width: 140 }, { prop: 'detail', label: '详情', minWidth: 300 },
      { prop: 'createdAt', label: '时间', width: 170 }
    ]
  }
}

const allTabs = [...Object.values(basic), ...Object.values(ops)]
const tab = ref('spots')
const basicCfg = computed(() => basic[tab.value])
const opsCfg = computed(() => ops[tab.value])
const cfg = computed(() => basicCfg.value || opsCfg.value)
const exportType = computed(() => opsCfg.value?.exportType || (tab.value === 'orders' ? 'orders' : ''))
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const status = ref('')
const page = ref(1)
const total = ref(0)
const dialog = ref(false)
const editing = ref(null)
const form = ref({})
const overview = ref({})
const merchants = ref([])
const spots = ref([])
const filterMerchant = ref(null)
const merchantOpts = computed(() => merchants.value)
const pendingCount = ref(0)

const configDialog = ref(false)
const editingConfig = ref(null)
const configValue = ref('')
const handleDialog = ref(false)
const editingComplaint = ref(null)
const handleForm = reactive({ status: 'RESOLVED', reply: '' })
const drillDialog = ref(false)
const drillTab = ref('products')
const drillRows = ref([])
const drillTarget = ref(null)
const drillKind = ref('merchant')
const drillTitle = computed(() => drillKind.value === 'merchant'
  ? '商家全景 · ' + (drillTarget.value?.nickname || '') : '游客订单 · ' + (drillTarget.value?.nickname || ''))
const drillColumns = computed(() => drillTab.value === 'products'
  ? [
      { prop: 'id', label: 'ID', width: 80 }, { prop: 'name', label: '商品', minWidth: 180 },
      { prop: 'type', label: '类型', width: 100 }, { prop: 'price', label: '价格', width: 100 },
      { prop: 'sales', label: '销量', width: 90 }, { prop: 'status', label: '状态', width: 90 },
      { prop: 'shopName', label: '店铺', minWidth: 140 }
    ]
  : [
      { prop: 'orderNo', label: '订单号', minWidth: 190 }, { prop: 'productName', label: '商品', minWidth: 170 },
      { prop: 'quantity', label: '数量', width: 80 }, { prop: 'total', label: '金额', width: 110 },
      { prop: 'status', label: '状态', width: 100 }, { prop: 'createdAt', label: '时间', width: 170 }
    ])

const statCards = computed(() => [
  { label: '景点', value: overview.value.spots ?? 0 },
  { label: '跟团游线路', value: overview.value.tours ?? 0 },
  { label: '店铺', value: overview.value.shops ?? 0 },
  { label: '商品', value: overview.value.products ?? 0 },
  { label: '用户', value: overview.value.users ?? 0 },
  { label: '订单', value: overview.value.orders ?? 0 },
  { label: '已支付', value: overview.value.paidOrders ?? 0 },
  { label: '总营业额', value: '¥' + (overview.value.turnover ?? 0) }
])

function optionList(f) {
  if (f.dynamic === 'merchants') return merchants.value.map(u => ({ label: u.nickname + '（' + u.username + '）', value: u.id }))
  if (f.dynamic === 'spots') return spots.value.map(s => ({ label: s.name, value: s.id }))
  return f.options || []
}
function merchantName(id) { return merchants.value.find(u => u.id === id)?.nickname || ('商家#' + id) }
function spotName(id) { return spots.value.find(s => s.id === id)?.name || ('景点#' + id) }
function statusType(s) { return { PENDING: 'warning', PAID: 'success', CANCELED: 'info' }[s] || 'info' }
function auditType(s) { return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info' }
function complaintType(s) { return { PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'success', REJECTED: 'info' }[s] || 'info' }

async function load() {
  loading.value = true
  try {
    if (tab.value === 'orders') {
      const d = await request.get('/admin/orders', { params: { page: page.value, size: 10, status: status.value || undefined } })
      list.value = d.records || []; total.value = Number(d.total || 0)
    } else if (tab.value === 'products') {
      list.value = await request.get('/admin/products', {
        params: { merchantId: filterMerchant.value || undefined, keyword: keyword.value || undefined }
      }) || []
    } else if (basicCfg.value) {
      list.value = await request.get(basicCfg.value.api,
        basicCfg.value.search ? { params: { keyword: keyword.value || undefined } } : {}) || []
    } else {
      list.value = await request.get(opsCfg.value.api) || []
    }
    if (tab.value === 'merchants') {
      pendingCount.value = list.value.filter(x => x.auditStatus === 'PENDING').length
    }
  } finally { loading.value = false }
}

async function loadRefs() {
  try {
    const [u, s, o, m] = await Promise.all([
      request.get('/admin/users'), request.get('/admin/spots'), request.get('/admin/overview'),
      request.get('/admin/merchants', { silent: true }).catch(() => [])
    ])
    merchants.value = (u || []).filter(x => x.role === 'MERCHANT')
    spots.value = s || []
    overview.value = o || {}
    pendingCount.value = (m || []).filter(x => x.auditStatus === 'PENDING').length
  } catch { /* 拦截器已提示 */ }
}

function onTabChange() {
  page.value = 1; status.value = ''; keyword.value = ''; filterMerchant.value = null
  load()
}
function openAdd() {
  editing.value = null
  const f = {}
  basicCfg.value.fields.forEach(x => { if (x.default !== undefined) f[x.key] = x.default })
  form.value = f; dialog.value = true
}
function openEdit(row) {
  editing.value = row
  form.value = { ...row }
  delete form.value.password; delete form.value.createdAt
  dialog.value = true
}
async function save() {
  for (const f of basicCfg.value.fields) {
    if (f.required && (form.value[f.key] === undefined || form.value[f.key] === null || form.value[f.key] === '')) {
      return ElMessage.warning('请填写「' + f.label + '」')
    }
  }
  try {
    if (editing.value) await request.put(`${basicCfg.value.api}/${editing.value.id}`, form.value)
    else await request.post(basicCfg.value.api, form.value)
    ElMessage.success('已保存'); dialog.value = false; load(); loadRefs()
  } catch { /* 拦截器已提示 */ }
}
async function del(row) {
  const name = row.name || row.title || row.username || row.orderNo || ('#' + row.id)
  await ElMessageBox.confirm(`确定删除「${name}」？`, '删除确认', { type: 'warning' })
  await request.delete(`${cfg.value.api}/${row.id}`)
  ElMessage.success('已删除'); load(); loadRefs()
}
async function changeOrderStatus(row, v) {
  await request.put(`/admin/orders/${row.id}/status`, null, { params: { status: v } })
  ElMessage.success('订单状态已更新'); load(); loadRefs()
}

// ===== 运营操作 =====
async function audit(row, statusValue) {
  let remark = ''
  if (statusValue === 'REJECTED') {
    const r = await ElMessageBox.prompt('填写驳回原因', '驳回商家入驻', { inputPlaceholder: '资料不完整 / 资质不符' }).catch(() => null)
    if (!r) return
    remark = r.value || '资料不符合要求'
  }
  await request.put(`/admin/merchants/${row.id}/audit`, { status: statusValue, remark })
  ElMessage.success(statusValue === 'APPROVED' ? '已通过，商家可进入后台' : '已驳回')
  load(); loadRefs()
}
async function toggleProduct(row) {
  const next = row.status === 1 ? 0 : 1
  await request.put(`/admin/products/${row.id}/status`, null, { params: { status: next } })
  ElMessage.success(next === 1 ? '商品已上架' : '商品已下架')
  load(); loadRefs()
}
function editConfig(row) { editingConfig.value = row; configValue.value = row.configValue || ''; configDialog.value = true }
async function saveConfig() {
  await request.put(`/admin/configs/${editingConfig.value.configKey}`, { value: configValue.value })
  ElMessage.success('配置已保存'); configDialog.value = false; load()
}
function openHandle(row) {
  editingComplaint.value = row
  handleForm.status = row.status === 'PENDING' ? 'PROCESSING' : row.status
  handleForm.reply = row.reply || ''
  handleDialog.value = true
}
async function saveHandle() {
  await request.put(`/admin/complaints/${editingComplaint.value.id}`, handleForm)
  ElMessage.success('工单已处理'); handleDialog.value = false; load()
}
async function exportCsv(type) {
  const csv = await request.get('/admin/export', { params: { type } })
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${type}_${new Date().toISOString().slice(0, 10)}.csv`
  a.click(); URL.revokeObjectURL(url)
  ElMessage.success('已导出 ' + type + '.csv')
}
function drill(kind, row) {
  drillKind.value = kind; drillTarget.value = row
  drillTab.value = kind === 'customer' ? 'orders' : 'products'
  drillDialog.value = true
  loadDrill()
}
async function loadDrill() {
  if (!drillTarget.value) return
  const id = drillTarget.value.id
  if (drillKind.value === 'customer') {
    drillRows.value = await request.get('/admin/customer-orders', { params: { userId: id } }) || []
  } else if (drillTab.value === 'products') {
    drillRows.value = await request.get('/admin/products', { params: { merchantId: id } }) || []
  } else {
    drillRows.value = await request.get('/admin/merchant-orders', { params: { merchantId: id } }) || []
  }
}

watch(tab, onTabChange)
onMounted(() => { load(); loadRefs() })
</script>

<style scoped>
.admin-page { padding-bottom: 40px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-cards { display: grid; grid-template-columns: repeat(8, 1fr); gap: 10px; margin: 18px 0 16px; }
.stat-card { padding: 14px 12px; border-radius: 12px; }
.stat-card .label { display: block; color: var(--txt-sub); font-size: 11.5px; margin-bottom: 6px; }
.stat-card b { font-size: 18px; }
.notice { display: flex; align-items: center; gap: 14px; padding: 10px 16px; border-radius: 10px; margin-bottom: 14px; font-size: 13.5px; }
.notice b { color: #fbbf24; }
.admin-tabs { margin-bottom: 12px; }
.tool-row { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; flex-wrap: wrap; }
.pager { display: flex; justify-content: center; margin-top: 18px; }
.on { color: #4ade80; }
.off { color: #f87171; }
.muted { color: var(--txt-sub); }
.dlg-hint { color: var(--txt-sub); font-size: 13px; line-height: 1.7; margin: 0 0 14px; }
@media (max-width: 1100px) { .stat-cards { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 700px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } }
</style>
