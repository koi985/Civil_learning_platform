import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import Vant from 'vant'
import 'vant/lib/index.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import './styles.css'
import './mobile/mobile.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(Vant)
app.mount('#app')
