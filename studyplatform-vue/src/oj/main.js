import '../oj/oj.css'

import { createApp } from 'vue'
import OjPlatform from '../pages/OjPlatform.vue'

// Dedicated OJ page entry. The existing home page keeps using src/main.js.
createApp(OjPlatform).mount('#oj-app')
