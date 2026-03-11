import { ref, computed } from 'vue'

const STORAGE_KEY = 'mts_auth_user'

const currentUser = ref(null)

function restore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) currentUser.value = JSON.parse(raw)
  } catch {
    localStorage.removeItem(STORAGE_KEY)
  }
}

function login(user) {
  currentUser.value = user
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

function logout() {
  currentUser.value = null
  localStorage.removeItem(STORAGE_KEY)
}

export function useAuth() {
  const isLoggedIn = computed(() => !!currentUser.value)
  const role = computed(() => currentUser.value?.role ?? null)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isManager = computed(() => role.value === 'MANAGER')

  return {
    currentUser,
    isLoggedIn,
    role,
    isAdmin,
    isManager,
    login,
    logout,
    restore,
  }
}
