<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNo: 1,
  pageSize: 6,
  orderStatus: ''
})
const paymentChannel = ref('ALIPAY')
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentOrder = ref(null)
const refundVisible = ref(false)
const refundSubmitting = ref(false)
const refundForm = reactive({
  orderId: null,
  refundReason: ''
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/orders', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function payOrder(id) {
  await request.post(`/api/orders/${id}/pay`, { paymentChannel: paymentChannel.value })
  ElMessage.success('支付成功，课程已发放到我的课程')
  loadData()
}

async function openDetail(id) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    currentOrder.value = await request.get(`/api/orders/${id}`)
  } finally {
    detailLoading.value = false
  }
}

function openRefund(item) {
  refundForm.orderId = item.id
  refundForm.refundReason = ''
  refundVisible.value = true
}

async function submitRefund() {
  refundSubmitting.value = true
  try {
    await request.post(`/api/orders/${refundForm.orderId}/refund`, {
      refundReason: refundForm.refundReason
    })
    ElMessage.success('退款申请已提交')
    refundVisible.value = false
    await loadData()
    if (detailVisible.value && currentOrder.value?.id === refundForm.orderId) {
      currentOrder.value = await request.get(`/api/orders/${refundForm.orderId}`)
    }
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="order-page glass-card" v-loading="loading">
    <div class="page-head">
      <div class="section-title">订单中心</div>
      <div class="toolbar">
        <el-select v-model="query.orderStatus" clearable placeholder="订单状态" @change="loadData">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
        </el-select>
        <el-select v-model="paymentChannel">
          <el-option label="支付宝" value="ALIPAY" />
          <el-option label="微信支付" value="WECHAT" />
        </el-select>
      </div>
    </div>

    <div class="order-list" v-if="list.length">
      <article v-for="item in list" :key="item.id" class="order-card">
        <div class="order-head">
          <div>
            <strong>{{ item.orderNo }}</strong>
            <p>{{ item.createdAt }}</p>
          </div>
          <div class="price">¥{{ item.totalAmount }}</div>
        </div>
        <div class="item-list">
          <div v-for="course in item.items" :key="course.id" class="item-row">
            <span>{{ course.courseTitle }}</span>
            <span>¥{{ course.unitPrice }}</span>
          </div>
        </div>
        <div class="order-foot">
          <span class="status">{{ item.orderStatus }} / {{ item.paymentStatus }} / {{ item.refund?.refundStatus || '无退款' }}</span>
          <div class="toolbar">
            <el-button plain @click="openDetail(item.id)">订单详情</el-button>
            <el-button v-if="item.orderStatus !== 'PAID'" type="primary" @click="payOrder(item.id)">立即支付</el-button>
            <el-button v-else-if="!item.refund || item.refund.refundStatus === 'REJECTED'" plain @click="openRefund(item)">申请退款</el-button>
          </div>
        </div>
      </article>
    </div>
    <el-empty v-else description="暂无订单" />

    <el-pagination
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      style="margin-top: 24px; justify-content: center"
      @current-change="(page) => { query.pageNo = page; loadData() }"
    />

    <el-drawer v-model="detailVisible" size="520px" title="订单详情">
      <div v-loading="detailLoading" class="detail-panel" v-if="currentOrder">
        <div class="detail-card">
          <span>订单号</span>
          <strong>{{ currentOrder.orderNo }}</strong>
        </div>
        <div class="detail-card">
          <span>订单状态</span>
          <strong>{{ currentOrder.orderStatus }} / {{ currentOrder.paymentStatus }}</strong>
        </div>
        <div class="detail-card">
          <span>支付方式</span>
          <strong>{{ currentOrder.paymentChannel || '未支付' }}</strong>
        </div>
        <div class="detail-card">
          <span>支付时间</span>
          <strong>{{ currentOrder.paidAt || '未支付' }}</strong>
        </div>
        <div class="detail-card">
          <span>备注</span>
          <strong>{{ currentOrder.remark || '无' }}</strong>
        </div>
        <div class="detail-card">
          <span>课程明细</span>
          <div class="detail-list">
            <div v-for="course in currentOrder.items" :key="course.id" class="item-row">
              <span>{{ course.courseTitle }}</span>
              <span>¥{{ course.unitPrice }}</span>
            </div>
          </div>
        </div>
        <div class="detail-card">
          <span>支付流水</span>
          <div class="detail-list" v-if="currentOrder.paymentLogs?.length">
            <div v-for="log in currentOrder.paymentLogs" :key="log.id" class="item-row">
              <span>{{ log.paymentChannel }} / {{ log.paymentStatus }}</span>
              <span>{{ log.transactionNo }}</span>
            </div>
          </div>
          <strong v-else>暂无支付流水</strong>
        </div>
        <div class="detail-card">
          <span>退款信息</span>
          <div v-if="currentOrder.refund" class="detail-list">
            <div class="item-row">
              <span>退款单号</span>
              <span>{{ currentOrder.refund.refundNo }}</span>
            </div>
            <div class="item-row">
              <span>退款状态</span>
              <span>{{ currentOrder.refund.refundStatus }}</span>
            </div>
            <div class="item-row">
              <span>退款金额</span>
              <span>¥{{ currentOrder.refund.refundAmount }}</span>
            </div>
            <div class="item-row">
              <span>退款原因</span>
              <span>{{ currentOrder.refund.refundReason }}</span>
            </div>
            <div class="item-row">
              <span>审批备注</span>
              <span>{{ currentOrder.refund.reviewRemark || '无' }}</span>
            </div>
            <div class="detail-list" v-if="currentOrder.refund.logs?.length">
              <div v-for="log in currentOrder.refund.logs" :key="log.id" class="item-row">
                <span>{{ log.actionType }} / {{ log.fromStatus }} -> {{ log.toStatus }}</span>
                <span>{{ log.operatorName }} / {{ log.createdAt }}</span>
              </div>
            </div>
          </div>
          <strong v-else>暂无退款记录</strong>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="refundVisible" title="申请退款" width="520px">
      <div class="dialog-form">
        <el-input
          v-model="refundForm.refundReason"
          type="textarea"
          :rows="4"
          placeholder="填写退款原因，说明课程问题或退款诉求"
        />
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.order-page {
  padding: 24px;
}

.page-head,
.toolbar,
.order-head,
.item-row,
.order-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.toolbar {
  flex-wrap: wrap;
}

.order-list {
  display: grid;
  gap: 16px;
}

.order-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 18px;
  background: #fff;
}

.order-head p,
.status {
  color: var(--muted);
}

.price {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
}

.item-list {
  display: grid;
  gap: 10px;
  margin: 16px 0;
}

.detail-panel,
.detail-list,
.dialog-form {
  display: grid;
  gap: 12px;
}

.detail-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.detail-card span,
.detail-card strong:last-child {
  word-break: break-all;
}

.detail-card span {
  display: block;
  color: var(--muted);
  margin-bottom: 8px;
}
</style>
