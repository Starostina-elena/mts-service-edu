import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { useAuth } from '@/auth/useAuth'

const { restore } = useAuth()
restore()

createApp(App).use(router).mount('#app')
