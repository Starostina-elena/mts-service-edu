<script setup>
defineProps({
  tariff: Object,
  category: String,
})
const emit = defineEmits(['details'])

function formatPrice(p) {
  if (p == null) return ''
  return `${Number(p).toLocaleString('ru-RU')} \u20BD/\u043Cec`
}
</script>

<template>
  <div class="card">
    <h3 class="card__name">{{ tariff.name }}</h3>

    <!-- HOME -->
    <template v-if="category === 'home'">
      <p class="card__label">Для дома</p>
      <p class="card__feature" v-if="tariff.speedMbps"><strong>{{ tariff.speedMbps }}</strong> Мбит/с</p>
      <p class="card__feature" v-if="tariff.tvChannels">{{ tariff.tvChannels }} ТВ-каналов</p>
      <div class="card__services" v-if="tariff.services?.length">
        <span v-for="s in tariff.services" :key="s.id" class="badge">{{ s.name }}</span>
      </div>
    </template>

    <!-- MOBILE -->
    <template v-if="category === 'mobile'">
      <p class="card__feature" v-if="tariff.internetGb"><strong>{{ tariff.internetGb }}</strong> ГБ</p>
      <p class="card__feature" v-if="tariff.callsMinutes"><strong>{{ tariff.callsMinutes }}</strong> минут</p>
      <p class="card__feature" v-if="tariff.smsCount"><strong>{{ tariff.smsCount }}</strong> SMS</p>
      <span v-if="tariff.forFamily" class="badge badge--accent">Семейный</span>
    </template>

    <!-- DEVICES -->
    <template v-if="category === 'devices'">
      <p class="card__feature" v-if="tariff.deviceType">{{ tariff.deviceType === 'WATCH' ? 'Часы' : 'Модем' }}</p>
      <p class="card__feature" v-if="tariff.internetGb"><strong>{{ tariff.internetGb }}</strong> ГБ</p>
    </template>

    <!-- LANDLINE -->
    <template v-if="category === 'landline'">
      <p class="card__feature" v-if="tariff.localMinutes">Местные: <strong>{{ tariff.localMinutes }}</strong> мин</p>
      <p class="card__feature" v-if="tariff.intercityMinutes">Межгород: <strong>{{ tariff.intercityMinutes }}</strong> мин</p>
      <p class="card__feature" v-if="tariff.mobileMinutes">На мобильный: <strong>{{ tariff.mobileMinutes }}</strong> мин</p>
    </template>

    <!-- SATELLITE_TV -->
    <template v-if="category === 'satellite-tv'">
      <p class="card__feature" v-if="tariff.channelsTotal"><strong>{{ tariff.channelsTotal }}</strong> каналов</p>
      <p class="card__feature" v-if="tariff.channelsHd">HD: <strong>{{ tariff.channelsHd }}</strong></p>
    </template>

    <!-- BUSINESS -->
    <template v-if="category === 'business'">
      <p class="card__feature" v-if="tariff.speedMbps"><strong>{{ tariff.speedMbps }}</strong> Мбит/с</p>
      <p class="card__feature" v-if="tariff.slaDescription">{{ tariff.slaDescription }}</p>
      <div class="card__services" v-if="tariff.services?.length">
        <span v-for="s in tariff.services" :key="s.id" class="badge">{{ s.name }}</span>
      </div>
    </template>

    <div class="card__footer">
      <span class="card__price">{{ formatPrice(tariff.price ?? tariff.basePrice) }}</span>
      <div class="card__actions">
        <button class="btn btn--primary">Подключить</button>
        <button class="btn btn--secondary" @click="emit('details', tariff.id)">Подробнее</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  transition: box-shadow .2s;
}
.card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,.1);
}
.card__name {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}
.card__label {
  color: #888;
  font-size: 13px;
  margin: 0;
}
.card__feature {
  margin: 2px 0;
  font-size: 14px;
}
.card__services {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.badge {
  background: #f3f3f3;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}
.badge--accent {
  background: #ffe0e0;
  color: #e00;
}
.card__footer {
  margin-top: auto;
  padding-top: 12px;
}
.card__price {
  font-size: 22px;
  font-weight: 700;
  display: block;
  margin-bottom: 12px;
}
.card__actions {
  display: flex;
  gap: 8px;
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
.btn--secondary {
  background: transparent;
  border: 1px solid #ddd;
  color: #333;
}
.btn--secondary:hover {
  background: #f5f5f5;
}
</style>