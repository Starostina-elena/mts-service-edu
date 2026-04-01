<script setup>
import { ref, onMounted } from 'vue'
import { fetchMyBalance, topUpBalance } from '@/api/account'
import { useAuth } from '@/auth/useAuth'

const { currentUser } = useAuth()

const balance = ref(null)
const loading = ref(false)
const error = ref(null)

const topUpAmount = ref('')
const topUpLoading = ref(false)
const topUpError = ref(null)
const topUpUrl = ref(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const data = await fetchMyBalance()
    balance.value = data
  } catch (err) {
    error.value = err.message || String(err)
  } finally {
    loading.value = false
  }
}

async function doTopUp() {
  topUpError.value = null
  topUpUrl.value = null
  const amount = Number(topUpAmount.value)
  if (!amount || amount <= 0) {
    topUpError.value = 'Введите корректную сумму'
    return
  }
  topUpLoading.value = true
  try {
    const resp = await topUpBalance(amount)
    // backend returns { payment_url }
    topUpUrl.value = resp.payment_url || resp.paymentUrl || null
  } catch (err) {
    topUpError.value = err.message || String(err)
  } finally {
    topUpLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="account-page">
    <h1>Личный кабинет</h1>

    <div v-if="loading">Загрузка баланса...</div>
    <div v-else-if="error" class="error">Ошибка: {{ error }}</div>
    <div v-else>
      <div class="balance-card">
        <div class="balance-row">
          <div class="balance-label">Пользователь:</div>
          <div class="balance-value">{{ currentUser?.name || currentUser?.email }}</div>
        </div>
        <div class="balance-row">
          <div class="balance-label">Баланс:</div>
          <div class="balance-value">{{ balance?.amount }} {{ balance?.currency }}</div>
        </div>
        <div class="balance-row" v-if="balance?.updated_at">
          <div class="balance-label">Обновлено:</div>
          <div class="balance-value">{{ new Date(balance.updated_at).toLocaleString() }}</div>
        </div>
      </div>

      <div class="topup">
        <h2>Пополнить баланс</h2>
        <div class="topup-form">
          <input type="number" v-model="topUpAmount" placeholder="Сумма" />
          <button @click="doTopUp" :disabled="topUpLoading">Пополнить</button>
        </div>
        <div v-if="topUpLoading">Подготовка платежа...</div>
        <div v-if="topUpError" class="error">{{ topUpError }}</div>
        <div v-if="topUpUrl" class="payment-link">
          <p>Перейти для оплаты:</p>
          <a :href="topUpUrl" target="_blank" rel="noopener noreferrer">Открыть платёж</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.account-page {
  max-width: 800px;
  margin: 32px auto;
  padding: 0 16px;
}
.balance-card {
  background: #fff;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}
.balance-row {
  display: flex;
  gap: 12px;
  padding: 6px 0;
}
.balance-label { color: #666; width: 140px }
.balance-value { font-weight: 700 }
.topup-form { display: flex; gap: 8px; align-items: center }
.topup-form input { padding: 8px; border-radius: 8px; border: 1px solid #ddd }
.topup-form button { padding: 8px 12px; border-radius: 8px; background: #e00; color: #fff; border: none }
.error { color: #c00; margin-top: 8px }
.payment-link { margin-top: 12px }
</style>

