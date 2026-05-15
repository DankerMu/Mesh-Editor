import { createPinia } from "pinia"
import piniaPluginPersistedstate from "pinia-plugin-persistedstate"

const store = createPinia()
store.use(piniaPluginPersistedstate)

export { store }

export * from "./modules/map"
export * from "./modules/user"
export * from './modules/timeline1'
export * from './modules/projection'
export * from './modules/layerManager'

export default store
