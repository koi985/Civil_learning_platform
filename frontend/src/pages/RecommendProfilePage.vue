<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const data = ref({
  portrait: { labels: [] },
  preferenceSubjects: [],
  weaknessFocus: [],
  recommendReasonStats: []
})

const resourceLabelMap = {
  COURSE: '课程推荐',
  PRACTICE: '刷题推荐',
  EXAM: '模考推荐',
  NEWS: '资讯推荐'
}

const portraitConclusion = computed(() => {
  const portrait = data.value.portrait || {}
  return `${portrait.targetProvince || '全国'} ${portrait.examType || '公考'} / ${portrait.preferredSubject || '综合能力'}主攻 / ${portrait.weakSubject || '待补强'}待补强`
})

const portraitDecision = computed(() => {
  const portrait = data.value.portrait || {}
  return `推荐会优先偏向 ${portrait.weakSubject || '薄弱项'} 补强、${portrait.targetProvince || '目标地区'} 相关资源，以及 ${portrait.preferredSubject || '主攻科目'} 延展训练。`
})

const portraitStatusCards = computed(() => {
  const portrait = data.value.portrait || {}
  return [
    {
      label: '目标地区',
      value: portrait.targetProvince || '全国',
      note: `优先命中 ${portrait.targetProvince || '目标地区'} 的课程、模考和资讯`
    },
    {
      label: '主攻科目',
      value: portrait.preferredSubject || '综合能力',
      note: `最近活跃 ${portrait.activeDays || 0} 天，保持主攻方向连续性`
    },
    {
      label: '薄弱项',
      value: portrait.weakSubject || '待分析',
      note: '建议优先补强，推荐会提高该方向的曝光权重'
    },
    {
      label: '强项',
      value: portrait.strongSubject || '待分析',
      note: '保持训练频率，用于稳定得分盘'
    }
  ]
})

const groupedPortraitLabels = computed(() => {
  const labels = data.value.portrait?.labels || []
  const groups = {
    basic: [],
    learning: [],
    system: []
  }
  labels.forEach((label) => {
    if (label.includes('备考') || label.includes('主攻')) {
      groups.basic.push(label)
      return
    }
    if (label.includes('错题') || label.includes('继续学习')) {
      groups.learning.push(label)
      return
    }
    groups.system.push(label)
  })
  return [
    { title: '基础画像标签', items: groups.basic },
    { title: '学习状态标签', items: groups.learning },
    { title: '推荐系统标签', items: groups.system }
  ].filter((group) => group.items.length)
})

const recommendationBasis = computed(() => {
  const portrait = data.value.portrait || {}
  const total = (data.value.recommendReasonStats || []).reduce((sum, item) => sum + Number(item.count || 0), 0)
  return (data.value.recommendReasonStats || []).map((item) => {
    const label = resourceLabelMap[item.resourceType] || item.resourceType
    let description = `${label}当前命中 ${item.count} 条。`
    if (item.resourceType === 'COURSE') {
      description = `${label}占比更高，主要因为 ${portrait.weakSubject || '薄弱项'} 补强和 ${portrait.targetProvince || '地区'} 方向命中较多。`
    } else if (item.resourceType === 'PRACTICE') {
      description = `${label}用于把 ${portrait.weakSubject || '薄弱项'} 转成直接训练入口，缩短补强路径。`
    } else if (item.resourceType === 'EXAM') {
      description = `${label}主要承担阶段检验作用，和当前考试类型、地区方向联动。`
    } else if (item.resourceType === 'NEWS') {
      description = `${label}用于补充考试动态、热点素材和最新政策信息。`
    }
    return {
      ...item,
      label,
      total,
      description
    }
  })
})

const readablePreferenceTags = computed(() => {
  return (data.value.preferenceSubjects || []).map((item) => {
    const tags = String(item.label || '')
      .split(/[、,，|/]+/)
      .map((part) => part.trim())
      .filter(Boolean)
    return {
      ...item,
      tags: tags.length ? tags : [item.label]
    }
  })
})

const portraitSources = computed(() => {
  const portrait = data.value.portrait || {}
  return [
    {
      title: '练习记录',
      detail: `已纳入 ${portrait.practiceCount || 0} 次练习结果，用于识别强弱项和主攻方向。`
    },
    {
      title: '模考表现',
      detail: `已纳入 ${portrait.examCount || 0} 次模考结果，用于调整阶段检验类推荐。`
    },
    {
      title: '课程学习',
      detail: `累计学习 ${portrait.totalStudyHours || 0} 小时，继续学习轨迹会影响课程优先级。`
    },
    {
      title: '反馈回流',
      detail: `系统已记录 ${portrait.feedbackCount || 0} 次推荐反馈，用于降低不相关内容。`
    }
  ]
})

const portraitTrendCards = computed(() => {
  const portrait = data.value.portrait || {}
  return [
    {
      title: `${portrait.weakSubject || '薄弱项'}补强优先`,
      description: `${portrait.weakSubject || '薄弱项'} 仍处于优先补强位，建议先做专项训练，再承接课程和模考。`
    },
    {
      title: `${portrait.strongSubject || '强项'}保持稳定`,
      description: `${portrait.strongSubject || '强项'} 当前是优势项，更适合维持频率而不是过度堆时长。`
    },
    {
      title: `近 ${portrait.activeDays || 0} 天保持活跃`,
      description: '活跃天数会直接影响继续学习、协同行为和阶段推荐的刷新速度。'
    }
  ]
})

const quickActions = computed(() => {
  const portrait = data.value.portrait || {}
  return [
    {
      title: `去补强 ${portrait.weakSubject || '薄弱项'}`,
      description: '直接进入搜题页，先把短板转成专项训练。',
      path: `/search?subjectName=${encodeURIComponent(portrait.weakSubject || '')}`
    },
    {
      title: `看 ${portrait.targetProvince || '目标地区'} 课程`,
      description: '回到推荐中心，优先看与你当前地区方向匹配的课程。',
      path: '/recommend'
    },
    {
      title: '进入专项模考',
      description: '用模考验证当前学习策略是否有效。',
      path: '/exam'
    },
    {
      title: '调整偏好',
      description: '进入用户中心，更新地区、考试类型和主攻科目。',
      path: '/user-center'
    }
  ]
})

async function loadData() {
  loading.value = true
  try {
    data.value = await request.get('/api/recommend/profile')
  } finally {
    loading.value = false
  }
}

function handleRegionChanged() {
  loadData()
}

onMounted(() => {
  loadData()
  window.addEventListener('civil-region-changed', handleRegionChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('civil-region-changed', handleRegionChanged)
})
</script>

<template>
  <div class="page-shell profile-page" v-loading="loading">
    <section class="glass-card hero-panel">
      <div class="hero-head">
        <div class="hero-copy">
          <div class="section-title">我的画像</div>
          <p class="hero-lead">{{ portraitConclusion }}</p>
          <p class="muted hero-summary">{{ portraitDecision }}</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/recommend')">返回推荐中心</el-button>
          <el-button plain @click="router.push('/search')">去搜题补强</el-button>
          <el-button plain @click="router.push('/exam')">去模考验证</el-button>
        </div>
      </div>
      <div class="status-grid">
        <article v-for="item in portraitStatusCards" :key="item.label" class="status-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </article>
      </div>
      <div class="label-groups">
        <div v-for="group in groupedPortraitLabels" :key="group.title" class="label-group">
          <span class="group-title">{{ group.title }}</span>
          <div class="tag-row">
            <el-tag v-for="item in group.items" :key="`${group.title}-${item}`" round effect="plain">{{ item }}</el-tag>
          </div>
        </div>
      </div>
    </section>

    <section class="grid-2">
      <div class="glass-card panel">
        <div class="section-title small">推荐依据</div>
        <div class="stack">
          <article v-for="item in recommendationBasis" :key="item.resourceType" class="explain-card">
            <div class="explain-head">
              <strong>{{ item.label }}</strong>
              <span>{{ item.count }} 条</span>
            </div>
            <p class="muted">{{ item.description }}</p>
          </article>
        </div>
      </div>
      <div class="glass-card panel">
        <div class="section-title small">推荐标签</div>
        <div class="stack">
          <article v-for="item in readablePreferenceTags" :key="item.label" class="explain-card">
            <div class="preference-tags">
              <span v-for="tag in item.tags" :key="`${item.label}-${tag}`">{{ tag }}</span>
            </div>
            <p class="muted">本次推荐中命中 {{ item.count }} 次</p>
          </article>
        </div>
      </div>
    </section>

    <section class="grid-2">
      <div class="glass-card panel">
        <div class="section-title small">画像来源</div>
        <div class="stack">
          <article v-for="item in portraitSources" :key="item.title" class="source-card">
            <strong>{{ item.title }}</strong>
            <p class="muted">{{ item.detail }}</p>
          </article>
        </div>
      </div>
      <div class="glass-card panel">
        <div class="section-title small">画像趋势</div>
        <div class="stack">
          <article v-for="item in portraitTrendCards" :key="item.title" class="source-card">
            <strong>{{ item.title }}</strong>
            <p class="muted">{{ item.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">薄弱项建议</div>
      <div class="stack">
        <div v-for="item in data.weaknessFocus" :key="item.title" class="focus-card">
          <div>
            <strong>{{ item.title }}</strong>
            <p class="muted">{{ item.reason }}</p>
          </div>
          <el-button type="primary" plain @click="router.push(item.path)">立即进入</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">下一步动作</div>
      <div class="action-grid">
        <article v-for="item in quickActions" :key="item.title" class="action-card">
          <strong>{{ item.title }}</strong>
          <p class="muted">{{ item.description }}</p>
          <el-button type="primary" plain @click="router.push(item.path)">立即前往</el-button>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-page,
.grid-2,
.stack,
.status-grid,
.action-grid {
  display: grid;
  gap: 20px;
}

.hero-panel,
.panel {
  padding: 28px;
}

.hero-panel {
  display: grid;
  gap: 24px;
}

.hero-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: start;
}

.hero-copy {
  display: grid;
  gap: 10px;
}

.hero-lead {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.3;
  font-weight: 800;
}

.hero-summary {
  margin: 0;
  max-width: 960px;
  font-size: 16px;
  line-height: 1.7;
}

.hero-actions,
.tag-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-actions {
  justify-content: flex-end;
  align-content: start;
}

.grid-2 {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.status-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.status-card,
.explain-card,
.source-card,
.focus-card,
.action-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 18px;
  background: #fff;
}

.status-card span,
.group-title,
.muted {
  color: var(--muted);
}

.status-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 247, 255, 0.9));
  box-shadow: 0 10px 24px rgba(30, 64, 175, 0.06);
}

.status-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.2;
}

.status-card p,
.source-card p,
.action-card p,
.explain-card p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.label-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.label-group {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.85);
}

.group-title {
  font-size: 13px;
  font-weight: 700;
}

.explain-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.explain-head strong,
.source-card strong,
.action-card strong,
.focus-card strong {
  color: #0f172a;
  font-size: 16px;
}

.explain-head span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.preference-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preference-tags span {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.focus-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.action-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.action-card {
  display: grid;
  gap: 14px;
  align-content: start;
}

@media (max-width: 1100px) {
  .hero-head {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-panel,
  .panel {
    padding: 20px;
  }

  .hero-lead {
    font-size: 22px;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .focus-card {
    display: grid;
  }
}
</style>
