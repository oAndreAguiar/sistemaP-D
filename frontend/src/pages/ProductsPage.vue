<template>
  <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
    <h2 style="margin:0;">{{ t("nav.products") }}</h2>
    <button @click="openCreate">{{ t("common.add") }}</button>
  </div>

  <div style="margin-top:12px;">
    <input
      v-model="query"
      :placeholder="t('tables.search')"
      style="padding:8px 10px; border:1px solid #ddd; border-radius:8px; width: 320px;"
    />
  </div>

  <div v-if="loading" style="margin-top:12px;">Loading...</div>
  <div v-else-if="error" style="margin-top:12px; color:#b00020;">
    {{ error }}
  </div>

  <table v-else style="width:100%; border-collapse: collapse; margin-top:12px;">
    <thead>
      <tr style="text-align:left; border-bottom:1px solid #eee;">
        <th>{{ t("tables.id") }}</th>
        <th>{{ t("tables.code") }}</th>
        <th>{{ t("tables.name") }}</th>
        <th>{{ t("tables.unitValue") }}</th>
        <th>{{ t("tables.components") }}</th>
        <th>{{ t("common.actions") }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="p in filtered" :key="p.id" style="border-bottom:1px solid #f2f2f2;">
        <td style="padding:10px;">{{ p.id }}</td>
        <td style="padding:10px;">{{ p.code }}</td>
        <td style="padding:10px;">{{ p.name }}</td>
        <td style="padding:10px;">{{ formatMoney(p.unitValue) }}</td>
        <td style="padding:10px;">
          <div v-if="p.components?.length">
            <div v-for="c in p.components" :key="c.rawMaterialId" style="font-size: 13px; color:#444;">
              • {{ c.rawMaterialCode }} ({{ c.rawMaterialName }}): {{ c.quantityPerUnit }}
            </div>
          </div>
          <span v-else style="color:#666;">-</span>
        </td>
        <td style="padding:10px; display:flex; gap:8px;">
          <button @click="openEdit(p)">{{ t("common.edit") }}</button>
          <button @click="remove(p)" style="border-color:#f2c0c0;">{{ t("common.delete") }}</button>
        </td>
      </tr>

      <tr v-if="filtered.length === 0">
        <td colspan="6" style="padding:12px; color:#666;">No data</td>
      </tr>
    </tbody>
  </table>

  <!-- Modal -->
  <div
    v-if="modal.open"
    style="position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; padding:16px;"
  >
    <div style="background:#fff; width: 780px; border-radius:12px; padding:16px;">
      <h3 style="margin-top:0;">
        {{ modal.mode === "create" ? t("common.add") : t("common.edit") }}
      </h3>

      <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
        <label style="display:grid; gap:6px;">
          <span>Code</span>
          <input
            v-model="form.code"
            :disabled="modal.mode === 'edit'"
            style="padding:8px 10px; border:1px solid #ddd; border-radius:8px;"
          />
        </label>

        <label style="display:grid; gap:6px;">
          <span>Name</span>
          <input
            v-model="form.name"
            style="padding:8px 10px; border:1px solid #ddd; border-radius:8px;"
          />
        </label>

        <label style="display:grid; gap:6px;">
          <span>Unit Value</span>
          <input
            v-model.number="form.unitValue"
            type="number"
            min="0"
            step="0.01"
            style="padding:8px 10px; border:1px solid #ddd; border-radius:8px;"
          />
        </label>
      </div>

      <hr style="margin:14px 0; border:0; border-top:1px solid #eee;" />

      <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
        <strong>Composition</strong>
        <button @click="addComponent">+ Add component</button>
      </div>

      <div v-if="rawMaterials.length === 0" style="margin-top:10px; color:#b00020;">
        You must create Raw Materials first.
      </div>

      <div style="display:grid; gap:10px; margin-top:10px;">
        <!-- ✅ AQUI: troquei GRID por FLEX com classes -->
        <div
          v-for="(c, idx) in form.components"
          :key="idx"
          class="component-row"
        >
          <label class="field">
            <span>Raw Material</span>
            <select v-model.number="c.rawMaterialId">
              <option :value="null" disabled>Select...</option>
              <option v-for="rm in rawMaterials" :key="rm.id" :value="rm.id">
                {{ rm.code }} - {{ rm.name }}
              </option>
            </select>
          </label>

          <label class="field qty">
            <span>Qty / unit</span>
            <input
              v-model.number="c.quantityPerUnit"
              type="number"
              min="1"
              step="1"
            />
          </label>

          <button class="remove-btn" @click="removeComponent(idx)">
            Remove
          </button>
        </div>

        <div v-if="form.components.length === 0" style="color:#666; font-size: 13px;">
          No components. Click “Add component”.
        </div>
      </div>

      <div v-if="modalError" style="color:#b00020; margin-top:10px;">{{ modalError }}</div>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:14px;">
        <button @click="closeModal">{{ t("common.cancel") }}</button>
        <button @click="save" :disabled="saving">
          {{ saving ? "Saving..." : t("common.save") }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { api } from "../services/api";

const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const modalError = ref("");
const query = ref("");

const products = ref([]);
const rawMaterials = ref([]);

const modal = reactive({
  open: false,
  mode: "create", // create | edit
  editId: null,
});

const form = reactive({
  code: "",
  name: "",
  unitValue: 0,
  components: [],
});

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase();
  if (!q) return products.value;
  return products.value.filter((x) =>
    `${x.code} ${x.name} ${x.id}`.toLowerCase().includes(q)
  );
});

function formatMoney(v) {
  if (v == null) return "-";
  const n = Number(v);
  return isNaN(n) ? String(v) : n.toFixed(2);
}

function resetForm() {
  form.code = "";
  form.name = "";
  form.unitValue = 0;
  form.components = [];
}

function openCreate() {
  modalError.value = "";
  resetForm();
  modal.mode = "create";
  modal.editId = null;
  modal.open = true;
}

function openEdit(p) {
  modalError.value = "";
  modal.mode = "edit";
  modal.editId = p.id;

  form.code = p.code;
  form.name = p.name;
  form.unitValue = Number(p.unitValue);

  form.components = (p.components || []).map((c) => ({
    rawMaterialId: c.rawMaterialId,
    quantityPerUnit: c.quantityPerUnit,
  }));

  modal.open = true;
}

function closeModal() {
  modal.open = false;
}

function addComponent() {
  form.components.push({ rawMaterialId: null, quantityPerUnit: 1 });
}

function removeComponent(idx) {
  form.components.splice(idx, 1);
}

async function fetchAll() {
  loading.value = true;
  error.value = "";
  try {
    const [pRes, rmRes] = await Promise.all([
      api.get("/products"),
      api.get("/raw-materials"),
    ]);
    products.value = pRes.data;
    rawMaterials.value = rmRes.data;
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || "Failed to load data";
  } finally {
    loading.value = false;
  }
}

function validate() {
  if (!form.code?.trim() && modal.mode === "create") return "Code is required";
  if (!form.name?.trim()) return "Name is required";
  if (form.unitValue == null || Number(form.unitValue) < 0) return "Unit value must be >= 0";

  if (!Array.isArray(form.components) || form.components.length === 0) {
    return "At least 1 component is required";
  }

  const seen = new Set();
  for (const c of form.components) {
    if (!c.rawMaterialId) return "Select a raw material in all components";
    if (seen.has(c.rawMaterialId)) return "You cannot repeat the same raw material";
    seen.add(c.rawMaterialId);

    if (c.quantityPerUnit == null || Number(c.quantityPerUnit) < 1) {
      return "Quantity per unit must be >= 1";
    }
  }

  return "";
}

async function save() {
  modalError.value = "";
  const v = validate();
  if (v) {
    modalError.value = v;
    return;
  }

  saving.value = true;
  try {
    const payloadCreate = {
      code: form.code.trim(),
      name: form.name.trim(),
      unitValue: Number(form.unitValue),
      components: form.components.map((c) => ({
        rawMaterialId: Number(c.rawMaterialId),
        quantityPerUnit: Number(c.quantityPerUnit),
      })),
    };

    const payloadUpdate = {
      name: form.name.trim(),
      unitValue: Number(form.unitValue),
      components: form.components.map((c) => ({
        rawMaterialId: Number(c.rawMaterialId),
        quantityPerUnit: Number(c.quantityPerUnit),
      })),
    };

    if (modal.mode === "create") {
      await api.post("/products", payloadCreate);
    } else {
      await api.put(`/products/${modal.editId}`, payloadUpdate);
    }

    modal.open = false;
    await fetchAll();
  } catch (e) {
    modalError.value = e?.response?.data?.message || e?.message || "Save failed";
  } finally {
    saving.value = false;
  }
}

async function remove(p) {
  const ok = confirm(`Delete ${p.code}?`);
  if (!ok) return;

  try {
    await api.delete(`/products/${p.id}`);
    await fetchAll();
  } catch (e) {
    alert(e?.response?.data?.message || e?.message || "Delete failed");
  }
}

onMounted(fetchAll);
</script>

<style scoped>
button {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #ddd;
  cursor: pointer;
  background: #fff;
}
button:hover {
  background: #f7f7f7;
}
select, input {
  font: inherit;
}

/* ✅ FIX DEFINITIVO DO "REMOVE BUGADO" */
.component-row{
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.field{
  display: grid;
  gap: 6px;
  flex: 1;
}

.field.qty{
  flex: 0 0 160px;
}

.field select,
.field input{
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  height: 38px; /* alinha tudo */
}

.remove-btn{
  flex: 0 0 120px;
  height: 38px;
  border: 1px solid #f2c0c0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}
.remove-btn:hover{
  background: #fff2f2;
}
</style>