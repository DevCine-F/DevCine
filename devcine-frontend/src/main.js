import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './assets/styles/global.css'
import App from './App.vue'
import router from './routers'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')
