<script setup>
import { onMounted } from 'vue'
import AppHeader from './components/AppHeader.vue'
import { useAuth } from '@/auth/useAuth'

const { restore } = useAuth()
onMounted(() => restore())
</script>

<template>
  <AppHeader />
  <router-view />
</template>
