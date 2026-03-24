const BASE = import.meta.env.VITE_API_BASE_URL || ''

function getMockUserHeader() {
  try {
    const raw = localStorage.getItem('mts_auth_user')
    if (!raw) return {}
    const user = JSON.parse(raw)
    if (user?.email) return { 'X-Mock-User': user.email }

    const role = user?.role
    if (role === 'ADMIN') return { 'X-Mock-User': 'admin@local' }
    if (role === 'MANAGER') return { 'X-Mock-User': 'manager@local' }
    return { 'X-Mock-User': 'mock@local' }
  } catch {
    return {}
  }
}

async function handleResponse(res) {
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(typeof data === 'string' ? data : data.error || data.message || `HTTP ${res.status}`)
  }
  return data
}

export async function fetchMyBalance() {
  const url = `${BASE}/account/balance`
  const res = await fetch(url, { headers: { ...getMockUserHeader() } })
  return handleResponse(res)
}

export async function topUpBalance(amount) {
  const url = `${BASE}/account/balance/top-up`
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getMockUserHeader() },
    body: JSON.stringify({ amount }),
  })
  return handleResponse(res)
}
