const state = {
  applications: [],
  forms: {}
};

const els = {
  actor: document.getElementById("actor"),
  createForm: document.getElementById("createForm"),
  createFields: document.getElementById("createFields"),
  decisionForm: document.getElementById("decisionForm"),
  decisionFields: document.getElementById("decisionFields"),
  applicationSelect: document.getElementById("applicationSelect"),
  approveBtn: document.getElementById("approveBtn"),
  refreshBtn: document.getElementById("refreshBtn"),
  topUpBtn: document.getElementById("topUpBtn"),
  applicationsBody: document.getElementById("applicationsBody"),
  notice: document.getElementById("notice")
};

function headers() {
  return {
    "Content-Type": "application/json",
    "X-Mock-User": els.actor.value
  };
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...headers(),
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(body?.error || body?.message || `HTTP ${response.status}`);
  }
  return body;
}

async function loadForm(formId) {
  if (!state.forms[formId]) {
    state.forms[formId] = await request(`/ui/forms/${formId}`, { method: "GET" });
  }
  return state.forms[formId];
}

function fieldType(component) {
  if (component.type === "textarea") {
    return "textarea";
  }
  if (component.type === "checkbox") {
    return "checkbox";
  }
  return "input";
}

function renderForm(container, form) {
  container.replaceChildren();
  form.components.forEach(component => {
    const type = fieldType(component);
    const label = document.createElement("label");
    label.className = type === "checkbox" ? "check-field" : "field";
    if (type !== "checkbox" && component.type === "textarea") {
      label.classList.add("wide");
    }

    const input = type === "textarea" ? document.createElement("textarea") : document.createElement("input");
    input.name = component.key;
    input.required = Boolean(component.validate?.required);
    if (component.validate?.maxLength) {
      input.maxLength = component.validate.maxLength;
    }
    if (type === "checkbox") {
      input.type = "checkbox";
      label.append(input, document.createTextNode(component.label));
    } else {
      input.type = "text";
      input.placeholder = component.label;
      const title = document.createElement("span");
      title.textContent = component.label;
      label.append(title, input);
    }
    container.append(label);
  });
}

function formValue(form, key) {
  const element = form.elements[key];
  if (!element) {
    return null;
  }
  if (element.type === "checkbox") {
    return element.checked;
  }
  return element.value.trim();
}

function parseIds(value) {
  if (!value) {
    return [];
  }
  return value.split(",").map(v => Number(v.trim())).filter(Number.isFinite);
}

function selectedApplicationId() {
  return Number(els.applicationSelect.value);
}

function show(message, type = "") {
  els.notice.className = `notice ${type}`;
  els.notice.textContent = message;
}

function renderApplications() {
  els.applicationsBody.replaceChildren();
  els.applicationSelect.replaceChildren();

  state.applications.forEach(application => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${application.id}</td>
      <td>${application.tariffName || application.tariffId}</td>
      <td>${application.address || ""}</td>
      <td><span class="status ${application.status}">${application.status}</span></td>
      <td>${application.rejectReason || ""}</td>
    `;
    els.applicationsBody.append(row);

    if (application.status === "PENDING") {
      const option = document.createElement("option");
      option.value = application.id;
      option.textContent = `#${application.id} ${application.tariffName || ""}`;
      els.applicationSelect.append(option);
    }
  });

  if (!els.applicationSelect.options.length) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "Нет задач";
    els.applicationSelect.append(option);
  }
}

async function refreshApplications() {
  const page = await request("/applications?limit=100", { method: "GET" });
  state.applications = page.items || [];
  renderApplications();
}

async function createApplication(event) {
  event.preventDefault();
  const payload = {
    tariffId: Number(formValue(els.createForm, "tariffId")),
    cityId: formValue(els.createForm, "cityId") ? Number(formValue(els.createForm, "cityId")) : null,
    address: formValue(els.createForm, "address"),
    additionalServiceIds: parseIds(formValue(els.createForm, "additionalServiceIds"))
  };
  await request("/applications", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  els.createForm.reset();
  show("Заявка создана", "ok");
  await refreshApplications();
}

async function approveApplication() {
  const id = selectedApplicationId();
  if (!id) {
    return;
  }
  await request(`/applications/${id}/approve`, { method: "POST" });
  show(`Заявка #${id} одобрена`, "ok");
  await refreshApplications();
}

async function rejectApplication(event) {
  event.preventDefault();
  const id = selectedApplicationId();
  if (!id) {
    return;
  }
  await request(`/applications/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason: formValue(els.decisionForm, "rejectReason") || "Rejected" })
  });
  els.decisionForm.reset();
  show(`Заявка #${id} отклонена`, "ok");
  await refreshApplications();
}

async function topUp() {
  await request("/payments/callback", {
    method: "POST",
    body: JSON.stringify({ userId: 1, amount: 50000 })
  });
  show("Баланс пользователя пополнен", "ok");
}

async function run(action) {
  try {
    show("");
    await action();
  } catch (error) {
    show(error.message, "error");
  }
}

async function init() {
  renderForm(els.createFields, await loadForm("application-create"));
  renderForm(els.decisionFields, await loadForm("manager-decision"));
  els.decisionFields.querySelector("[name=approved]")?.closest("label")?.remove();
  els.createForm.elements.tariffId.value = "4";
  els.createForm.elements.cityId.value = "1";

  els.createForm.addEventListener("submit", event => run(() => createApplication(event)));
  els.decisionForm.addEventListener("submit", event => run(() => rejectApplication(event)));
  els.approveBtn.addEventListener("click", () => run(approveApplication));
  els.refreshBtn.addEventListener("click", () => run(refreshApplications));
  els.topUpBtn.addEventListener("click", () => run(topUp));
  els.actor.addEventListener("change", () => run(refreshApplications));

  await refreshApplications();
}

init().catch(error => show(error.message, "error"));
