import { createApp } from 'vue'
import { createPinia } from 'pinia'
import vuetify from './plugins/vuetify'
import setupPersist from './plugins/pinia'

import App from './App.vue'
import router from './router'

const pinia = createPinia()
setupPersist(pinia)

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(vuetify)

app.mount('#app')
