<script setup>
import { ref } from 'vue'
import { createService } from '@/api/admin'

const form = ref({
  name: '',
  price: '',
  description: '',
})

const loading = ref(false)
const error = ref(null)
const successId = ref(null)

function validate() {
  if (!form.value.name.trim()) return 'Введите название услуги'
  if (!form.value.description.trim()) return 'Введите описание'
  const p = parseFloat(form.value.price)
  if (isNaN(p) || p < 0.01) return 'Укажите корректную цену (минимум 0.01)'
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
      price: parseFloat(form.value.price),
      description: form.value.description.trim(),
    }
    const result = await createService(data)
    successId.value = result.id
    form.value = { name: '', price: '', description: '' }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1 class="page__title">Создать услугу</h1>

    <div v-if="successId" class="alert alert--success">
      Услуга создана (ID: {{ successId }})
    </div>
    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label class="field__label">Название</label>
        <input v-model="form.name" class="field__input" placeholder="Например: Антивирус" />
      </div>

      <div class="field">
        <label class="field__label">Цена (руб.)</label>
        <input v-model="form.price" type="number" step="0.01" min="0.01" class="field__input" placeholder="100.00" />
      </div>

      <div class="field">
        <label class="field__label">Описание</label>
        <textarea v-model="form.description" class="field__textarea" rows="3" placeholder="Описание услуги..." />
      </div>

      <button type="submit" class="btn btn--primary btn--submit" :disabled="loading">
        {{ loading ? 'Создание...' : 'Создать' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.page {
  max-width: 600px;
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
.btn--submit {
  align-self: flex-start;
  padding: 12px 32px;
  font-size: 15px;
}
</style>
