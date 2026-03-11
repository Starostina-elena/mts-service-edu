// In dev mode, requests go through Vite proxy (relative paths).
// In production, set VITE_API_BASE_URL to the backend origin.
const BASE = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, params = {}) {
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') qs.set(k, String(v))
  })
  const query = qs.toString()
  const url = `${BASE}${path}${query ? '?' + query : ''}`
  const res = await fetch(url)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || `HTTP ${res.status}`)
  }
  return res.json()
}

export function fetchHome(cityId, { after, limit, priceMax } = {}) {
  return request('/catalog/home', { cityId, after, limit, priceMax })
}

export function fetchMobile({ internetGb, callsMinutes, forFamily, noSubscriptionFee, after, limit } = {}) {
  return request('/catalog/mobile', { internetGb, callsMinutes, forFamily, noSubscriptionFee, after, limit })
}

export function fetchDevices({ after, limit } = {}) {
  return request('/catalog/devices', { after, limit })
}

export function fetchLandline(cityId, { after, limit } = {}) {
  return request('/catalog/landline', { cityId, after, limit })
}

export function fetchSatelliteTv(cityId, { after, limit } = {}) {
  return request('/catalog/satellite-tv', { cityId, after, limit })
}

export function fetchBusiness({ after, limit } = {}) {
  return request('/catalog/business', { after, limit })
}

export function fetchTariffDetail(tariffId, cityId) {
  return request(`/catalog/tariffs/${tariffId}`, { cityId })
}

export function fetchCities() {
  return request('/catalog/cities')
}

export function fetchServices() {
  return request('/catalog/services')
}