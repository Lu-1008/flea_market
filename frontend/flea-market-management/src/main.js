import { createApp } from "vue";
import App from "./App.vue";
import "@/assets/less/index.less"
import router from "./router";
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import {createPinia} from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
// 导入API接口
import api from './api';

const pinia = createPinia();
const app = createApp(App);

// 将API挂载到全局
app.config.globalProperties.$api = api;

app.use(ElementPlus);
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.use(router);
app.use(pinia);

app.mount("#app");
