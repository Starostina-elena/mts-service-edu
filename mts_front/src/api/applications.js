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
    throw new Error(body.error || body.message || `HTTP ${res.status}`)
  }
  return res.json()
}

async function postJSON(path, body, headers = {}) {
  const url = `${BASE}${path}`
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(typeof data === 'string' ? data : data.error || data.message || `HTTP ${res.status}`)
  }
  return data
}

async function postEmpty(path, headers = {}) {
  const url = `${BASE}${path}`
  const res = await fetch(url, { method: 'POST', headers })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(typeof data === 'string' ? data : data.error || data.message || `HTTP ${res.status}`)
  }
  return data
}

export function fetchApplications({ userId, status, after, limit } = {}) {
  return request('/applications', { userId, status, after, limit })
}

export function fetchApplicationDetail(applicationId) {
  return request(`/applications/${applicationId}`)
}

export function createApplication(userId, data) {
  return postJSON('/applications', data, { 'X-User-Id': String(userId) })
}

export function approveApplication(userId, applicationId) {
  return postEmpty(`/applications/${applicationId}/approve`, { 'X-User-Id': String(userId) })
}

export function rejectApplication(userId, applicationId, reason) {
  return postJSON(`/applications/${applicationId}/reject`, { reason }, { 'X-User-Id': String(userId) })
}
