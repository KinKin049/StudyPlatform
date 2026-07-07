// 应用入口文件，负责创建Vue实例并挂载到DOM

// 导入全局样式文件
import './assets/main.css'
import './assets/lab.css'
import './oj/oj.css'
import 'element-plus/dist/index.css'
import './assets/well-log.css'
import './assets/production-simulation.css'
import './assets/games.css'

// 导入Vue核心模块和依赖
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'

// 创建Vue应用实例，注册路由和Element Plus，挂载到#app元素
createApp(App).use(router).use(ElementPlus).mount('#app')
