<script setup>
import { ref, watch } from 'vue'
import { fetchTariffDetail } from '@/api/catalog'

const props = defineProps({
  tariffId: Number,
  cityId: Number,
})
const emit = defineEmits(['close'])

const detail = ref(null)
const loading = ref(false)
const error = ref(null)

watch(
  () => props.tariffId,
  async (id) => {
    if (!id) return
    loading.value = true
    error.value = null
    try {
      detail.value = await fetchTariffDetail(id, props.cityId)
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

function formatPrice(p) {
  if (p == null) return ''
  return `${Number(p).toLocaleString('ru-RU')} \u20BD/\u043Cec`
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal">
      <button class="modal__close" @click="emit('close')">&times;</button>

      <div v-if="loading" class="modal__loading">Загрузка...</div>
      <div v-else-if="error" class="modal__error">{{ error }}</div>
      <template v-else-if="detail">
        <h2>{{ detail.name }}</h2>
        <span class="modal__category">{{ detail.category }}</span>
        <p class="modal__price">{{ formatPrice(detail.price) }}</p>
        <p v-if="detail.description" class="modal__desc">{{ detail.description }}</p>

        <ul class="modal__features">
          <li v-if="detail.speedMbps">Скорость: {{ detail.speedMbps }} Мбит/с</li>
          <li v-if="detail.tvChannels">ТВ-каналов: {{ detail.tvChannels }}</li>
          <li v-if="detail.internetGb">Интернет: {{ detail.internetGb }} ГБ</li>
          <li v-if="detail.callsMinutes">Звонки: {{ detail.callsMinutes }} минут</li>
          <li v-if="detail.smsCount">SMS: {{ detail.smsCount }}</li>
          <li v-if="detail.forFamily">Семейный тариф</li>
          <li v-if="detail.noSubscriptionFee">Без абонентской платы</li>
          <li v-if="detail.deviceType">Тип устройства: {{ detail.deviceType === 'WATCH' ? 'Часы' : 'Модем' }}</li>
          <li v-if="detail.localMinutes">Местные: {{ detail.localMinutes }} мин</li>
          <li v-if="detail.intercityMinutes">Межгород: {{ detail.intercityMinutes }} мин</li>
          <li v-if="detail.mobileMinutes">На мобильный: {{ detail.mobileMinutes }} мин</li>
          <li v-if="detail.channelsTotal">Каналов: {{ detail.channelsTotal }}</li>
          <li v-if="detail.channelsHd">HD каналов: {{ detail.channelsHd }}</li>
          <li v-if="detail.slaDescription">SLA: {{ detail.slaDescription }}</li>
        </ul>

        <div v-if="detail.services?.length" class="modal__services">
          <h4>Включённые сервисы</h4>
          <div v-for="s in detail.services" :key="s.id" class="service-item">
            <strong>{{ s.name }}</strong>
            <span v-if="s.description"> — {{ s.description }}</span>
          </div>
        </div>

        <button class="btn btn--primary btn--wide">Подключить</button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  max-width: 560px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
}
.modal__close {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 28px;
  background: none;
  border: none;
  cursor: pointer;
  color: #888;
}
.modal__category {
  background: #f3f3f3;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  text-transform: uppercase;
}
.modal__price {
  font-size: 28px;
  font-weight: 700;
  margin: 16px 0;
}
.modal__desc {
  color: #555;
  line-height: 1.5;
}
.modal__features {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.modal__features li {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}
.modal__services {
  margin-top: 16px;
}
.modal__services h4 {
  margin: 0 0 8px;
}
.service-item {
  font-size: 14px;
  padding: 4px 0;
}
.modal__loading,
.modal__error {
  text-align: center;
  padding: 40px;
}
.modal__error {
  color: #e00;
}
.btn--wide {
  width: 100%;
  margin-top: 20px;
  padding: 14px;
  font-size: 16px;
}
.btn {
  border-radius: 24px;
  border: none;
  cursor: pointer;
  font-weight: 600;
}
.btn--primary {
  background: #e00;
  color: #fff;
}
.btn--primary:hover {
  background: #c00;
}
</style>
