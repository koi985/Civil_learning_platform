<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, paymentChannel: '', paymentStatus: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/payment-logs', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page glass-card" v-loading="loading">
    <div class="toolbar">
      <el-select v-model="query.paymentChannel" clearable placeholder="支付渠道">
        <el-option label="支付宝" value="ALIPAY" />
        <el-option label="微信" value="WECHAT" />
      </el-select>
      <el-select v-model="query.paymentStatus" clearable placeholder="支付状态">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="已退款" value="REFUNDED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="orderId" label="订单ID" />
      <el-table-column prop="userId" label="用户ID" />
      <el-table-column prop="paymentChannel" label="渠道" />
      <el-table-column prop="paymentAmount" label="金额" />
      <el-table-column prop="transactionNo" label="交易号" />
      <el-table-column prop="paidAt" label="支付时间" />
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
