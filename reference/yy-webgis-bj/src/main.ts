import '@/styles/index.scss'
import TDesign from "tdesign-vue-next"
import { createApp } from "vue"

import App from "./App.vue"
// import Test from "./Test.vue"
import router from "./router"
import { store } from "./stores"

import "tdesign-vue-next/es/style/index.css"
import './yy.less'
import "./style.css"
import "uno.css"
import './reset.css'

import EventBus from './utils/eventBus'
window.$bus = EventBus

import { hasPermission } from '@/utils/role'
window.hasPermission = hasPermission

const app = createApp(App)

app.use(TDesign)
app.use(store)
app.use(router)

app.mount("#app")
