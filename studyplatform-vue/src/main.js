import './assets/main.css'
import './assets/lab.css'
import './oj/oj.css'
import 'element-plus/dist/index.css'
import './assets/well-log.css'
import './assets/production-simulation.css'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'

createApp(App).use(router).use(ElementPlus).mount('#app')
