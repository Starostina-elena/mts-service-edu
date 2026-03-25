const BASE = import.meta.env.VITE_API_BASE_URL || ''

async function postJSON(path, body) {
  const url = `${BASE}${path}`
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(typeof data === 'string' ? data : data.error || data.message || `HTTP ${res.status}`)
  }
  return data
}

async function postEmpty(path) {
  const url = `${BASE}${path}`
  const res = await fetch(url, { method: 'POST' })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(typeof data === 'string' ? data : data.error || data.message || `HTTP ${res.status}`)
  }
  return data
}

export function createTariff(data) {
  return postJSON('/admin/tariffs', data)
}

export function archiveTariff(tariffId) {
  return postEmpty(`/admin/tariffs/${tariffId}/archive`)
}

export function createService(data) {
  return postJSON('/admin/services', data)
}

export function reindexTariffs() {
  return postEmpty('/admin/tariffs/reindex')
}
