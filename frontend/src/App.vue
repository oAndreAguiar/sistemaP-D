<template>
  <div style="font-family: system-ui, Arial; padding: 16px; max-width: 1100px; margin: 0 auto;">
    <header style="display:flex; gap:12px; align-items:center; justify-content:space-between; margin-bottom: 16px;">
      <nav style="display:flex; gap:12px; align-items:center;">
        <RouterLink to="/raw-materials">{{ t("nav.rawMaterials") }}</RouterLink>
        <RouterLink to="/products">{{ t("nav.products") }}</RouterLink>
        <RouterLink to="/production-plan">{{ t("nav.productionPlan") }}</RouterLink>
      </nav>

      <div style="display:flex; gap:8px; align-items:center;">
        <button @click="setLocale('pt')">PT</button>
        <button @click="setLocale('en')">EN</button>
      </div>
    </header>

    <RouterView />
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from "vue-router";
import { useI18n } from "vue-i18n";

const { t, locale } = useI18n();

function setLocale(l) {
  locale.value = l;
}
</script>

<style scoped>
a {
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 8px;
}
.router-link-active {
  background: #eee;
}
button {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #ddd;
  cursor: pointer;
}
</style>