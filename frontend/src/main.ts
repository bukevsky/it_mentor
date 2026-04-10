import { createApp } from "vue";
import { createPinia } from "pinia";
import { router } from "@/app/router";
import App from "./App.vue";
import "./styles/conductor-lite.css";
import "./style.scss";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.mount("#app");
