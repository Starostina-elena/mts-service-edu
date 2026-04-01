<script setup>
import { ref, onMounted } from 'vue'
import { fetchCities } from '@/api/catalog'

const props = defineProps({ modelValue: Number })
const emit = defineEmits(['update:modelValue'])

const cities = ref([])

onMounted(async () => {
  try {
    cities.value = await fetchCities()
    if (!props.modelValue && cities.value.length) {
      emit('update:modelValue', cities.value[0].id)
    }
  } catch (e) {
    console.error('Failed to load cities', e)
  }
})
</script>

<template>
  <select
    id="city-select"
    name="city"
    class="city-select"
    :value="modelValue"
    @change="emit('update:modelValue', Number($event.target.value))"
  >
    <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
  </select>
</template>

<style scoped>
.city-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}
</style>