<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/auth/useAuth'
import { fetchApplicationDetail, approveApplication, rejectApplication } from '@/api/applications'

const route = useRoute()
const router = useRouter()
const { currentUser, isManager } = useAuth()

const app = ref(null)
const loading = ref(false)
const error = ref(null)

const showRejectForm = ref(false)
const rejectReason = ref('')
const actionLoading = ref(false)
const actionError = ref(null)

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

async function doApprove() {
  actionLoading.value = true
  actionError.value = null
  try {
    const updated = await approveApplication(currentUser.value.id, app.value.id)
    app.value = { ...app.value, status: updated.status, updatedAt: updated.updatedAt }
  } catch (e) {
    actionError.value = e.message
  } finally {
    actionLoading.value = false
  }
}

async function doReject() {
  if (!rejectReason.value.trim()) return
  actionLoading.value = true
  actionError.value = null
  try {
    const updated = await rejectApplication(currentUser.value.id, app.value.id, rejectReason.value.trim())
    app.value = { ...app.value, status: updated.status, rejectReason: updated.rejectReason, updatedAt: updated.updatedAt }
    showRejectForm.value = false
    rejectReason.value = ''
  } catch (e) {
    actionError.value = e.message
  } finally {
    actionLoading.value = false
  }
}

function goBack() {
  router.push({ name: 'applications' })
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
    <button class="back-btn" @click="goBack">&larr; Назад к заявкам</button>

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
            <span class="detail__label">Пользователь</span>
            <span class="detail__value">ID {{ app.userId }}</span>
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

      <div v-if="actionError" class="page__error">{{ actionError }}</div>

      <div v-if="isManager && app.status === 'PENDING'" class="detail__actions">
        <template v-if="showRejectForm">
          <div class="reject-form">
            <input
              v-model="rejectReason"
              type="text"
              class="reject-form__input"
              placeholder="Причина отклонения..."
              @keyup.enter="doReject"
            />
            <div class="reject-form__btns">
              <button
                class="btn btn--danger"
                :disabled="!rejectReason.trim() || actionLoading"
                @click="doReject"
              >
                {{ actionLoading ? 'Отклонение...' : 'Подтвердить отклонение' }}
              </button>
              <button class="btn btn--secondary" @click="showRejectForm = false">Отмена</button>
            </div>
          </div>
        </template>
        <template v-else>
          <button
            class="btn btn--approve"
            :disabled="actionLoading"
            @click="doApprove"
          >
            {{ actionLoading ? 'Одобрение...' : 'Одобрить' }}
          </button>
          <button
            class="btn btn--reject"
            :disabled="actionLoading"
            @click="showRejectForm = true"
          >
            Отклонить
          </button>
        </template>
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
.detail__actions {
  padding: 20px 24px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.reject-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reject-form__input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}
.reject-form__input:focus {
  border-color: #e00;
}
.reject-form__btns {
  display: flex;
  gap: 8px;
}
.btn {
  padding: 10px 24px;
  border-radius: 20px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all .15s;
}
.btn:disabled {
  opacity: .5;
  cursor: default;
}
.btn--approve {
  background: #2e7d32;
  color: #fff;
}
.btn--approve:hover:not(:disabled) {
  background: #1b5e20;
}
.btn--reject {
  background: #fff;
  color: #c62828;
  border: 1px solid #c62828;
}
.btn--reject:hover:not(:disabled) {
  background: #fce4ec;
}
.btn--danger {
  background: #c62828;
  color: #fff;
}
.btn--danger:hover:not(:disabled) {
  background: #b71c1c;
}
.btn--secondary {
  background: #f5f5f5;
  color: #333;
}
.btn--secondary:hover {
  background: #eee;
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
