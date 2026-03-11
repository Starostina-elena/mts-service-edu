<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/auth/useAuth'
import { fetchTariffDetail, fetchServices } from '@/api/catalog'
import { createApplication } from '@/api/applications'

const route = useRoute()
const router = useRouter()
const { currentUser } = useAuth()

const tariffId = computed(() => Number(route.params.id))
const cityId = computed(() => route.query.cityId ? Number(route.query.cityId) : null)

const tariff = ref(null)
const allServices = ref([])
const loadingTariff = ref(false)
const loadingServices = ref(false)
const loadError = ref(null)

const address = ref('')
const selectedServiceIds = ref([])
const submitting = ref(false)
const submitError = ref(null)
const submitted = ref(false)
const createdApp = ref(null)

const totalPrice = computed(() => {
  if (!tariff.value) return 0
  let total = Number(tariff.value.price ?? tariff.value.basePrice ?? 0)
  for (const svc of allServices.value) {
    if (selectedServiceIds.value.includes(svc.id)) {
      total += Number(svc.price ?? 0)
    }
  }
  return total
})

function formatPrice(p) {
  if (p == null) return ''
  return new Intl.NumberFormat('ru-RU').format(p) + ' \u20BD'
}

function toggleService(id) {
  const idx = selectedServiceIds.value.indexOf(id)
  if (idx === -1) {
    selectedServiceIds.value.push(id)
  } else {
    selectedServiceIds.value.splice(idx, 1)
  }
}

async function loadData() {
  loadingTariff.value = true
  loadingServices.value = true
  loadError.value = null
  try {
    const [t, svcs] = await Promise.all([
      fetchTariffDetail(tariffId.value, cityId.value),
      fetchServices(),
    ])
    tariff.value = t
    allServices.value = svcs
  } catch (e) {
    loadError.value = e.message
  } finally {
    loadingTariff.value = false
    loadingServices.value = false
  }
}

async function submit() {
  if (!address.value.trim()) return
  submitting.value = true
  submitError.value = null
  try {
    const result = await createApplication(currentUser.value.id, {
      tariffId: tariffId.value,
      cityId: cityId.value,
      address: address.value.trim(),
      additionalServiceIds: selectedServiceIds.value,
    })
    createdApp.value = result
    submitted.value = true
  } catch (e) {
    submitError.value = e.message
  } finally {
    submitting.value = false
  }
}

function goToMyApps() {
  router.push({ name: 'my-applications' })
}

function goToCatalog() {
  router.push({ name: 'catalog' })
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <button class="back-btn" @click="goToCatalog">&larr; Назад к каталогу</button>

    <div v-if="loadingTariff" class="page__loading">Загрузка...</div>
    <div v-if="loadError" class="page__error">{{ loadError }}</div>

    <!-- Success state -->
    <div v-if="submitted" class="success">
      <div class="success__icon">&#10003;</div>
      <h1 class="success__title">Заявка подана</h1>
      <p class="success__text">
        Ваша заявка #{{ createdApp?.id }} на подключение тарифа
        <strong>{{ tariff?.name }}</strong> успешно создана.
      </p>
      <p class="success__text">Статус заявки можно отслеживать в разделе «Мои заявки».</p>
      <div class="success__actions">
        <button class="btn btn--primary" @click="goToMyApps">Мои заявки</button>
        <button class="btn btn--secondary" @click="goToCatalog">В каталог</button>
      </div>
    </div>

    <!-- Form -->
    <template v-else-if="tariff && !loadingTariff">
      <h1 class="page__title">Подключение тарифа</h1>

      <div class="tariff-summary">
        <h2 class="tariff-summary__name">{{ tariff.name }}</h2>
        <span class="tariff-summary__category">{{ tariff.category }}</span>
        <p class="tariff-summary__price">{{ formatPrice(tariff.price ?? tariff.basePrice) }}/мес</p>
        <p v-if="tariff.description" class="tariff-summary__desc">{{ tariff.description }}</p>
      </div>

      <div class="form">
        <div class="form__group">
          <label class="form__label" for="address">Адрес подключения *</label>
          <input
            id="address"
            v-model="address"
            type="text"
            class="form__input"
            placeholder="Введите адрес подключения"
            maxlength="500"
          />
        </div>

        <div v-if="allServices.length" class="form__group">
          <label class="form__label">Дополнительные услуги</label>
          <div class="services-grid">
            <div
              v-for="svc in allServices"
              :key="svc.id"
              :class="['service-option', { 'service-option--selected': selectedServiceIds.includes(svc.id) }]"
              @click="toggleService(svc.id)"
            >
              <div class="service-option__checkbox">
                <span v-if="selectedServiceIds.includes(svc.id)" class="service-option__check">&#10003;</span>
              </div>
              <div class="service-option__info">
                <span class="service-option__name">{{ svc.name }}</span>
                <span v-if="svc.description" class="service-option__desc">{{ svc.description }}</span>
              </div>
              <span class="service-option__price">{{ formatPrice(svc.price) }}</span>
            </div>
          </div>
        </div>

        <div class="form__total">
          <span class="form__total-label">Итого:</span>
          <span class="form__total-value">{{ formatPrice(totalPrice) }}/мес</span>
        </div>

        <div v-if="submitError" class="page__error">{{ submitError }}</div>

        <button
          class="btn btn--primary btn--wide"
          :disabled="!address.trim() || submitting"
          @click="submit"
        >
          {{ submitting ? 'Отправка...' : 'Подать заявку' }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page__title {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 24px;
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

.tariff-summary {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}
.tariff-summary__name {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
}
.tariff-summary__category {
  background: #f3f3f3;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  text-transform: uppercase;
}
.tariff-summary__price {
  font-size: 24px;
  font-weight: 700;
  margin: 12px 0 0;
  color: #e00;
}
.tariff-summary__desc {
  color: #555;
  font-size: 14px;
  line-height: 1.5;
  margin: 8px 0 0;
}

.form {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 24px;
}
.form__group {
  margin-bottom: 24px;
}
.form__label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.form__input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color .15s;
}
.form__input:focus {
  border-color: #e00;
}

.services-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.service-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all .15s;
}
.service-option:hover {
  background: #f9f9f9;
}
.service-option--selected {
  border-color: #e00;
  background: #fff0f0;
}
.service-option__checkbox {
  width: 22px;
  height: 22px;
  border: 2px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all .15s;
}
.service-option--selected .service-option__checkbox {
  background: #e00;
  border-color: #e00;
}
.service-option__check {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
.service-option__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.service-option__name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.service-option__desc {
  font-size: 12px;
  color: #888;
}
.service-option__price {
  font-size: 14px;
  font-weight: 600;
  color: #e00;
  white-space: nowrap;
}

.form__total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #eee;
  margin-bottom: 16px;
}
.form__total-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.form__total-value {
  font-size: 22px;
  font-weight: 700;
  color: #e00;
}

.btn {
  border-radius: 24px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 16px;
  padding: 14px;
}
.btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}
.btn--primary {
  background: #e00;
  color: #fff;
}
.btn--primary:hover:not(:disabled) {
  background: #c00;
}
.btn--secondary {
  background: transparent;
  border: 1px solid #ddd;
  color: #333;
}
.btn--secondary:hover {
  background: #f5f5f5;
}
.btn--wide {
  width: 100%;
}

.success {
  text-align: center;
  padding: 40px 20px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
}
.success__icon {
  width: 64px;
  height: 64px;
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  margin: 0 auto 20px;
}
.success__title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 12px;
}
.success__text {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  margin: 0 0 8px;
}
.success__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}
.success__actions .btn {
  padding: 12px 32px;
  font-size: 14px;
}
</style>
