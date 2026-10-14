<script setup>
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import GlobalBroadcastBar from './components/GlobalBroadcastBar.vue'

const route = useRoute()
const isMobileRoute = computed(() => route.path.startsWith('/m'))

watchEffect(() => {
  document.body.classList.toggle('mobile-route', isMobileRoute.value)
})
</script>

<template>
  <div>
    <GlobalBroadcastBar v-if="!isMobileRoute" />
    <router-view />
  </div>
</template>
