<template>
  <div>
    <div class="head-row">
      <div>
        <h2 class="page-title">📦 商品管理</h2>
        <p class="page-sub">增删改查 · 同店铺内商品名自动查重，重名无法添加</p>
      </div>
      <button class="btn-grad" style="padding: 10px 22px" @click="openAdd">＋ 添加商品</button>
    </div>

    <!-- 店铺选择 -->
    <el-select v-model="shopId" placeholder="选择店铺" size="large" style="margin-bottom: 16px; width: 320px"
               @change="loadProducts">
      <el-option v-for="s in shops" :key="s.id" :label="s.name + '（' + (s.type === 'FOOD' ? '美食' : '饮品') + '）'"
                 :value="s.id" />
    </el-select>

    <el-table v-if="shopId" :data="products" v-loading="loading" style="width: 100%" size="large">
      <el-table-column label="商品" min-width="240">
        <template #default="{ row }">
          <div style="display: flex; align-items: center; gap: 10px">
            <img :src="row.coverImg" style="width: 54px; height: 40px; border-radius: 8px; object-fit: cover" />
            <b>{{ row.name }}</b>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ { FOOD: '美食', DRINK: '饮品', PACKAGE: '套餐' }[row.type] }}</template>
      </el-table-column>
      <el-table-column prop="price" label="售价" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="sales" label="销量" width="90" sortable />
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" round size="small">
            {{ row.status === 1 ? '在售' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="请先选择店铺" />

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑商品' : '添加商品'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="商品名">
          <el-input v-model="form.name" maxlength="30" show-word-limit
                    placeholder="示例：壮家竹筒饭（同店铺内不能重名）" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="FOOD">美食</el-radio-button>
            <el-radio-button value="DRINK">饮品</el-radio-button>
            <el-radio-button value="PACKAGE">团购套餐</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="售价(¥)"><el-input-number v-model="form.price" :min="0.1" :precision="2" /></el-form-item>
        <el-form-item v-if="form.type === 'PACKAGE'" label="原价(¥)">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="1" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="示例：竹筒现烤，糯米混着腊味与香茅，一人份，配一碗野菜汤更好吃" />
        </el-form-item>
        <el-form-item label="状态" v-if="editing">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="在售" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api.js'

const shops = ref([])
const shopId = ref(null)
const products = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const form = ref({})

onMounted(async () => {
  shops.value = await request.get('/merchant/shops')
  if (shops.value.length) {
    shopId.value = shops.value[0].id
    loadProducts()
  }
})

async function loadProducts() {
  loading.value = true
  try {
    products.value = await request.get(`/merchant/shops/${shopId.value}/products`)
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editing.value = null
  form.value = { name: '', type: 'FOOD', price: 10, stock: 100, description: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (editing.value) {
    await request.put(`/merchant/products/${editing.value.id}`, form.value)
    ElMessage.success('已保存')
  } else {
    await request.post(`/merchant/shops/${shopId.value}/products`, form.value)
    ElMessage.success('商品已添加')
  }
  dialogVisible.value = false
  loadProducts()
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」？删除后不可恢复`, '删除确认', { type: 'warning' })
  await request.delete(`/merchant/products/${row.id}`)
  ElMessage.success('已删除')
  loadProducts()
}
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
</style>
