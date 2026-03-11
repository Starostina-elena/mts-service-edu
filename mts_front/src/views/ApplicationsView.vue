<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/auth/useAuth'
import { fetchApplications, approveApplication, rejectApplication } from '@/api/applications'

const router = useRouter()
const { currentUser, isManager } = useAuth()

const STATUSES = [
  { value: '', label: 'Все' },
  { value: 'PENDING', label: 'Ожидают' },
  { value: 'APPROVED', label: 'Одобрены' },
  { value: 'REJECTED', label: 'Отклонены' },
  { value: 'CONNECTED', label: 'Подключены' },
]

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

const statusFilter = ref('')
const applications = ref([])
const nextCursor = ref(null)
const loading = ref(false)
const error = ref(null)

const rejectingId = ref(null)
const rejectReason = ref('')
const actionLoading = ref(null)
const actionError = ref(null)

async function load(append = false) {
  loading.value = true
  error.value = null
  try {
    const opts = { limit: 20 }
    if (statusFilter.value) opts.status = statusFilter.value
    if (append && nextCursor.value) opts.after = nextCursor.value
    const data = await fetchApplications(opts)
    applications.value = append ? [...applications.value, ...data.items] : data.items
    nextCursor.value = data.nextCursor ?? null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  router.push({ name: 'application-detail', params: { id } })
}

async function doApprove(app) {
  actionLoading.value = app.id
  actionError.value = null
  try {
    const updated = await approveApplication(currentUser.value.id, app.id)
    const idx = applications.value.findIndex(a => a.id === app.id)
    if (idx !== -1) applications.value[idx] = updated
  } catch (e) {
    actionError.value = `${app.id}: ${e.message}`
  } finally {
    actionLoading.value = null
  }
}

function startReject(app) {
  rejectingId.value = app.id
  rejectReason.value = ''
}

function cancelReject() {
  rejectingId.value = null
  rejectReason.value = ''
}

async function doReject(app) {
  if (!rejectReason.value.trim()) return
  actionLoading.value = app.id
  actionError.value = null
  try {
    const updated = await rejectApplication(currentUser.value.id, app.id, rejectReason.value.trim())
    const idx = applications.value.findIndex(a => a.id === app.id)
    if (idx !== -1) applications.value[idx] = updated
    rejectingId.value = null
    rejectReason.value = ''
  } catch (e) {
    actionError.value = `${app.id}: ${e.message}`
  } finally {
    actionLoading.value = null
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

watch(statusFilter, () => {
  applications.value = []
  nextCursor.value = null
  load()
})

load()
</script>

<template>
  <div class="page">
    <div class="page__header">
      <h1 class="page__title">Заявки</h1>
    </div>

    <nav class="tabs">
      <button
        v-for="s in STATUSES"
        :key="s.value"
        :class="['tab', { 'tab--active': statusFilter === s.value }]"
        @click="statusFilter = s.value"
      >
        {{ s.label }}
      </button>
    </nav>

    <div v-if="error" class="page__error">{{ error }}</div>
    <div v-if="actionError" class="page__error">{{ actionError }}</div>

    <div v-if="applications.length" class="list">
      <div
        v-for="app in applications"
        :key="app.id"
        class="card"
      >
        <div class="card__header" @click="openDetail(app.id)">
          <span class="card__id">#{{ app.id }}</span>
          <span :class="['badge', STATUS_CLASSES[app.status]]">{{ STATUS_LABELS[app.status] || app.status }}</span>
        </div>

        <div class="card__body" @click="openDetail(app.id)">
          <div class="card__row">
            <span class="card__label">Тариф:</span>
            <span class="card__value">{{ app.tariffName }}</span>
          </div>
          <div class="card__row">
            <span class="card__label">Адрес:</span>
            <span class="card__value">{{ app.address }}</span>
          </div>
          <div class="card__row">
            <span class="card__label">Пользователь:</span>
            <span class="card__value">ID {{ app.userId }}</span>
          </div>
          <div class="card__row">
            <span class="card__label">Дата:</span>
            <span class="card__value">{{ formatDate(app.createdAt) }}</span>
          </div>
          <div v-if="app.rejectReason" class="card__row">
            <span class="card__label">Причина отказа:</span>
            <span class="card__value card__value--reject">{{ app.rejectReason }}</span>
          </div>
        </div>

        <div v-if="isManager && app.status === 'PENDING'" class="card__actions">
          <template v-if="rejectingId === app.id">
            <div class="reject-form">
              <input
                v-model="rejectReason"
                type="text"
                class="reject-form__input"
                placeholder="Причина отклонения..."
                @keyup.enter="doReject(app)"
              />
              <div class="reject-form__btns">
                <button
                  class="btn btn--danger"
                  :disabled="!rejectReason.trim() || actionLoading === app.id"
                  @click="doReject(app)"
                >
                  {{ actionLoading === app.id ? 'Отклонение...' : 'Подтвердить' }}
                </button>
                <button class="btn btn--secondary" @click="cancelReject">Отмена</button>
              </div>
            </div>
          </template>
          <template v-else>
            <button
              class="btn btn--approve"
              :disabled="actionLoading === app.id"
              @click="doApprove(app)"
            >
              {{ actionLoading === app.id ? 'Одобрение...' : 'Одобрить' }}
            </button>
            <button
              class="btn btn--reject"
              :disabled="actionLoading === app.id"
              @click="startReject(app)"
            >
              Отклонить
            </button>
          </template>
        </div>
      </div>
    </div>

    <div v-if="loading" class="page__loading">Загрузка...</div>

    <div v-if="!loading && !applications.length && !error" class="page__empty">
      Заявки не найдены
    </div>

    <button
      v-if="nextCursor && !loading"
      class="btn btn--load-more"
      @click="load(true)"
    >
      Загрузить ещё
    </button>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page__title {
  font-size: 36px;
  font-weight: 800;
  margin: 0;
}
.tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 24px;
}
.tab {
  white-space: nowrap;
  padding: 10px 20px;
  border-radius: 24px;
  border: 1px solid #eee;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all .15s;
}
.tab:hover {
  background: #f5f5f5;
}
.tab--active {
  background: #e00;
  color: #fff;
  border-color: #e00;
}
.list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  transition: box-shadow .15s;
}
.card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,.08);
}
.card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 8px;
  cursor: pointer;
}
.card__id {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}
.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
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
.card__body {
  padding: 0 16px 12px;
  cursor: pointer;
}
.card__row {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}
.card__label {
  color: #888;
  white-space: nowrap;
}
.card__value {
  color: #333;
}
.card__value--reject {
  color: #c62828;
}
.card__actions {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.reject-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reject-form__input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 13px;
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
  padding: 8px 20px;
  border-radius: 20px;
  border: none;
  font-size: 13px;
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
.btn--load-more {
  display: block;
  margin: 24px auto;
  padding: 12px 32px;
  border-radius: 24px;
  border: 1px solid #ddd;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
}
.btn--load-more:hover {
  background: #f5f5f5;
}
.page__loading,
.page__empty {
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
