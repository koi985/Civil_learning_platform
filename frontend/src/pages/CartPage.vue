<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const selectedIds = ref([])
const form = reactive({
  paymentChannel: 'ALIPAY',
  remark: '购物车下单'
})

const totalAmount = computed(() => list.value
  .filter((item) => selectedIds.value.includes(item.id))
  .reduce((sum, item) => sum + Number(item.price || 0), 0)
  .toFixed(2))

async function loadData() {
  loading.value = true
  try {
    list.value = await request.get('/api/cart/items')
    selectedIds.value = list.value.map((item) => item.id)
  } finally {
    loading.value = false
  }
}

async function removeItem(id) {
  await request.delete(`/api/cart/items/${id}`)
  ElMessage.success('已移出购物车')
  loadData()
}

async function createOrder() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择一门课程')
    return
  }
  await request.post('/api/orders/from-cart', {
    cartItemIds: selectedIds.value,
    paymentChannel: form.paymentChannel,
    remark: form.remark
  })
  ElMessage.success('订单已创建，请前往订单中心完成支付')
  router.push('/user/order')
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="glass-card cart-card" v-loading="loading">
      <div class="page-head">
        <div>
          <div class="section-title">购物车</div>
          <p>将想买的课程集中下单，随后在订单中心完成模拟支付。</p>
        </div>
      </div>

      <div class="cart-layout" v-if="list.length">
        <div class="cart-list">
          <el-checkbox-group v-model="selectedIds" class="item-stack">
            <article v-for="item in list" :key="item.id" class="item-card">
              <el-checkbox :label="item.id">
                <span class="checkbox-label">{{ item.title }}</span>
              </el-checkbox>
              <div class="item-meta">{{ item.teacherName }} · {{ item.subjectName }} · {{ item.examType }}</div>
              <div class="item-footer">
                <strong>¥{{ item.price }}</strong>
                <el-button text type="danger" @click="removeItem(item.id)">移除</el-button>
              </div>
            </article>
          </el-checkbox-group>
        </div>

        <aside class="glass-card pay-card">
          <div class="section-title small">下单信息</div>
          <el-select v-model="form.paymentChannel">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
          <el-input v-model="form.remark" style="margin-top: 12px" placeholder="订单备注" />
          <div class="amount">合计 ¥{{ totalAmount }}</div>
          <el-button type="primary" style="width: 100%" @click="createOrder">创建订单</el-button>
        </aside>
      </div>

      <el-empty v-else description="购物车还是空的，先去课程中心逛逛吧" />
    </section>
  </div>
</template>

<style scoped>
.cart-card,
.pay-card {
  padding: 24px;
}

.page-head p,
.item-meta {
  color: var(--muted);
}

.cart-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

.item-stack {
  display: grid;
  gap: 16px;
}

.item-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 18px;
  background: #fff;
}

.checkbox-label {
  font-weight: 700;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.amount {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
  margin: 18px 0;
}

.section-title.small {
  font-size: 22px;
}

@media (max-width: 900px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }
}
</style>
