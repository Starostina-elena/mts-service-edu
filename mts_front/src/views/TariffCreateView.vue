<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createTariff } from '@/api/admin'
import { fetchCities, fetchServices } from '@/api/catalog'

const router = useRouter()

const CATEGORIES = [
  { value: 'home', label: 'Связь, ТВ и интернет' },
  { value: 'mobile', label: 'Мобильная связь' },
  { value: 'devices', label: 'Для часов и модемов' },
  { value: 'landline', label: 'Домашний телефон' },
  { value: 'satellite_tv', label: 'Спутниковое ТВ' },
  { value: 'business', label: 'Тарифы для бизнеса' },
]

const form = ref({
  name: '',
  category: 'home',
  price: '',
  description: '',
  // home / business
  speed_mbps: '',
  tv_channels: '',
  // mobile
  internet_gb: '',
  calls_minutes: '',
  sms_count: '',
  for_family: false,
  no_subscription_fee: false,
  // devices
  device_type: 'WATCH',
  // landline
  local_minutes: '',
  intercity_minutes: '',
  mobile_minutes: '',
  // satellite
  channels_total: '',
  channels_hd: '',
  // business
  sla_description: '',
})

const cityPrices = ref([])
const selectedServiceIds = ref([])
const cities = ref([])
const services = ref([])
const loading = ref(false)
const error = ref(null)
const successId = ref(null)

const cat = computed(() => form.value.category)
const needsCityPrices = computed(() => ['home', 'landline', 'satellite_tv'].includes(cat.value))
const needsServices = computed(() => ['home', 'business'].includes(cat.value))

onMounted(async () => {
  try {
    const [c, s] = await Promise.all([fetchCities(), fetchServices()])
    cities.value = c
    services.value = s
  } catch (e) {
    console.error('Failed to load reference data', e)
  }
})

function addCityPrice() {
  cityPrices.value.push({ city_id: '', price: '' })
}

function removeCityPrice(index) {
  cityPrices.value.splice(index, 1)
}

function intOrNull(v) {
  const n = parseInt(v, 10)
  return isNaN(n) ? null : n
}

function validate() {
  if (!form.value.name.trim()) return 'Введите название тарифа'
  const p = parseFloat(form.value.price)
  if (isNaN(p) || p < 0.01) return 'Укажите корректную цену (минимум 0.01)'

  if (needsCityPrices.value) {
    for (let i = 0; i < cityPrices.value.length; i++) {
      const cp = cityPrices.value[i]
      if (!cp.city_id) return `Город не выбран в строке ${i + 1} городских цен`
      const pp = parseFloat(cp.price)
      if (isNaN(pp) || pp < 0.01) return `Некорректная цена в строке ${i + 1} городских цен`
    }
  }

  return null
}

async function submit() {
  const err = validate()
  if (err) {
    error.value = err
    return
  }

  loading.value = true
  error.value = null
  successId.value = null

  try {
    const data = {
      name: form.value.name.trim(),
      category: form.value.category,
      price: parseFloat(form.value.price),
      description: form.value.description.trim() || null,
    }

    if (cat.value === 'home' || cat.value === 'business') {
      data.speed_mbps = intOrNull(form.value.speed_mbps)
      data.tv_channels = intOrNull(form.value.tv_channels)
    }

    if (cat.value === 'mobile') {
      data.internet_gb = intOrNull(form.value.internet_gb)
      data.calls_minutes = intOrNull(form.value.calls_minutes)
      data.sms_count = intOrNull(form.value.sms_count)
      data.for_family = form.value.for_family
      data.no_subscription_fee = form.value.no_subscription_fee
    }

    if (cat.value === 'devices') {
      data.internet_gb = intOrNull(form.value.internet_gb)
      data.device_type = form.value.device_type
    }

    if (cat.value === 'landline') {
      data.local_minutes = intOrNull(form.value.local_minutes)
      data.intercity_minutes = intOrNull(form.value.intercity_minutes)
      data.mobile_minutes = intOrNull(form.value.mobile_minutes)
    }

    if (cat.value === 'satellite_tv') {
      data.channels_total = intOrNull(form.value.channels_total)
      data.channels_hd = intOrNull(form.value.channels_hd)
    }

    if (cat.value === 'business') {
      data.sla_description = form.value.sla_description.trim() || null
    }

    if (needsServices.value && selectedServiceIds.value.length) {
      data.service_ids = selectedServiceIds.value.map(String)
    }

    if (needsCityPrices.value && cityPrices.value.length) {
      data.city_prices = cityPrices.value.map(cp => ({
        city_id: cp.city_id,
        price: parseFloat(cp.price),
      }))
    }

    const result = await createTariff(data)
    successId.value = result.id
    setTimeout(() => router.push('/'), 1500)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1 class="page__title">Создать тариф</h1>

    <div v-if="successId" class="alert alert--success">
      Тариф создан (ID: {{ successId }})
    </div>
    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <form class="form" @submit.prevent="submit">
      <!-- Common fields -->
      <div class="field">
        <label class="field__label">Категория</label>
        <select v-model="form.category" class="field__input">
          <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </div>

      <div class="field">
        <label class="field__label">Название</label>
        <input v-model="form.name" class="field__input" placeholder="Название тарифа" />
      </div>

      <div class="field">
        <label class="field__label">Базовая цена (руб.)</label>
        <input v-model="form.price" type="number" step="0.01" min="0.01" class="field__input" placeholder="500.00" />
      </div>

      <div class="field">
        <label class="field__label">Описание</label>
        <textarea v-model="form.description" class="field__textarea" rows="2" placeholder="Необязательное описание..." />
      </div>

      <!-- HOME / BUSINESS fields -->
      <template v-if="cat === 'home' || cat === 'business'">
        <div class="field-row">
          <div class="field">
            <label class="field__label">Скорость (Мбит/с)</label>
            <input v-model="form.speed_mbps" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">ТВ-каналов</label>
            <input v-model="form.tv_channels" type="number" min="0" class="field__input" />
          </div>
        </div>
      </template>

      <!-- BUSINESS extra -->
      <template v-if="cat === 'business'">
        <div class="field">
          <label class="field__label">SLA описание</label>
          <textarea v-model="form.sla_description" class="field__textarea" rows="2" placeholder="Описание SLA..." />
        </div>
      </template>

      <!-- MOBILE fields -->
      <template v-if="cat === 'mobile'">
        <div class="field-row">
          <div class="field">
            <label class="field__label">Интернет (ГБ)</label>
            <input v-model="form.internet_gb" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">Звонки (минуты)</label>
            <input v-model="form.calls_minutes" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">SMS</label>
            <input v-model="form.sms_count" type="number" min="0" class="field__input" />
          </div>
        </div>
        <div class="field-row">
          <label class="checkbox">
            <input v-model="form.for_family" type="checkbox" />
            <span>Семейный тариф</span>
          </label>
          <label class="checkbox">
            <input v-model="form.no_subscription_fee" type="checkbox" />
            <span>Без абонентской платы</span>
          </label>
        </div>
      </template>

      <!-- DEVICES fields -->
      <template v-if="cat === 'devices'">
        <div class="field-row">
          <div class="field">
            <label class="field__label">Тип устройства</label>
            <select v-model="form.device_type" class="field__input">
              <option value="WATCH">Часы</option>
              <option value="MODEM">Модем</option>
            </select>
          </div>
          <div class="field">
            <label class="field__label">Интернет (ГБ)</label>
            <input v-model="form.internet_gb" type="number" min="0" class="field__input" />
          </div>
        </div>
      </template>

      <!-- LANDLINE fields -->
      <template v-if="cat === 'landline'">
        <div class="field-row">
          <div class="field">
            <label class="field__label">Местные (мин)</label>
            <input v-model="form.local_minutes" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">Межгород (мин)</label>
            <input v-model="form.intercity_minutes" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">На мобильный (мин)</label>
            <input v-model="form.mobile_minutes" type="number" min="0" class="field__input" />
          </div>
        </div>
      </template>

      <!-- SATELLITE_TV fields -->
      <template v-if="cat === 'satellite_tv'">
        <div class="field-row">
          <div class="field">
            <label class="field__label">Всего каналов</label>
            <input v-model="form.channels_total" type="number" min="0" class="field__input" />
          </div>
          <div class="field">
            <label class="field__label">HD каналов</label>
            <input v-model="form.channels_hd" type="number" min="0" class="field__input" />
          </div>
        </div>
      </template>

      <!-- City prices -->
      <template v-if="needsCityPrices">
        <div class="section">
          <div class="section__header">
            <h3 class="section__title">Цены по городам</h3>
            <button type="button" class="btn btn--small" @click="addCityPrice">+ Добавить город</button>
          </div>
          <div v-for="(cp, idx) in cityPrices" :key="idx" class="city-price-row">
            <select v-model="cp.city_id" class="field__input">
              <option value="" disabled>Выберите город</option>
              <option v-for="c in cities" :key="c.id" :value="c.code">{{ c.name }}</option>
            </select>
            <input v-model="cp.price" type="number" step="0.01" min="0.01" class="field__input" placeholder="Цена" />
            <button type="button" class="btn btn--danger btn--small" @click="removeCityPrice(idx)">Удалить</button>
          </div>
        </div>
      </template>

      <!-- Services multi-select -->
      <template v-if="needsServices">
        <div class="section">
          <h3 class="section__title">Включённые услуги</h3>
          <div v-if="services.length" class="service-checkboxes">
            <label v-for="s in services" :key="s.id" class="checkbox">
              <input
                type="checkbox"
                :value="s.id"
                v-model="selectedServiceIds"
              />
              <span>{{ s.name }} ({{ new Intl.NumberFormat('ru-RU').format(s.price) }} &#8381;)</span>
            </label>
          </div>
          <p v-else class="hint">Услуги не загружены</p>
        </div>
      </template>

      <button type="submit" class="btn btn--primary btn--submit" :disabled="loading">
        {{ loading ? 'Создание...' : 'Создать тариф' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.page {
  max-width: 700px;
  margin: 32px auto;
  padding: 0 16px;
}
.page__title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 24px;
}
.alert {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}
.alert--success {
  background: #e8f5e9;
  color: #2e7d32;
}
.alert--error {
  background: #fff0f0;
  color: #c00;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field__label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 6px;
}
.field__input,
.field__textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  background: #fff;
}
.field__input:focus,
.field__textarea:focus {
  outline: none;
  border-color: #e00;
}
.field__textarea {
  resize: vertical;
}
.field-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.field-row .field {
  flex: 1;
  min-width: 140px;
}
.section {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px;
}
.section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section__title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 8px;
}
.city-price-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.city-price-row .field__input {
  flex: 1;
}
.service-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}
.checkbox input[type="checkbox"] {
  accent-color: #e00;
  width: 16px;
  height: 16px;
}
.hint {
  color: #888;
  font-size: 13px;
}
.btn {
  padding: 10px 20px;
  border-radius: 24px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}
.btn--primary {
  background: #e00;
  color: #fff;
}
.btn--primary:hover {
  background: #c00;
}
.btn--primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.btn--small {
  padding: 6px 14px;
  font-size: 13px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 16px;
}
.btn--small:hover {
  background: #f5f5f5;
}
.btn--danger {
  color: #e00;
  border-color: #e00;
}
.btn--danger:hover {
  background: #fff0f0;
}
.btn--submit {
  align-self: flex-start;
  padding: 12px 32px;
  font-size: 15px;
  margin-top: 8px;
}
</style>
