<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentRefund = ref(null)
const reviewRemark = ref('')
const query = reactive({ pageNo: 1, pageSize: 10, refundStatus: '', keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/refunds', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detailLoading.value = true
  detailVisible.value = true
  reviewRemark.value = row.reviewRemark || ''
  try {
    currentRefund.value = await request.get(`/api/admin/refunds/${row.id}`)
  } finally {
    detailLoading.value = false
  }
}

async function processRefund(action) {
  try {
    await request.post(`/api/admin/refunds/${currentRefund.value.id}/${action}`, { reviewRemark: reviewRemark.value })
    ElMessage.success(action === 'approve' ? '退款已通过' : '退款已驳回')
    detailVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="glass-card panel">
      <div class="toolbar">
        <el-select v-model="query.refundStatus" clearable placeholder="退款状态">
          <el-option label="申请中" value="REQUESTED" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="退款中" value="REFUNDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索退款单、订单或用户" />
        <el-button type="primary" @click="query.pageNo = 1; loadData()">查询</el-button>
      </div>
    </div>

    <div class="glass-card panel" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="refundNo" label="退款单号" />
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="nickname" label="用户" />
        <el-table-column prop="refundAmount" label="退款金额" />
        <el-table-column prop="refundStatus" label="状态" />
        <el-table-column prop="requestedAt" label="申请时间" />
        <el-table-column prop="reviewedAt" label="处理时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 20px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </div>

    <el-drawer v-model="detailVisible" size="560px" title="退款详情">
      <div v-loading="detailLoading" class="detail-panel" v-if="currentRefund">
        <div class="detail-card"><span>退款单号</span><strong>{{ currentRefund.refundNo }}</strong></div>
        <div class="detail-card"><span>订单号</span><strong>{{ currentRefund.orderNo }}</strong></div>
        <div class="detail-card"><span>用户</span><strong>{{ currentRefund.nickname }}</strong></div>
        <div class="detail-card"><span>退款状态</span><strong>{{ currentRefund.refundStatus }}</strong></div>
        <div class="detail-card"><span>退款原因</span><strong>{{ currentRefund.refundReason }}</strong></div>
        <div class="detail-card"><span>处理备注</span><el-input v-model="reviewRemark" type="textarea" :rows="3" /></div>
        <div class="detail-card">
          <span>状态日志</span>
          <div class="log-list">
            <div v-for="item in currentRefund.logs || []" :key="item.id" class="log-item">
              <div>{{ item.actionType }} · {{ item.fromStatus || '-' }} → {{ item.toStatus }}</div>
              <div class="muted">{{ item.operatorName }} · {{ item.createdAt }}</div>
              <div class="muted">{{ item.remark || '无备注' }}</div>
            </div>
          </div>
        </div>
        <div v-if="currentRefund.refundStatus === 'REQUESTED'" class="toolbar">
          <el-button type="success" @click="processRefund('approve')">通过退款</el-button>
          <el-button type="danger" @click="processRefund('reject')">驳回退款</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.panel {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-panel {
  display: grid;
  gap: 12px;
}

.detail-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: #fff;
}

.detail-card span,
.muted {
  color: var(--muted);
}

.detail-card strong {
  display: block;
  margin-top: 8px;
  word-break: break-all;
}

.log-list {
  display: grid;
  gap: 10px;
}

.log-item {
  border-top: 1px dashed var(--border);
  padding-top: 10px;
}
</style>
