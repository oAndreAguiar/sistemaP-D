<template>
  <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
    <h2 style="margin:0;">{{ t("nav.rawMaterials") }}</h2>

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
        <th>{{ t("tables.stock") }}</th>
        <th>{{ t("common.actions") }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="rm in filtered" :key="rm.id" style="border-bottom:1px solid #f2f2f2;">
        <td style="padding:10px;">{{ rm.id }}</td>
        <td style="padding:10px;">{{ rm.code }}</td>
        <td style="padding:10px;">{{ rm.name }}</td>
        <td style="padding:10px;">{{ rm.stockQuantity }}</td>
        <td style="padding:10px; display:flex; gap:8px;">
          <button @click="openEdit(rm)">{{ t("common.edit") }}</button>
          <button @click="remove(rm)" style="border-color:#f2c0c0;">{{ t("common.delete") }}</button>
        </td>
      </tr>

      <tr v-if="filtered.length === 0">
        <td colspan="5" style="padding:12px; color:#666;">No data</td>
      </tr>
    </tbody>
  </table>

  <!-- Modal simples -->
  <div
    v-if="modal.open"
    style="position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; padding:16px;"
  >
    <div style="background:#fff; width: 520px; border-radius:12px; padding:16px;">
      <h3 style="margin-top:0;">
        {{ modal.mode === "create" ? t("common.add") : t("common.edit") }}
      </h3>

      <div style="display:grid; gap:10px;">
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
          <span>Stock Quantity</span>
          <input
            v-model.number="form.stockQuantity"
            type="number"
            min="0"
            style="padding:8px 10px; border:1px solid #ddd; border-radius:8px;"
          />
        </label>

        <div v-if="modalError" style="color:#b00020;">{{ modalError }}</div>

        <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:6px;">
          <button @click="closeModal">{{ t("common.cancel") }}</button>
          <button @click="save" :disabled="saving">
            {{ saving ? "Saving..." : t("common.save") }}
          </button>
        </div>
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

const list = ref([]);

const modal = reactive({
  open: false,
  mode: "create", // create | edit
  editId: null,
});

const form = reactive({
  code: "",
  name: "",
  stockQuantity: 0,
});

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase();
  if (!q) return list.value;
  return list.value.filter((x) =>
    `${x.code} ${x.name} ${x.id}`.toLowerCase().includes(q)
  );
});

function resetForm() {
  form.code = "";
  form.name = "";
  form.stockQuantity = 0;
}

function openCreate() {
  modalError.value = "";
  resetForm();
  modal.mode = "create";
  modal.editId = null;
  modal.open = true;
}

function openEdit(rm) {
  modalError.value = "";
  modal.mode = "edit";
  modal.editId = rm.id;
  form.code = rm.code;
  form.name = rm.name;
  form.stockQuantity = rm.stockQuantity;
  modal.open = true;
}

function closeModal() {
  modal.open = false;
}

async function fetchAll() {
  loading.value = true;
  error.value = "";
  try {
    list.value = await api.get("/raw-materials").then((r) => r.data);
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || "Failed to load data";
  } finally {
    loading.value = false;
  }
}

function validate() {
  if (!form.code?.trim() && modal.mode === "create") return "Code is required";
  if (!form.name?.trim()) return "Name is required";
  if (form.stockQuantity == null || form.stockQuantity < 0) return "Stock must be >= 0";
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
    if (modal.mode === "create") {
      await api.post("/raw-materials", {
        code: form.code.trim(),
        name: form.name.trim(),
        stockQuantity: Number(form.stockQuantity),
      });
    } else {
      await api.put(`/raw-materials/${modal.editId}`, {
        name: form.name.trim(),
        stockQuantity: Number(form.stockQuantity),
      });
    }

    modal.open = false;
    await fetchAll();
  } catch (e) {
    modalError.value = e?.response?.data?.message || e?.message || "Save failed";
  } finally {
    saving.value = false;
  }
}

async function remove(rm) {
  const ok = confirm(`Delete ${rm.code}?`);
  if (!ok) return;

  try {
    await api.delete(`/raw-materials/${rm.id}`);
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
</style>