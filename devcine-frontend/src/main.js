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

// Hide global loader when app is mounted and fonts are ready
document.fonts.ready.then(() => {
  // Add a tiny delay to ensure smooth transition
  setTimeout(() => {
    const loader = document.getElementById('global-loader')
    if (loader) {
      loader.style.opacity = '0'
      loader.style.visibility = 'hidden'
      document.body.classList.remove('loading')
      // Remove from DOM after transition
      setTimeout(() => loader.remove(), 600)
    }
  }, 100)
})
