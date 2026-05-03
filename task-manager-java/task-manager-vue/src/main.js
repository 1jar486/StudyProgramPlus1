import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 【关键】引入你定义的路由配置文件
import './assets/style.css'   // 引入你的全局样式

const app = createApp(App)

// 【关键】必须使用 app.use(router) 插件，路由跳转才会生效
app.use(router)

app.mount('#app')