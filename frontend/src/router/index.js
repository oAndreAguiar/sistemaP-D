import { createRouter, createWebHistory } from "vue-router";
import RawMaterialsPage from "../pages/RawMaterialsPage.vue";
import ProductsPage from "../pages/ProductsPage.vue";
import ProductionPlanPage from "../pages/ProductionPlanPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/raw-materials" },
    { path: "/raw-materials", component: RawMaterialsPage },
    { path: "/products", component: ProductsPage },
    { path: "/production-plan", component: ProductionPlanPage }
  ],
});