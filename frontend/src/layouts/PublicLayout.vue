<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MainHeader from '../components/MainHeader.vue'
import TeacherBackButton from '../components/TeacherBackButton.vue'

const route = useRoute()
const assistantVisiblePaths = new Set(['/', '/course', '/search', '/news', '/recommend', '/classroom'])
const showAssistant = computed(() => assistantVisiblePaths.has(route.path))
const assistantReady = ref(false)
const CivilAssistantFloat = defineAsyncComponent(() => import('../components/CivilAssistantFloat.vue'))
let assistantIdleHandle = null

function scheduleAssistant() {
  if (!showAssistant.value || assistantReady.value || assistantIdleHandle) {
    return
  }
  const load = () => {
    assistantReady.value = true
    assistantIdleHandle = null
  }
  if ('requestIdleCallback' in window) {
    assistantIdleHandle = window.requestIdleCallback(load, { timeout: 2500 })
  } else {
    assistantIdleHandle = window.setTimeout(load, 1200)
  }
}

watch(showAssistant, scheduleAssistant, { immediate: true })

onBeforeUnmount(() => {
  if (!assistantIdleHandle) {
    return
  }
  if ('cancelIdleCallback' in window) {
    window.cancelIdleCallback(assistantIdleHandle)
  } else {
    window.clearTimeout(assistantIdleHandle)
  }
})
</script>

<template>
  <div>
    <MainHeader />
    <TeacherBackButton v-if="$route.meta.teacherBack" />
    <router-view />
    <CivilAssistantFloat v-if="showAssistant && assistantReady" />
  </div>
</template>
