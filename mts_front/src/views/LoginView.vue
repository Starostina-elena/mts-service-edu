<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/auth/useAuth'
import { MOCK_USERS } from '@/auth/mockUsers'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const ROLE_LABELS = {
  USER: 'Пользователь',
  MANAGER: 'Менеджер',
  ADMIN: 'Администратор',
}

const ROLE_COLORS = {
  USER: '#4caf50',
  MANAGER: '#2196f3',
  ADMIN: '#e00',
}

function pickUser(user) {
  login(user)
  const redirect = route.query.redirect || '/'
  router.push(redirect)
}
</script>

<template>
  <div class="login-page">
    <h1 class="login-page__title">Выберите пользователя</h1>
    <p class="login-page__subtitle">Демо-авторизация для тестирования</p>

    <div class="login-page__grid">
      <div
        v-for="user in MOCK_USERS"
        :key="user.id"
        class="user-card"
        @click="pickUser(user)"
      >
        <div class="user-card__avatar" :style="{ background: ROLE_COLORS[user.role] }">
          {{ user.name[0] }}
        </div>
        <div class="user-card__info">
          <div class="user-card__name">{{ user.name }}</div>
          <div class="user-card__role" :style="{ color: ROLE_COLORS[user.role] }">
            {{ ROLE_LABELS[user.role] }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  max-width: 600px;
  margin: 60px auto;
  padding: 0 16px;
  text-align: center;
}
.login-page__title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}
.login-page__subtitle {
  color: #888;
  margin-bottom: 32px;
}
.login-page__grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  transition: box-shadow .2s, transform .15s;
}
.user-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  transform: translateY(-2px);
}
.user-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}
.user-card__info {
  text-align: left;
}
.user-card__name {
  font-size: 16px;
  font-weight: 600;
}
.user-card__role {
  font-size: 13px;
  font-weight: 500;
  margin-top: 2px;
}
</style>
