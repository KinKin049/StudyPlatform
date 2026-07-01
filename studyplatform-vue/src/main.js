import './assets/main.css'
import './assets/lab.css'
import 'element-plus/dist/index.css'
import './assets/well-log.css'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from './App.vue'

createApp(App).use(ElementPlus).mount('#app')
