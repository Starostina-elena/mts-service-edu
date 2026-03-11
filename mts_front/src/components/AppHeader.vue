<script setup>
import { useAuth } from '@/auth/useAuth'
import { useRouter } from 'vue-router'

const { currentUser, isLoggedIn, isAdmin, logout } = useAuth()
const router = useRouter()

function doLogout() {
  logout()
  router.push('/')
}
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <router-link to="/" class="header__logo">
        <span class="header__logo-text">МТС</span>
      </router-link>

      <nav class="header__nav">
        <router-link to="/" class="header__link">Каталог</router-link>
        <template v-if="isAdmin">
          <router-link to="/admin/tariffs/new" class="header__link">Создать тариф</router-link>
          <router-link to="/admin/services/new" class="header__link">Создать услугу</router-link>
        </template>
      </nav>

      <div class="header__user">
        <template v-if="isLoggedIn">
          <span class="header__username">{{ currentUser.name }}</span>
          <span class="header__role">({{ currentUser.role }})</span>
          <button class="header__logout" @click="doLogout">Выйти</button>
        </template>
        <template v-else>
          <router-link to="/login" class="header__login-link">Войти</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 50;
}
.header__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.header__logo {
  text-decoration: none;
}
.header__logo-text {
  font-size: 24px;
  font-weight: 900;
  color: #e00;
}
.header__nav {
  display: flex;
  gap: 16px;
  flex: 1;
}
.header__link {
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  padding: 6px 12px;
  border-radius: 16px;
  transition: background .15s;
}
.header__link:hover {
  background: #f5f5f5;
}
.header__link.router-link-exact-active {
  background: #fff0f0;
  color: #e00;
}
.header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  white-space: nowrap;
}
.header__username {
  font-weight: 600;
}
.header__role {
  color: #888;
}
.header__logout {
  background: none;
  border: 1px solid #ddd;
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #555;
}
.header__logout:hover {
  background: #f5f5f5;
}
.header__login-link {
  text-decoration: none;
  color: #e00;
  font-weight: 600;
  font-size: 14px;
}
</style>
