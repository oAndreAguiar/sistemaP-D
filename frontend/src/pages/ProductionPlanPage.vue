<template>
  <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
    <h2 style="margin:0;">{{ t("nav.productionPlan") }}</h2>

    <button @click="run" :disabled="loading">
      {{ loading ? "Loading..." : t("pages.production.run") }}
    </button>
  </div>

  <div v-if="error" style="margin-top:12px; color:#b00020;">
    {{ error }}
  </div>

  <div v-if="plan" style="margin-top:12px;">
    <div style="display:flex; gap:12px; align-items:center; justify-content:space-between;">
      <strong>{{ t("pages.production.total") }}:</strong>
      <span>{{ formatMoney(plan.totalValue) }}</span>
    </div>

    <h3 style="margin:16px 0 8px 0;">{{ t("pages.production.items") }}</h3>
    <table style="width:100%; border-collapse: collapse;">
      <thead>
        <tr style="text-align:left; border-bottom:1px solid #eee;">
          <th style="padding:10px;">{{ t("tables.product") }}</th>
          <th style="padding:10px;">{{ t("tables.units") }}</th>
          <th style="padding:10px;">{{ t("tables.unitValue") }}</th>
          <th style="padding:10px;">{{ t("tables.totalValue") }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="it in plan.items" :key="it.productId" style="border-bottom:1px solid #f2f2f2;">
          <td style="padding:10px;">
            <div><strong>{{ it.productCode }}</strong> - {{ it.productName }}</div>
          </td>
          <td style="padding:10px;">{{ it.unitsToProduce }}</td>
          <td style="padding:10px;">{{ formatMoney(it.unitValue) }}</td>
          <td style="padding:10px;">{{ formatMoney(it.totalValue) }}</td>
        </tr>

        <tr v-if="!plan.items?.length">
          <td colspan="4" style="padding:12px; color:#666;">
            No items suggested.
          </td>
        </tr>
      </tbody>
    </table>

    <h3 style="margin:16px 0 8px 0;">{{ t("pages.production.remainingStock") }}</h3>
    <table style="width:100%; border-collapse: collapse;">
      <thead>
        <tr style="text-align:left; border-bottom:1px solid #eee;">
          <th style="padding:10px;">{{ t("tables.rawMaterial") }}</th>
          <th style="padding:10px;">{{ t("tables.remainingQuantity") }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="rm in plan.remainingStock" :key="rm.rawMaterialId" style="border-bottom:1px solid #f2f2f2;">
          <td style="padding:10px;">
            <div><strong>{{ rm.rawMaterialCode }}</strong> - {{ rm.rawMaterialName }}</div>
          </td>
          <td style="padding:10px;">{{ rm.remainingQuantity }}</td>
        </tr>

        <tr v-if="!plan.remainingStock?.length">
          <td colspan="2" style="padding:12px; color:#666;">
            No remaining stock info
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <div v-else style="margin-top:12px; color:#666;">
    {{ t("pages.production.hint") }}
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { api } from "../services/api";

const { t } = useI18n();

const loading = ref(false);
const error = ref("");
const plan = ref(null);

function formatMoney(v) {
  if (v == null) return "-";
  const n = Number(v);
  return isNaN(n) ? String(v) : n.toFixed(2);
}

async function run() {
  loading.value = true;
  error.value = "";
  try {
    plan.value = await api.post("/production/plan", {}).then((r) => r.data);
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || "Failed to run plan";
  } finally {
    loading.value = false;
  }
}
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