import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import zhCN from 'tdesign-vue-next/es/locale/zh_CN'
import 'tdesign-vue-next/es/style/index.css'
import './style.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/authStore'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(TDesign, { globalConfig: zhCN })

useAuthStore().restoreSession()

app.mount('#app')
