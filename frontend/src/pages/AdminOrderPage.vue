<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '', orderStatus: '', paymentStatus: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/orders', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function markPaid(row) {
  try {
    await request.post('/api/admin/orders/status', { orderId: row.id, orderStatus: 'PAID', paymentStatus: 'SUCCESS' })
    ElMessage.success('订单状态已更新')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page glass-card" v-loading="loading">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="订单号" style="max-width: 220px" />
      <el-select v-model="query.orderStatus" clearable placeholder="订单状态">
        <el-option label="待支付" value="PENDING" />
        <el-option label="已支付" value="PAID" />
      </el-select>
      <el-select v-model="query.paymentStatus" clearable placeholder="支付状态">
        <el-option label="未支付" value="UNPAID" />
        <el-option label="成功" value="SUCCESS" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="nickname" label="用户" />
      <el-table-column prop="totalAmount" label="金额" />
      <el-table-column prop="orderStatus" label="订单状态" />
      <el-table-column prop="paymentStatus" label="支付状态" />
      <el-table-column label="退款状态">
        <template #default="{ row }">{{ row.refund?.refundStatus || '-' }}</template>
      </el-table-column>
      <el-table-column prop="paymentChannel" label="渠道" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.orderStatus !== 'PAID'" link type="primary" @click="markPaid(row)">标记已支付</el-button>
          <el-button link type="warning" @click="$router.push('/admin/refund')">退款处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      style="margin-top: 16px; justify-content: center"
      @current-change="(page) => { query.pageNo = page; loadData() }"
    />
  </div>
</template>

<style scoped>
.admin-page {
  padding: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
</style>
