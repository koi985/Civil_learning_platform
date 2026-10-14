<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const captchaCanvasRef = ref(null)
const captchaCode = ref('')
const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

function randomCode(length = 4) {
  const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

function drawCaptcha() {
  const canvas = captchaCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height
  captchaCode.value = randomCode()
  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 24; i += 1) {
    ctx.fillStyle = `rgba(${80 + Math.random() * 120}, ${90 + Math.random() * 110}, ${100 + Math.random() * 100}, 0.35)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, Math.random() * 2 + 1, 0, Math.PI * 2)
    ctx.fill()
  }

  for (let i = 0; i < 4; i += 1) {
    ctx.strokeStyle = `rgba(${40 + Math.random() * 100}, ${80 + Math.random() * 100}, ${120 + Math.random() * 100}, 0.55)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  captchaCode.value.split('').forEach((char, index) => {
    ctx.save()
    ctx.translate(20 + index * 24, 31)
    ctx.rotate((Math.random() - 0.5) * 0.45)
    ctx.font = `${24 + Math.floor(Math.random() * 5)}px Arial`
    ctx.fillStyle = ['#1d4ed8', '#0f766e', '#f97316', '#374151'][index % 4]
    ctx.fillText(char, 0, 0)
    ctx.restore()
  })
}

function refreshCaptcha() {
  form.captcha = ''
  drawCaptcha()
}

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (!form.captcha.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (form.captcha.trim().toUpperCase() !== captchaCode.value) {
    ElMessage.error('验证码不正确')
    refreshCaptcha()
    return
  }

  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password
    })
    ElMessage.success('登录成功')
    const roles = authStore.roles
    if (roles.includes('ADMIN')) {
      router.push('/admin/dashboard')
    } else if (roles.includes('TEACHER')) {
      router.push('/teacher/dashboard')
    } else {
      router.push('/')
    }
  } catch (error) {
    ElMessage.error(error.message)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  drawCaptcha()
})
</script>

<template>
  <div class="auth-shell page-shell">
    <div class="auth-card glass-card">
      <h1>登录平台</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="图形验证码">
          <div class="captcha-row">
            <el-input v-model="form.captcha" maxlength="4" placeholder="请输入验证码" @keyup.enter="submit" />
            <button class="captcha-canvas-button" type="button" title="点击刷新验证码" @click="refreshCaptcha">
              <canvas ref="captchaCanvasRef" width="128" height="44"></canvas>
            </button>
          </div>
        </el-form-item>
        <div class="auth-actions">
          <el-button type="primary" :loading="loading" @click="submit">登录</el-button>
          <el-button @click="router.push('/register')">去注册</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-shell {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  max-width: none;
  min-height: calc(100vh - 96px);
  margin: 0;
  padding: 64px 16px 72px;
  overflow: hidden;
  background:
    linear-gradient(118deg, rgba(37, 99, 235, 0.42) 0%, transparent 28%, rgba(225, 29, 72, 0.32) 52%, transparent 74%),
    linear-gradient(32deg, transparent 6%, rgba(250, 204, 21, 0.28) 22%, transparent 42%, rgba(59, 130, 246, 0.28) 72%, transparent 92%),
    linear-gradient(155deg, #050b18 0%, #07152a 36%, #120c1f 67%, #050916 100%);
  background-size: 240% 240%, 220% 220%, 100% 100%;
  animation: auth-background-shift 9s ease-in-out infinite alternate;
}

.auth-shell::before,
.auth-shell::after {
  position: absolute;
  inset: -16%;
  content: "";
  pointer-events: none;
}

.auth-shell::before {
  background:
    radial-gradient(ellipse at 16% 22%, rgba(37, 99, 235, 0.64), transparent 35%),
    radial-gradient(ellipse at 78% 18%, rgba(239, 68, 68, 0.5), transparent 35%),
    radial-gradient(ellipse at 58% 84%, rgba(250, 204, 21, 0.42), transparent 32%),
    linear-gradient(112deg, transparent 14%, rgba(96, 165, 250, 0.26) 34%, rgba(251, 191, 36, 0.2) 48%, transparent 66%);
  filter: blur(22px);
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1.04);
  animation: auth-aurora-drift 8s ease-in-out infinite alternate;
}

.auth-shell::after {
  background:
    linear-gradient(105deg, transparent 5%, rgba(255, 255, 255, 0.18) 27%, transparent 49%),
    linear-gradient(rgba(147, 197, 253, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(251, 191, 36, 0.09) 1px, transparent 1px),
    radial-gradient(circle, rgba(248, 113, 113, 0.2) 1px, transparent 1.5px);
  background-size: 100% 100%, 54px 54px, 54px 54px, 27px 27px;
  mask-image: linear-gradient(180deg, transparent 0%, #000 18%, #000 82%, transparent 100%);
  opacity: 0.56;
  animation: auth-grid-breathe 7s ease-in-out infinite alternate;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: min(460px, 100%);
  padding: 28px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(186, 230, 253, 0.42);
  box-shadow:
    0 34px 90px rgba(2, 8, 23, 0.38),
    0 0 0 1px rgba(255, 255, 255, 0.32) inset,
    0 0 42px rgba(59, 130, 246, 0.2),
    0 0 72px rgba(250, 204, 21, 0.08);
  backdrop-filter: blur(22px) saturate(132%);
}

.auth-card h1 {
  margin-top: 0;
  color: #0f172a;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.64);
}

.auth-card p {
  color: var(--muted);
  line-height: 1.7;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 12px;
  width: 100%;
}

.captcha-canvas-button {
  width: 128px;
  height: 44px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.captcha-canvas-button canvas {
  display: block;
}

.auth-actions {
  display: grid;
  gap: 12px;
}

.auth-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

@keyframes auth-background-shift {
  from {
    background-position: 0% 42%, 100% 28%, center;
  }

  to {
    background-position: 100% 62%, 0% 72%, center;
  }
}

@keyframes auth-aurora-drift {
  from {
    transform: translate3d(-4%, -3%, 0) scale(1.04) rotate(-1deg);
  }

  to {
    transform: translate3d(4%, 3%, 0) scale(1.12) rotate(1deg);
  }
}

@keyframes auth-grid-breathe {
  from {
    opacity: 0.42;
    transform: translate3d(-18px, -14px, 0);
  }

  to {
    opacity: 0.66;
    transform: translate3d(18px, 14px, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-shell,
  .auth-shell::before,
  .auth-shell::after {
    animation: none;
  }
}

@media (max-width: 520px) {
  .auth-shell {
    padding: 42px 12px 48px;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }

  .captcha-canvas-button {
    width: 100%;
  }

  .captcha-canvas-button canvas {
    margin: 0 auto;
  }
}
</style>
