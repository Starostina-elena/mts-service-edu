<script setup>
import { ref, watch, computed } from 'vue'
import CitySelector from './CitySelector.vue'
import TariffCard from './TariffCard.vue'
import TariffDetail from './TariffDetail.vue'
import {
  fetchHome, fetchMobile, fetchDevices,
  fetchLandline, fetchSatelliteTv, fetchBusiness,
  searchCatalog,
} from '@/api/catalog'

const TABS = [
  { key: 'home',         label: 'Связь, ТВ и интернет', needsCity: true },
  { key: 'mobile',       label: 'Мобильная связь',      needsCity: false },
  { key: 'devices',      label: 'Для часов и модемов',   needsCity: false },
  { key: 'landline',     label: 'Домашний телефон',      needsCity: true },
  { key: 'satellite-tv', label: 'Спутниковое ТВ',        needsCity: true },
  { key: 'business',     label: 'Тарифы для бизнеса',    needsCity: false },
]

const activeTab = ref('home')
const cityId = ref(null)
const tariffs = ref([])
const nextCursor = ref(null)
const loading = ref(false)
const error = ref(null)
const detailId = ref(null)

const searchQuery = ref('')
const searchPriceMax = ref('')
const searchInTab = ref(true)

const currentTab = computed(() => TABS.find(t => t.key === activeTab.value))
const showCity = computed(() => currentTab.value?.needsCity)

const FETCHERS = {
  home:           (c, opts) => fetchHome(c, opts),
  mobile:         (_c, opts) => fetchMobile(opts),
  devices:        (_c, opts) => fetchDevices(opts),
  landline:       (c, opts) => fetchLandline(c, opts),
  'satellite-tv': (c, opts) => fetchSatelliteTv(c, opts),
  business:       (_c, opts) => fetchBusiness(opts),
}

async function load(append = false) {
  const tab = activeTab.value
  const fetcher = FETCHERS[tab]
  if (!fetcher) return
  if (currentTab.value.needsCity && !cityId.value) return

  loading.value = true
  error.value = null
  try {
    const opts = { limit: 20 }
    if (append && nextCursor.value) opts.after = nextCursor.value

    if (searchQuery.value && searchQuery.value.trim() !== '') {
      const params = {
        q: searchQuery.value.trim(),
        priceMax: searchPriceMax.value !== '' ? searchPriceMax.value : undefined,
        after: opts.after,
        limit: opts.limit,
      }
      if (searchInTab.value) params.category = tab
      const data = await searchCatalog(params)
      const mapped = data.items.map(i => ({
        id: i.id,
        name: i.name,
        description: i.description,
        basePrice: i.basePrice,
        price: i.basePrice,
        category: i.category,
        status: i.status,
        services: i.services ?? [],
        speedMbps: i.speedMbps ?? null,
        tvChannels: i.tvChannels ?? null,
        internetGb: i.internetGb ?? null,
        callsMinutes: i.callsMinutes ?? null,
      }))
      tariffs.value = append ? [...tariffs.value, ...mapped] : mapped
      nextCursor.value = data.nextCursor ?? null
      return
    }

    if (append && nextCursor.value) opts.after = nextCursor.value
    const data = await fetcher(cityId.value, opts)
    tariffs.value = append ? [...tariffs.value, ...data.items] : data.items
    nextCursor.value = data.nextCursor ?? null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  activeTab.value = key
  tariffs.value = []
  nextCursor.value = null
}

function onArchived(tariffId) {
  tariffs.value = tariffs.value.filter(t => t.id !== tariffId)
}

function doSearch() {
  tariffs.value = []
  nextCursor.value = null
  load()
}

function resetSearch() {
  searchQuery.value = ''
  searchPriceMax.value = ''
  tariffs.value = []
  nextCursor.value = null
  load()
}

watch([activeTab, cityId], () => {
  tariffs.value = []
  nextCursor.value = null
  load()
})
</script>

<template>
  <div class="catalog">
    <div class="catalog__header">
      <h1 class="catalog__title">Тарифы</h1>
      <div class="catalog__search">
        <input v-model="searchQuery" class="search__input" placeholder="Поиск по тарифам (имя, описание)" />
        <input v-model="searchPriceMax" class="search__price" placeholder="Макс. цена" type="number" min="0" />
        <label class="search__in-tab"><input type="checkbox" v-model="searchInTab" /> По текущей вкладке</label>
        <button class="btn" @click="doSearch">Поиск</button>
        <button class="btn" @click="resetSearch">Сбросить</button>
      </div>
      <CitySelector v-if="showCity" v-model="cityId" />
    </div>

    <nav class="tabs">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        :class="['tab', { 'tab--active': activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div v-if="error" class="catalog__error">{{ error }}</div>

    <div v-if="tariffs.length" class="grid">
      <TariffCard
        v-for="t in tariffs"
        :key="t.id"
        :tariff="t"
        :category="activeTab"
        @details="detailId = $event"
        @archived="onArchived"
      />
    </div>

    <div v-if="loading" class="catalog__loading">Загрузка...</div>

    <div v-if="!loading && !tariffs.length && !error" class="catalog__empty">
      Тарифы не найдены
    </div>

    <button
      v-if="nextCursor && !loading"
      class="btn btn--load-more"
      @click="load(true)"
    >
      Загрузить ещё
    </button>

    <TariffDetail
      v-if="detailId"
      :tariff-id="detailId"
      :city-id="cityId"
      @close="detailId = null"
      @archived="onArchived"
    />
  </div>
</template>

<style scoped>
.catalog {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}
.catalog__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.catalog__title {
  font-size: 36px;
  font-weight: 800;
  margin: 0;
}
.catalog__search {
  display: flex;
  gap: 8px;
  align-items: center;
}
.search__input {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
}
.search__price {
  width: 120px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
}
.search__in-tab {
  font-size: 13px;
  color: #444;
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
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.catalog__loading,
.catalog__empty {
  text-align: center;
  padding: 40px;
  color: #888;
}
.catalog__error {
  background: #fff0f0;
  color: #c00;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
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
</style>
