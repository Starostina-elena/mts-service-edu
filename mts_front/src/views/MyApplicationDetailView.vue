<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchApplicationDetail } from '@/api/applications'

const route = useRoute()
const router = useRouter()

const app = ref(null)
const loading = ref(false)
const error = ref(null)

const STATUS_LABELS = {
  PENDING: 'Ожидает',
  APPROVED: 'Одобрена',
  REJECTED: 'Отклонена',
  CONNECTED: 'Подключена',
}

const STATUS_CLASSES = {
  PENDING: 'badge--pending',
  APPROVED: 'badge--approved',
  REJECTED: 'badge--rejected',
  CONNECTED: 'badge--connected',
}

async function load() {
  loading.value = true
  error.value = null
  try {
    app.value = await fetchApplicationDetail(route.params.id)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'my-applications' })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function formatPrice(price) {
  if (price == null) return ''
  return new Intl.NumberFormat('ru-RU').format(price) + ' \u20BD'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <button class="back-btn" @click="goBack">&larr; Назад к моим заявкам</button>

    <div v-if="loading" class="page__loading">Загрузка...</div>
    <div v-if="error" class="page__error">{{ error }}</div>

    <div v-if="app" class="detail">
      <div class="detail__header">
        <h1 class="detail__title">Заявка #{{ app.id }}</h1>
        <span :class="['badge', STATUS_CLASSES[app.status]]">{{ STATUS_LABELS[app.status] || app.status }}</span>
      </div>

      <div class="detail__section">
        <h2 class="detail__section-title">Основная информация</h2>
        <div class="detail__grid">
          <div class="detail__field">
            <span class="detail__label">Тариф</span>
            <span class="detail__value">{{ app.tariffName }}</span>
          </div>
          <div class="detail__field">
            <span class="detail__label">Адрес</span>
            <span class="detail__value">{{ app.address }}</span>
          </div>
          <div class="detail__field">
            <span class="detail__label">Дата создания</span>
            <span class="detail__value">{{ formatDate(app.createdAt) }}</span>
          </div>
          <div class="detail__field">
            <span class="detail__label">Последнее обновление</span>
            <span class="detail__value">{{ formatDate(app.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <div class="detail__section">
        <h2 class="detail__section-title">Проверки</h2>
        <div class="detail__grid">
          <div class="detail__field">
            <span class="detail__label">Паспорт верифицирован</span>
            <span :class="['detail__value', app.passportVerified ? 'detail__value--ok' : 'detail__value--no']">
              {{ app.passportVerified ? 'Да' : 'Нет' }}
            </span>
          </div>
          <div class="detail__field">
            <span class="detail__label">Техническая возможность</span>
            <span :class="['detail__value', app.technicalFeasibility ? 'detail__value--ok' : 'detail__value--no']">
              {{ app.technicalFeasibility ? 'Да' : 'Нет' }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="app.additionalServices && app.additionalServices.length" class="detail__section">
        <h2 class="detail__section-title">Дополнительные услуги</h2>
        <div class="services-list">
          <div v-for="svc in app.additionalServices" :key="svc.id" class="service-item">
            <span class="service-item__name">{{ svc.name }}</span>
            <span class="service-item__price">{{ formatPrice(svc.price) }}</span>
          </div>
        </div>
      </div>

      <div v-if="app.rejectReason" class="detail__section">
        <h2 class="detail__section-title">Причина отклонения</h2>
        <p class="detail__reject-reason">{{ app.rejectReason }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px;
}
.back-btn {
  background: none;
  border: none;
  color: #e00;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 20px;
}
.back-btn:hover {
  text-decoration: underline;
}
.detail {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
}
.detail__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #eee;
}
.detail__title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
}
.badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
}
.badge--pending {
  background: #fff3e0;
  color: #e65100;
}
.badge--approved {
  background: #e8f5e9;
  color: #2e7d32;
}
.badge--rejected {
  background: #fce4ec;
  color: #c62828;
}
.badge--connected {
  background: #e3f2fd;
  color: #1565c0;
}
.detail__section {
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}
.detail__section:last-child {
  border-bottom: none;
}
.detail__section-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 12px;
  color: #333;
}
.detail__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.detail__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail__label {
  font-size: 12px;
  color: #888;
  text-transform: uppercase;
  letter-spacing: .5px;
}
.detail__value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.detail__value--ok {
  color: #2e7d32;
}
.detail__value--no {
  color: #c62828;
}
.detail__reject-reason {
  font-size: 14px;
  color: #c62828;
  margin: 0;
  line-height: 1.5;
}
.services-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 14px;
}
.service-item__name {
  font-weight: 500;
  color: #333;
}
.service-item__price {
  color: #e00;
  font-weight: 600;
}
.page__loading {
  text-align: center;
  padding: 40px;
  color: #888;
}
.page__error {
  background: #fff0f0;
  color: #c00;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}
</style>
